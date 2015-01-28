package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.BallColor;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.model.Executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sf.json.JSONObject;
import jenkins.model.Jenkins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;

import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectBuilderContributionAction;
import org.jenkinsci.plugins.envinject.EnvInjectBuilder;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import groovy.util.*;

public class MultiJobBuilder extends Builder implements DependecyDeclarer {
    /**
     * The name of the parameter in the build.getBuildVariables() to enable the job build, regardless
     * of scm changes.
     */

    public static final String BUILD_ALWAYS_KEY = "hudson.scm.multijob.build.always";
    /**
     * List of messages to show by console.
     */
    private static final String[]  TRIGGER_MESSAGES = {
        "    >> [%s] added to build queue.\n",
        "    >> [%s] has changes since last build. Adding to build queue.\n",
        "    >> [%s] has no changes since last build, but it will be adding to build queue.\n",
        "    >> [%s] has no changes since last build, but you have enabled the 'build always' function. Adding to build queue.\n",
        "    >> [%s] has no changes since last build, so it will be skipped.\n",
        "    >> [%s] has been disabled. Skipping it.\n"
    };

    private static final Pattern PATTERN = Pattern.compile("(\\$\\{.+?\\})", Pattern.CASE_INSENSITIVE);

    private String phaseName;
    private List<PhaseJobsConfig> phaseJobs;
    private ContinuationCondition continuationCondition = ContinuationCondition.SUCCESSFUL;

    @DataBoundConstructor
    public MultiJobBuilder(String phaseName, List<PhaseJobsConfig> phaseJobs,
            ContinuationCondition continuationCondition) {
        this.phaseName = phaseName;
        this.phaseJobs = Util.fixNull(phaseJobs);
        this.continuationCondition = continuationCondition;
    }

    public String expandToken(String toExpand, final AbstractBuild<?,?> build, final BuildListener listener) {
        String expandedExpression = toExpand;
        try {
            expandedExpression = TokenMacro.expandAll(build, listener, toExpand, false, null);
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
        }

        Matcher matcher = PATTERN.matcher(expandedExpression);

        return matcher.replaceAll("");
    }
    private int getScmChange(AbstractProject subjob, PhaseJobsConfig phaseConfig, AbstractBuild build, BuildListener listener,Launcher launcher) throws IOException, InterruptedException{
        final boolean buildOnlyIfSCMChanges = phaseConfig.isBuildOnlyIfSCMChanges();
        final boolean buildAlways = Boolean.valueOf((String)(build.getBuildVariables().get(BUILD_ALWAYS_KEY)));
        final boolean containsLastBuild = buildAlways ? false : subjob.getLastBuild() != null;
        final boolean hasChanges = buildAlways ? false : !containsLastBuild || subjob.poll(listener).hasChanges();

        final int message = 
            (!buildOnlyIfSCMChanges)
                ? 0
                : (hasChanges
                    ? 1
                    : (!buildOnlyIfSCMChanges
                        ? 2
                        : ((buildAlways) ? 3 : 4)
                    )
                );
        listener.getLogger().printf(TRIGGER_MESSAGES[message], subjob.getName());
        return message; 
    }

    public boolean evalCondition(final String condition, final AbstractBuild<?, ?> build, final BuildListener listener) {
        try {
            return (Boolean) Eval.me(expandToken(condition, build, listener).toLowerCase().trim());
        } catch (Exception e) {
            listener.getLogger().println("Can't evaluate expression, false is assumed.");
            listener.getLogger().println(e.toString());
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean perform(final AbstractBuild<?, ? > build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        Jenkins jenkins = Jenkins.getInstance();
        MultiJobBuild multiJobBuild = (MultiJobBuild) build;
        MultiJobProject thisProject = multiJobBuild.getProject();

        Map<PhaseSubJob, PhaseJobsConfig> phaseSubJobs = new HashMap<PhaseSubJob, PhaseJobsConfig>(
                phaseJobs.size());

        for (PhaseJobsConfig phaseJobConfig : phaseJobs) {
            Item item = jenkins.getItem(phaseJobConfig.getJobName(), multiJobBuild.getParent(), AbstractProject.class);
            if (item instanceof AbstractProject) {
                AbstractProject job = (AbstractProject) item;
                phaseSubJobs.put(new PhaseSubJob(job), phaseJobConfig);
            }
        }

        List<SubTask> subTasks = new ArrayList<SubTask>();
        for (PhaseSubJob phaseSubJob : phaseSubJobs.keySet()) {
            AbstractProject subJob = phaseSubJob.job;
            if (subJob.isDisabled()) {
                listener.getLogger().println(String.format("Skipping %s. This Job has been disabled.", subJob.getName()));
                continue;
            }

            PhaseJobsConfig phaseConfig = phaseSubJobs.get(phaseSubJob);

            if (phaseConfig.getEnableCondition() && phaseConfig.getCondition() != null) {
                if (!evalCondition(phaseConfig.getCondition(), build, listener)) {
                    listener.getLogger().println(String.format("Skipping %s. Condition is evaluate to false.", subJob.getName()));
                    continue;
                }
            }
            if (phaseConfig.isBuildOnlyIfSCMChanges()){
                if( getScmChange(subJob,phaseConfig,multiJobBuild ,listener,launcher ) >= 4) {
                    continue;
                }
            }
            reportStart(listener, subJob);
            List<Action> actions = new ArrayList<Action>();
            prepareActions(multiJobBuild, subJob, phaseConfig, listener, actions);

            while (subJob.isInQueue()) {
                TimeUnit.SECONDS.sleep(subJob.getQuietPeriod());
            }

            if (!phaseConfig.isDisableJob()) {
                subTasks.add(new SubTask(subJob, phaseConfig, actions, multiJobBuild));
            } else {
                listener.getLogger().println(String.format("Warning: %s subjob is disabled.", subJob.getName()));
            }
        }

        if (subTasks.size() < 1)
            return true;

        ExecutorService executor = Executors.newFixedThreadPool(subTasks.size());
        Set<Result> jobResults = new HashSet<Result>();
        BlockingQueue<SubTask> queue = new ArrayBlockingQueue<SubTask>(subTasks.size());
        for (SubTask subTask : subTasks) {
            Runnable worker = new SubJobWorker(thisProject, listener, subTask, queue);
            executor.execute(worker);
        }

        try {
            executor.shutdown();
            int resultCounter = 0;
            while (!executor.isTerminated()) {
                SubTask subTask = queue.poll(5, TimeUnit.SECONDS);
                if (subTask != null) {
                    resultCounter++;
                    if (subTask.result != null) {
                        jobResults.add(subTask.result);
                        checkPhaseTermination(subTask, subTasks, listener);
                    }
                }
                if (subTasks.size() <= resultCounter) {
                    break;
                }
            }

            executor.shutdownNow();
        } catch (InterruptedException exception) {
            listener.getLogger().println("Aborting all subjobs.");
            for (SubTask _subTask : subTasks) {
                _subTask.cancelJob();
            }
            int i = 0;
            while (!executor.isTerminated() && i < 20) {
                Thread.sleep(1000);
                i++;
            }
            throw new InterruptedException();
        }

        for (Result result : jobResults) {
            if (!continuationCondition.isContinue(result)) {
                return false;
            }
        }

        return true;

    }

    public final class SubJobWorker extends Thread {
        final private MultiJobProject multiJobProject;
        final private BuildListener listener;
        private SubTask subTask;
        private BlockingQueue<SubTask> queue;
        private List<Pattern> compiledPatterns;

        public SubJobWorker(MultiJobProject multiJobProject,
                    BuildListener listener,
                    SubTask subTask,
                    BlockingQueue<SubTask> queue) {
            this.multiJobProject = multiJobProject;
            this.listener = listener;
            this.subTask = subTask;
            this.queue = queue;
        }

        public void run() {
            Result result = null;
            AbstractBuild jobBuild = null;
            try {
                int maxRetries = subTask.phaseConfig.getMaxRetries();
                if (!subTask.phaseConfig.getEnableRetryStrategy()) {
                    maxRetries = 0;
                }

                int retry = 0;
                boolean finish = false;

                while (retry <= maxRetries && !finish) {
                    retry++;
                    QueueTaskFuture<AbstractBuild> future = (QueueTaskFuture<AbstractBuild>) subTask.future;
                    while (true) {
                        if (subTask.isCancelled()) {
                            if (jobBuild != null) {
                                Executor exect = jobBuild.getExecutor();
                                if (exect != null) {
                                    exect.interrupt(Result.ABORTED);
                                }

                                reportFinish(listener, jobBuild, Result.ABORTED);
                                abortSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild);

                                finish = true;
                                break;
                            }
                        }

                        try {
                            jobBuild = future.getStartCondition().get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            if (e instanceof TimeoutException)
                                continue;
                            else {
                                throw e;
                            }
                        }
                        updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild);
                        if (future.isDone()) {
                            break;
                        }
                        Thread.sleep(2500);
                    }
                    if (jobBuild != null && !finish) {
                        result = jobBuild.getResult();
                        reportFinish(listener, jobBuild, result);

                        if (result.isWorseOrEqualTo(Result.FAILURE) && result.isCompleteBuild() && subTask.phaseConfig.getEnableRetryStrategy()) {
                            if (isKnownRandomFailure(jobBuild)) {
                                if (retry <= maxRetries) {
                                    listener.getLogger().println("Known failure detected, retrying this build. Try " + retry + " of " + maxRetries + ".");
                                    updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result, true);

                                    subTask.GenerateFuture();
                                } else {
                                    listener.getLogger().println("Known failure detected, max retries (" + maxRetries + ") exceeded.");
                                    updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result);
                                }
                            } else {
                                listener.getLogger().println("Failed the build, the failure doesn't match the rules.");
                                updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result);
                                finish = true;
                            }
                        } else {
                            updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result);
                            finish = true;
                        }

                        ChangeLogSet<Entry> changeLogSet = jobBuild.getChangeSet();
                        subTask.multiJobBuild.addChangeLogSet(changeLogSet);
                        addBuildEnvironmentVariables(subTask.multiJobBuild, jobBuild, listener);
                        subTask.result = result;
                    }
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    if (jobBuild != null) {
                        reportFinish(listener, jobBuild, Result.ABORTED);
                        abortSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild);
                        subTask.result = Result.ABORTED;
                    }
                } else {
                    listener.getLogger().println(e.toString());
                    e.printStackTrace();
                }
            }

            if (jobBuild == null) {
                updateSubBuild(subTask.multiJobBuild, multiJobProject, subTask.phaseConfig);
            }
            queue.add(subTask);
        }

        private List<Pattern> getCompiledPattern() throws FileNotFoundException, InterruptedException {
            if (compiledPatterns == null) {
                compiledPatterns = new ArrayList<Pattern>();
                try {
                    listener.getLogger().println("Scanning failed job console output using parsing rule file " + subTask.phaseConfig.getParsingRulesPath() + ".");
                    final File rulesFile = new File(subTask.phaseConfig.getParsingRulesPath());
                    final BufferedReader reader = new BufferedReader(new FileReader(rulesFile.getAbsolutePath()));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            compiledPatterns.add(Pattern.compile(line));
                        }
                    } finally {
                        reader.close();
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        throw new InterruptedException();
                    } else if (e instanceof FileNotFoundException) {
                        throw new FileNotFoundException();
                    } else {
                        listener.getLogger().println(e.toString());
                        e.printStackTrace();
                    }
                }
            }
            return compiledPatterns;
        }

        private final class LineAnalyser extends Thread {
            final private BufferedReader reader;
            final private List<Pattern> patterns;
            private BlockingQueue<LineQueue> finishQueue;

            public LineAnalyser(BufferedReader reader, List<Pattern> patterns, BlockingQueue<LineQueue> finishQueue) {
                this.reader = reader;
                this.patterns = patterns;
                this.finishQueue = finishQueue;
            }

            public void run() {
                boolean errorFound = false;
                try {
                    String line;
                    while (reader.ready() && !errorFound) {
                        line = reader.readLine();
                        if (line != null) {
                            for (Pattern pattern : patterns) {
                                if (pattern.matcher(line).find()) {
                                    errorFound = true;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        // Nothing
                    } else {
                        listener.getLogger().println(e.toString());
                        e.printStackTrace();
                    }
                } finally {
                    finishQueue.add(new LineQueue(errorFound));
                }
            }
        }

        private boolean isKnownRandomFailure(AbstractBuild build) throws InterruptedException {
            boolean failure = false;
            try {
                final List<Pattern> patterns = getCompiledPattern();
                final File logFile = build.getLogFile();

                final BufferedReader reader = new BufferedReader(new FileReader(logFile.getAbsolutePath()));
                try {
                    int numberOfThreads = 10; // Todo : Add this in Configure section
                    if (numberOfThreads < 0) {
                        numberOfThreads = 1;
                    }
                    ExecutorService executorAnalyser = Executors.newFixedThreadPool(numberOfThreads);
                    BlockingQueue<LineQueue> finishQueue = new ArrayBlockingQueue<LineQueue>(numberOfThreads);

                    for (int i = 0; i < numberOfThreads; i++) {
                        Runnable worker = new LineAnalyser(reader, patterns, finishQueue);
                        executorAnalyser.execute(worker);
                    }

                    executorAnalyser.shutdown();
                    int resultCounter = 0;
                    while (!executorAnalyser.isTerminated()) {
                        resultCounter++;
                        LineQueue lineQueue = finishQueue.take();
                        if (lineQueue.hasError()) {
                            failure = true;
                            break;
                        } else if (numberOfThreads == resultCounter) {
                            break;
                        }
                    }
                    executorAnalyser.shutdownNow();
                } finally {
                    reader.close();
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    throw new InterruptedException();
                } else if (e instanceof FileNotFoundException) {
                    listener.getLogger().println("Parser rules file not found.");
                    failure = false;
                } else {
                    listener.getLogger().println(e.toString());
                    e.printStackTrace();
                }
            }
            return failure;
        }
    }

    protected boolean checkPhaseTermination(SubTask subTask, List<SubTask> subTasks, final BuildListener listener) {
        try {
            KillPhaseOnJobResultCondition killCondition = subTask.phaseConfig.getKillPhaseOnJobResultCondition();
            if (killCondition.equals(KillPhaseOnJobResultCondition.NEVER) && subTask.result != Result.ABORTED) {
                return false;
            }
            if (killCondition.isKillPhase(subTask.result)) {
                if (subTask.result != Result.ABORTED || subTask.phaseConfig.getAbortAllJob()) {
                    for (SubTask _subTask : subTasks) {
                        _subTask.cancelJob();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            listener.getLogger().printf(e.toString());
            return false;
        }
        return false;
    }

    private void reportStart(BuildListener listener, AbstractProject subJob) {
        listener.getLogger().printf(
                "Starting build job %s.\n",
                HyperlinkNote.encodeTo('/' + subJob.getUrl(),
                        subJob.getFullName()));
    }

    private void reportFinish(BuildListener listener, AbstractBuild jobBuild,
            Result result) {
        listener.getLogger().println(
                "Finished Build : "
                        + HyperlinkNote.encodeTo("/" + jobBuild.getUrl() + "/",
                                String.valueOf(jobBuild.getDisplayName()))
                        + " of Job : "
                        + HyperlinkNote.encodeTo('/' + jobBuild.getProject()
                                .getUrl(), jobBuild.getProject().getFullName())
                        + " with status : "
                        + HyperlinkNote.encodeTo('/' + jobBuild.getUrl()
                                + "/console", result.toString()));
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, PhaseJobsConfig phaseConfig) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), phaseConfig.getJobName(), 0,
                phaseName, null, BallColor.NOTBUILT.getImage(), "not built", "", null);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild jobBuild) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, null, jobBuild.getIconColor()
                        .getImage(), jobBuild.getDurationString(),
                jobBuild.getUrl(), jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild jobBuild,
            Result result) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, result, jobBuild.getIconColor().getImage(),
                jobBuild.getDurationString(), jobBuild.getUrl(), jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild jobBuild,
            Result result, boolean retry) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, result, jobBuild.getIconColor().getImage(),
                jobBuild.getDurationString(), jobBuild.getUrl(), retry, false, jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void abortSubBuild(MultiJobBuild multiJobBuild, MultiJobProject multiJobProject, AbstractBuild jobBuild) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, Result.ABORTED, BallColor.ABORTED.getImage(), "", jobBuild.getUrl(),
                false, true, jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    @SuppressWarnings("rawtypes")
    private void addBuildEnvironmentVariables(MultiJobBuild thisBuild,
            AbstractBuild jobBuild, BuildListener listener) {
        // Env variables map
        Map<String, String> variables = new HashMap<String, String>();

        String jobName = jobBuild.getProject().getName();
        String jobNameSafe = jobName.replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase();
        String buildNumber = Integer.toString(jobBuild.getNumber());
        String buildResult = jobBuild.getResult().toString();

        // These will always reference the last build
        variables.put("LAST_TRIGGERED_JOB_NAME", jobName);
        variables.put(jobNameSafe + "_BUILD_NUMBER", buildNumber);
        variables.put(jobNameSafe + "_BUILD_RESULT", buildResult);

        if (variables.get("TRIGGERED_JOB_NAMES") == null) {
            variables.put("TRIGGERED_JOB_NAMES", jobName);
        } else {
            String triggeredJobNames = variables.get("TRIGGERED_JOB_NAMES")
                    + "," + jobName;
            variables.put("TRIGGERED_JOB_NAMES", triggeredJobNames);
        }

        if (variables.get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe) == null) {
            variables.put("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe, "1");
        } else {
            String runCount = Integer.toString(Integer.parseInt(variables
                    .get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe)) + 1);
            variables.put("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe, runCount);
        }

        // Set the new build variables map
        injectEnvVars(thisBuild, listener, variables);
    }

    /**
     * Method for properly injecting environment variables via EnvInject plugin.
     * Method based off logic in {@link EnvInjectBuilder#perform}
     */
    private void injectEnvVars(AbstractBuild<?, ?> build,
            BuildListener listener, Map<String, String> incomingVars) {
        if (build != null && incomingVars != null) {
            EnvInjectLogger logger = new EnvInjectLogger(listener);
            FilePath ws = build.getWorkspace();
            EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(
                    ws);
            EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

            try {

                EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
                Map<String, String> previousEnvVars = variableGetter
                        .getEnvVarsPreviousSteps(build, logger);

                // Get current envVars
                Map<String, String> variables = new HashMap<String, String>(
                        previousEnvVars);

                // Resolve variables
                final Map<String, String> resultVariables = envInjectEnvVarsService
                        .getMergedVariables(variables, incomingVars);

                // Set the new build variables map
                build.addAction(new EnvInjectBuilderContributionAction(
                        resultVariables));

                // Add or get the existing action to add new env vars
                envInjectActionSetter.addEnvVarsToEnvInjectBuildAction(build,
                        resultVariables);
            } catch (Throwable throwable) {
                listener.getLogger()
                        .println(
                                "[MultiJob] - [ERROR] - Problems occurs on injecting env vars as a build step: "
                                        + throwable.getMessage());
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void prepareActions(AbstractBuild build, AbstractProject project,
            PhaseJobsConfig projectConfig, BuildListener listener,
            List<Action> actions) throws IOException, InterruptedException {
        List<Action> parametersActions = null;
        // if (projectConfig.hasProperties()) {
        parametersActions = (List<Action>) projectConfig.getActions(build, listener, project, projectConfig.isCurrParams());
        actions.addAll(parametersActions);
        // }

    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public List<PhaseJobsConfig> getPhaseJobs() {
        return phaseJobs;
    }

    public void setPhaseJobs(List<PhaseJobsConfig> phaseJobs) {
        this.phaseJobs = phaseJobs;
    }

    public boolean phaseNameExist(String phaseName) {
        for (PhaseJobsConfig phaseJob : phaseJobs) {
            if (phaseJob.getDisplayName().equals(phaseName)) {
                return true;
            }
        }
        return false;
    }

    private final static class PhaseSubJob {
        AbstractProject job;

        PhaseSubJob(AbstractProject job) {
            this.job = job;
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType.equals(MultiJobProject.class);
        }

        @Override
        public String getDisplayName() {
            return "MultiJob Phase";
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return req.bindJSON(MultiJobBuilder.class, formData);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            save();
            return true;
        }
    }

    @SuppressWarnings("rawtypes")
    public void buildDependencyGraph(AbstractProject owner,
            DependencyGraph graph) {
        Jenkins jenkins = Jenkins.getInstance();
        List<PhaseJobsConfig> phaseJobsConfigs = getPhaseJobs();

        if (phaseJobsConfigs == null)
            return;
        for (PhaseJobsConfig project : phaseJobsConfigs) {
            Item topLevelItem = jenkins.getItem(project.getJobName(), owner.getParent(), AbstractProject.class);
            if (topLevelItem instanceof AbstractProject) {
                Dependency dependency = new Dependency(owner,
                        (AbstractProject) topLevelItem) {

                    @Override
                    public boolean shouldTriggerBuild(AbstractBuild build,
                            TaskListener listener, List<Action> actions) {
                        return false;
                    }

                };
                graph.addDependency(dependency);
            }
        }
    }

    public boolean onJobRenamed(String oldName, String newName) {
        boolean changed = false;
        for (Iterator i = phaseJobs.iterator(); i.hasNext();) {
            PhaseJobsConfig phaseJobs = (PhaseJobsConfig) i.next();
            String jobName = phaseJobs.getJobName();
            if (jobName.trim().equals(oldName)) {
                if (newName != null) {
                    phaseJobs.setJobName(newName);
                    changed = true;
                } else {
                    i.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    public boolean onJobDeleted(String oldName) {
        return onJobRenamed(oldName, null);
    }

    public static enum ContinuationCondition {

        ALWAYS("Always") {
            @Override
            public boolean isContinue(Result result) {
                return true;
            }
        },
        SUCCESSFUL("Successful") {
            @Override
            public boolean isContinue(Result result) {
                return result.equals(Result.SUCCESS);
            }
        },
        COMPLETED("Completed") {
            @Override
            public boolean isContinue(Result result) {
                return result.isCompleteBuild();
            }
        },
        UNSTABLE("Stable or Unstable but not Failed") {
            @Override
            public boolean isContinue(Result result) {
                return result.isBetterOrEqualTo(Result.UNSTABLE);
            }
        },
        FAILURE("Failed") {
            @Override
            public boolean isContinue(Result result) {
                return result.isWorseOrEqualTo(Result.FAILURE);
            }
        };

        abstract public boolean isContinue(Result result);

        private ContinuationCondition(String label) {
            this.label = label;
        }

        final private String label;

        public String getLabel() {
            return label;
        }
    }

    public ContinuationCondition getContinuationCondition() {
        return continuationCondition;
    }

    public void setContinuationCondition(
            ContinuationCondition continuationCondition) {
        this.continuationCondition = continuationCondition;
    }
}