package com.tikal.jenkins.plugins.multijob;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import com.tikal.jenkins.plugins.multijob.counters.CounterHelper;
import com.tikal.jenkins.plugins.multijob.counters.CounterManager;
import groovy.util.Eval;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BallColor;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Executor;
import hudson.model.Item;
import hudson.model.Queue.QueueAction;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import jenkins.model.Jenkins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectBuilder;
import org.jenkinsci.plugins.envinject.EnvInjectBuilderContributionAction;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import groovy.util.*;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;
import java.util.concurrent.CancellationException;
import jenkins.model.CauseOfInterruption;

public class MultiJobBuilder extends Builder implements DependecyDeclarer {
    /**
     * The name of the parameter in the build.getBuildVariables() to enable the job build, regardless
     * of scm changes.
     */
    public static final String BUILD_ALWAYS_KEY = "hudson.scm.multijob.build.always";

    private String phaseName;
    private List<PhaseJobsConfig> phaseJobs;
    private ContinuationCondition continuationCondition = ContinuationCondition.SUCCESSFUL;
    private ExecutionType executionType;

    final static Pattern PATTERN = Pattern.compile("(\\$\\{.+?\\})", Pattern.CASE_INSENSITIVE);


    /**
     * The name of the new variable which stores the status of the current job.
     * The state is the name of the corresponding value in {@link StatusJob} enum.
     * @since 1.0.0
     * @see StatusJob#isBuildable()
     */
    public static final String JOB_STATUS = "JOB_STATUS";

    /**
     * The name of the new variable which stores if the job is buildable or not.
     * This value is getted from the {@link StatusJob#isBuildable()}.
     * The only values of this variable are <code>true</code> when the job is buildable,
     * or <code>false</code> when the job is not buildable.
     *
     * @since 1.0.0
     * @see StatusJob#isBuildable()
     */
    public static final String JOB_IS_BUILDABLE = "JOB_IS_BUILDABLE";

    /**
     * A prefix for env vars which should be loaded in {@link #prebuild(Build, BuildListener)}.
     * this will happen only when build was triggered by the {@link MultiJobResumeControl} action
     *
     * @since 1.0.0
     */
    public static final String PERSISTENT_VARS_PREFIX = "RESUMABLE_";

    @Deprecated
    public MultiJobBuilder(String phaseName, List<PhaseJobsConfig> phaseJobs,
                           ContinuationCondition continuationCondition) {
        this(phaseName, phaseJobs, continuationCondition, ExecutionType.PARALLEL);
    }

    @DataBoundConstructor
    public MultiJobBuilder(String phaseName, List<PhaseJobsConfig> phaseJobs,
            ContinuationCondition continuationCondition, ExecutionType executionType) {
        this.phaseName = phaseName;
        this.phaseJobs = Util.fixNull(phaseJobs);
        this.continuationCondition = continuationCondition;
        this.executionType = executionType;
    }

    public String expandToken(String toExpand, final AbstractBuild<?,?> build, final BuildListener listener) {
        String expandedExpression = toExpand;
        try {
            expandedExpression = TokenMacro.expandAll(build, listener, toExpand, false, null);
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
        }

        return PATTERN.matcher(expandedExpression).replaceAll("");
    }

    /**
     * Reports the status of the job.
     * <p>The sequence of the checks are the following (the first winner stops the sequence and returns):</p>
     *
     * <ol>
     *      <li>If job is disabled
     *          then returns <code>{@link StatusJob#IS_DISABLED}</code>.</li>
     *      <li>If job is disabled at phase configuration
     *          then returns <code>{@link StatusJob#IS_DISABLED_AT_PHASECONFIG}</code>.</li>
     *      <li>If BuildOnlyIfSCMChanges is disabled
     *          then returns <code>{@link StatusJob#BUILD_ONLY_IF_SCM_CHANGES_DISABLED}</code>.</li>
     *      <li>If 'Build Always' feature is enabled
     *          then returns <code>{@link StatusJob#BUILD_ALWAYS_IS_ENABLED}</code>.</li>
     *      <li>If job doesn't contains lastbuild
     *          then returns <code>{@link StatusJob#DOESNT_CONTAINS_LASTBUILD}</code>.</li>
     *      <li>If lastbuild result of the job is worse than unstable
     *          then returns <code>{@link StatusJob#LASTBUILD_RESULT_IS_WORSE_THAN_UNSTABLE}</code>.</li>
     *      <li>If job's workspace is empty
     *          then returns <code>{@link StatusJob#WORKSPACE_IS_EMPTY}</code>.</li>
     *      <li>If job contains scm changes
     *          then returns <code>{@link StatusJob#CHANGED_SINCE_LAST_BUILD}</code>.</li>
     *      <li>If job's doesn't contains scm changes
     *          then returns <code>{@link StatusJob#NOT_CHANGED_SINCE_LAST_BUILD}</code>.</li>
     * </ol>
     */
    private StatusJob getScmChange(AbstractProject subjob,PhaseJobsConfig phaseConfig,AbstractBuild build, BuildListener listener,Launcher launcher)
    throws IOException, InterruptedException {
        if ( subjob.isDisabled() ) {
            return StatusJob.IS_DISABLED;
        }
        if( phaseConfig.isDisableJob() ) {
            return StatusJob.IS_DISABLED_AT_PHASECONFIG;
        }
        if ( !phaseConfig.isBuildOnlyIfSCMChanges() ){
            return StatusJob.BUILD_ONLY_IF_SCM_CHANGES_DISABLED;
        }
        final boolean buildAlways = Boolean.valueOf((String)(build.getBuildVariables().get(BUILD_ALWAYS_KEY)));

        if ( buildAlways ) {
            return StatusJob.BUILD_ALWAYS_IS_ENABLED;
        }
        final AbstractBuild lastBuild = subjob.getLastBuild();
        if ( lastBuild == null ) {
            return StatusJob.DOESNT_CONTAINS_LASTBUILD;
        }
        if ( lastBuild.getResult() != null && lastBuild.getResult().isWorseThan(Result.UNSTABLE) ) {
            return StatusJob.LASTBUILD_RESULT_IS_WORSE_THAN_UNSTABLE;
        }
        if ( !lastBuild.getWorkspace().exists() ) {
            return StatusJob.WORKSPACE_IS_EMPTY;
        }
        if ( subjob.poll(listener).hasChanges() ) {
            return StatusJob.CHANGED_SINCE_LAST_BUILD;
        }

        return StatusJob.NOT_CHANGED_SINCE_LAST_BUILD;
    }

    public boolean evalCondition(final String condition, final AbstractBuild<?, ?> build, final BuildListener listener) {
        try {
            return (Boolean) Eval.me(expandToken(condition, build, listener).trim());
        } catch (Exception e) {
            listener.getLogger().println("Can't evaluate expression, false is assumed: " + e.toString());
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean perform(final AbstractBuild<?, ? > build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        Jenkins jenkins = Jenkins.getInstance();
        MultiJobBuild multiJobBuild = (MultiJobBuild) build;
        MultiJobProject thisProject = multiJobBuild.getProject();

        boolean resume = false;
        Map<String, SubBuild> successBuildMap = new HashMap<String, SubBuild>();
        Map<String, SubBuild> resumeBuildMap = new HashMap<String, SubBuild>();
        MultiJobResumeControl control = build.getAction(MultiJobResumeControl.class);
        if (null != control) {
            MultiJobBuild prevBuild = (MultiJobBuild) control.getRun();

            boolean willResumeBuild = true;
            if (thisProject.getCheckResumeEnvVars()) {
                String[] variables = thisProject.getResumeEnvVars().split(",");
                for( int i = 0; i < variables.length; i++ ) {
                    String previousValue = prevBuild.getEnvironment(listener).get(variables[i]);
                    String currentValue = build.getEnvironment(listener).get(variables[i]);
                    if( !StringUtils.equals(previousValue, currentValue) ) {
                        willResumeBuild = false;
                        listener.getLogger().println(String.format("Cannot resume the build, values for '%s' do not match: [%s][%s]", variables[i], previousValue, currentValue));
                        break;
                    }
                }
            }
            if(willResumeBuild) {
                for (SubBuild subBuild : prevBuild.getSubBuilds()) {
                    Item item = Jenkins.getInstance().getItem(subBuild.getJobName(), prevBuild.getParent(),
                        AbstractProject.class);
                    if (item instanceof AbstractProject) {
                        AbstractProject childProject = (AbstractProject) item;
                        AbstractBuild childBuild = childProject.getBuildByNumber(subBuild.getBuildNumber());
                        if (null != childBuild) {
                            if (Result.SUCCESS.equals(childBuild.getResult())) {
                                successBuildMap.put(childProject.getUrl(), subBuild);
                            } else {
                                resume = true;
                                resumeBuildMap.put(childProject.getUrl(), subBuild);
                            }
                        }
                    }
                }
            }

            if (!resume) {
                successBuildMap.clear();
            }
        }

        Map<PhaseSubJob, PhaseJobsConfig> phaseSubJobs = new LinkedHashMap<PhaseSubJob, PhaseJobsConfig>(
                phaseJobs.size());
        final CounterManager phaseCounters = new CounterManager();
        boolean aggragatedTestResults = false;
        for (PhaseJobsConfig phaseJobConfig : phaseJobs) {
            Item item = jenkins.getItem(phaseJobConfig.getJobName(), multiJobBuild.getParent(), AbstractProject.class);
            if (item instanceof AbstractProject) {
                AbstractProject job = (AbstractProject) item;
                phaseSubJobs.put(new PhaseSubJob(job), phaseJobConfig);
            }
            if (phaseJobConfig.isAggregatedTestResults()) {
                aggragatedTestResults = true;
            }
        }
        
        if (aggragatedTestResults) {
            multiJobBuild.addTestsResult();
        }

        List<SubTask> subTasks = new ArrayList<SubTask>();
        int index = 0;
        for (PhaseSubJob phaseSubJob : phaseSubJobs.keySet()) {
            index++;

            AbstractProject subJob = phaseSubJob.job;

            // To be coherent with final results, we need to do this here.
            PhaseJobsConfig phaseConfig = phaseSubJobs.get(phaseSubJob);
            StatusJob jobStatus = getScmChange(subJob,phaseConfig,multiJobBuild ,listener,launcher );
            listener.getLogger().println(jobStatus.getMessage(subJob));
            // We are ready to inject vars about scm status. It is useful at condition level.
            Map<String, String> jobScmVars = new HashMap<String, String>();
// New injected variable. It stores the status of the last job executed. It is useful at condition level.
            jobScmVars.put(JOB_STATUS, jobStatus.name());
// New injected variable. It reports if the job is buildable.
            jobScmVars.put(JOB_IS_BUILDABLE, String.valueOf(jobStatus.isBuildable()));
            injectEnvVars(build, listener, jobScmVars);

            if (jobStatus == StatusJob.IS_DISABLED) {
                phaseCounters.processSkipped();
                continue;
            }

            // If we want to apply the condition only if there were no SCM change
            // We can do it by using the "Apply condition only if no SCM changes were found"
            boolean conditionExistsAndEvaluatedToTrue = false;

            if (phaseConfig.getEnableCondition() && phaseConfig.getCondition() != null) {
            	String subJobDisplayName = subJob.getName();
            	if (phaseConfig.getJobAlias() != null && !phaseConfig.getJobAlias().equals("")) {
            		subJobDisplayName += " (" + phaseConfig.getJobAlias() + ")";
				}

                // if SCM has changes or set to always build and condition should always be evaluated
                if(jobStatus.isBuildable() && !phaseConfig.isApplyConditionOnlyIfNoSCMChanges()) {
                    if (evalCondition(phaseConfig.getCondition(), build, listener)) {
                        listener.getLogger().println(String.format("Triggering %s. Condition was evaluated to true.", subJobDisplayName));
                        conditionExistsAndEvaluatedToTrue = true;
                    } else {
                        listener.getLogger().println(String.format("Skipping %s. Condition was evaluated to false.", subJobDisplayName));
                        phaseCounters.processSkipped();
                        continue;
                    }
                }
                // if SCM has no changes but condition is set to be evaluated in this case
                else if(!jobStatus.isBuildable() && phaseConfig.isApplyConditionOnlyIfNoSCMChanges()) {
                    if (evalCondition(phaseConfig.getCondition(), build, listener)) {
                        listener.getLogger().println(String.format("Triggering %s. Condition was evaluated to true.", subJobDisplayName));
                        conditionExistsAndEvaluatedToTrue = true;
                    } else {
                        listener.getLogger().println(String.format("Skipping %s. Condition was evaluated to false.", subJobDisplayName));
                        phaseCounters.processSkipped();
                        continue;
                    }
                }
                // no SCM changes and no condition evaluation
                else if(!jobStatus.isBuildable() && !phaseConfig.isApplyConditionOnlyIfNoSCMChanges()) {
                    listener.getLogger().println(String.format("Skipping %s. No SCM changes found and condition is skipped.", subJobDisplayName));
                    phaseCounters.processSkipped();
                    continue;
                } else {
                    if (!evalCondition(phaseConfig.getCondition(), build, listener)) {
                        listener.getLogger().println(String.format("Skipping %s. Condition was evaluated to false.", subJobDisplayName));
                        phaseCounters.processSkipped();
                        continue;
                    } else {
                        listener.getLogger().println(String.format("Triggering %s. Condition was evaluated to true.", subJobDisplayName));
                        conditionExistsAndEvaluatedToTrue = true;
                    }
                }
            // This is needed because if no condition to eval, the legacy buildOnlyIfSCMChanges feature is still available,
            // so we don't need to change our job configuration.
            }
            if ( ! jobStatus.isBuildable() && !conditionExistsAndEvaluatedToTrue) {
                phaseCounters.processSkipped();
                continue;
            }

            reportStart(listener, subJob, phaseConfig);
            List<Action> actions = new ArrayList<Action>();

            if (resume) {
                SubBuild subBuild = resumeBuildMap.get(subJob.getUrl());
                if (null != subBuild) {
                    AbstractProject prj = Jenkins.getInstance().getItem(subBuild.getJobName(), multiJobBuild.getParent(),
                        AbstractProject.class);
                    AbstractBuild childBuild = prj.getBuildByNumber(subBuild.getBuildNumber());
                    MultiJobResumeControl childControl = new MultiJobResumeControl(childBuild);
                    actions.add(childControl);
                }
            }

            prepareActions(multiJobBuild, subJob, phaseConfig, listener, actions, index);

            if ( jobStatus == StatusJob.IS_DISABLED_AT_PHASECONFIG ) {
                phaseCounters.processSkipped();
                continue;
            } else {
                boolean shouldTrigger = null == successBuildMap.get(subJob.getUrl()) ? true : false;
                subTasks.add(new SubTask(subJob, phaseConfig, actions, multiJobBuild, shouldTrigger));
            }
        }

        if (subTasks.size() < 1) {
            // We inject the variables also when no jobs will be triggered.
            injectEnvVars(build, listener, phaseCounters.toMap());
            return true;
        }

        if (null == executionType) {
            executionType = ExecutionType.PARALLEL;
        }
        int poolSize = executionType.isParallel() ? subTasks.size() : 1;
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        Set<Result> jobResults = new HashSet<Result>();
        BlockingQueue<SubTask> queue = new ArrayBlockingQueue<SubTask>(subTasks.size());
        for (SubTask subTask : subTasks) {
            SubBuild subBuild = successBuildMap.get(subTask.subJob.getUrl());
            if (null == subBuild) {
                CompletionService<Boolean> completion = new ExecutorCompletionService<Boolean>(executor);
                Callable worker = new SubJobWorker(thisProject, listener, subTask, queue);
                completion.submit(worker);
                if (!executionType.isParallel()) {
                    Future<Boolean> future = completion.take();
                    try {
                        future.get();
                        if (checkPhaseTermination(subTask, subTasks, listener)) {
                            break;
                        }
                    } catch (ExecutionException e) {
                        listener.getLogger().println("Error while execution subtask " + subTask.subJob.getDisplayName());
                    }
                }
            } else {
                AbstractBuild jobBuild = subTask.subJob.getBuildByNumber(subBuild.getBuildNumber());
                updateSubBuild(multiJobBuild, thisProject, jobBuild, subBuild.getResult(), subBuild.getJobAlias());
            }
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
                        phaseCounters.process(subTask.result);
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
                phaseCounters.processAborted();
            }
            int i = 0;
            while (!executor.isTerminated() && i < 20) {
                Thread.sleep(1000);
                i++;
            }
            throw new InterruptedException();

        }
        injectEnvVars(build, listener, phaseCounters.toMap());

        for (Result result : jobResults) {
            if (!continuationCondition.isContinue(result)) {
                return false;
            }
        }
        return true;
    }

    public final class SubJobWorker implements Callable {
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

        /**
         * Subtask execution
         * @return value is not used in the builder. Can be used to return some state of the task in the future.
         */
        public Object call() {
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
                    if (subTask.isShouldTrigger()) {
                        subTask.generateFuture();
                    }
                    QueueTaskFuture<AbstractBuild> future = (QueueTaskFuture<AbstractBuild>) subTask.future;
                    while (true) {
                        if (subTask.isCancelled()) {
                            if (jobBuild != null) {
                                Executor exect = jobBuild.getExecutor();
                                if (exect != null) {
                                    exect.interrupt(Result.ABORTED);
                                }

                                reportFinish(listener, jobBuild, Result.ABORTED, subTask.phaseConfig);
                                abortSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, subTask.phaseConfig.getJobAlias());

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
                        updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, subTask.phaseConfig.getJobAlias());
                        if (future.isDone()) {
                            break;
                        }
                        Thread.sleep(2500);
                    }
                    if (jobBuild != null && !finish) {
                        result = jobBuild.getResult();
                        reportFinish(listener, jobBuild, result, subTask.phaseConfig);

                        if (result.isWorseOrEqualTo(Result.UNSTABLE) && result.isCompleteBuild() && subTask.phaseConfig.getEnableRetryStrategy()) {
                            if (isKnownRandomFailure(jobBuild)) {
                                if (retry <= maxRetries) {
                                    listener.getLogger().println("Known failure detected, retrying this build. Try " + retry + " of " + maxRetries + ".");
                                    updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result, true, subTask.phaseConfig.getJobAlias());

                                    subTask.generateFuture();
                                } else {
                                    listener.getLogger().println("Known failure detected, max retries (" + maxRetries + ") exceeded.");
                                    updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result, subTask.phaseConfig.getJobAlias());
                                }
                            } else {
                                listener.getLogger().println("Failed the build, the failure doesn't match the rules.");
                                updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result, subTask.phaseConfig.getJobAlias());
                                finish = true;
                            }
                        } else {
                            updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result, subTask.phaseConfig.getJobAlias());
                            finish = true;
                        }

                        ChangeLogSet<Entry> changeLogSet = jobBuild.getChangeSet();
                        subTask.multiJobBuild.addChangeLogSet(changeLogSet);
                        addBuildEnvironmentVariables(subTask.multiJobBuild, jobBuild, listener);
                        subTask.result = result;
                    }
                }

                if (subTask.phaseConfig.isAggregatedTestResults()) {
                    MultiJobTestAggregator.aggregateResultsFromBuild(jobBuild, subTask.multiJobBuild.getMultiJobTestResults(), listener);
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    if (jobBuild != null) {
                        reportFinish(listener, jobBuild, Result.ABORTED, subTask.phaseConfig);
                        abortSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, subTask.phaseConfig.getJobAlias());
                        subTask.result = Result.ABORTED;
                    }
                } else {
                    listener.getLogger().println(e.toString());
                }
            }

            if (jobBuild == null) {
                updateSubBuild(subTask.multiJobBuild, multiJobProject, subTask.phaseConfig);
            }
            queue.add(subTask);

            return Boolean.TRUE;
        }

        private List<Pattern> getCompiledPattern() throws FileNotFoundException, InterruptedException {
            if (compiledPatterns == null) {
                compiledPatterns = new ArrayList<Pattern>();
                try {
                    listener.getLogger().println("Scanning failed job console output using parsing rule file " + subTask.phaseConfig.getParsingRulesPath() + ".");
                    final File rulesFile = new File(subTask.phaseConfig.getParsingRulesPath());
                    FileInputStream fis = new FileInputStream(rulesFile.getAbsoluteFile());
                    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                    final BufferedReader reader = new BufferedReader(isr);
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            compiledPatterns.add(Pattern.compile(line));
                        }
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
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
                FileInputStream fis = new FileInputStream(logFile);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                final BufferedReader reader = new BufferedReader(isr);
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
                    if (reader != null) {
                        reader.close();
                    }
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
                if (subTask.result != Result.ABORTED && subTask.phaseConfig.getAbortAllJob()) {
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

    private void reportStart(BuildListener listener, AbstractProject subJob, PhaseJobsConfig phaseConfig) {
		String jobDisplayName = subJob.getFullName();
		if (phaseConfig.getJobAlias() != null && !phaseConfig.getJobAlias().equals("")) {
			jobDisplayName += " (" + phaseConfig.getJobAlias() + ")";
		}
        listener.getLogger().printf(
                "Starting build job %s.\n",
                HyperlinkNote.encodeTo('/' + subJob.getUrl(),
						jobDisplayName));
    }

    private void reportFinish(BuildListener listener, AbstractBuild jobBuild,
            Result result, PhaseJobsConfig phaseConfig) {
    	String jobDisplayName = jobBuild.getProject().getFullName();
    	if (phaseConfig.getJobAlias() != null && !phaseConfig.getJobAlias().equals("")) {
			jobDisplayName += " (" + phaseConfig.getJobAlias() + ")";
		}

        listener.getLogger().println(
                "Finished Build : "
                        + HyperlinkNote.encodeTo("/" + jobBuild.getUrl() + "/",
                                String.valueOf(jobBuild.getDisplayName()))
                        + " of Job : "
                        + HyperlinkNote.encodeTo('/' + jobBuild.getProject()
                                .getUrl(), jobDisplayName)
                        + " with status : "
                        + HyperlinkNote.encodeTo('/' + jobBuild.getUrl()
                                + "/console", result.toString()));
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, PhaseJobsConfig phaseConfig) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), phaseConfig.getJobName(), phaseConfig.getJobAlias(), 0,
                phaseName, null, BallColor.NOTBUILT.getImage(), "not built", "", null);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild, String jobAlias) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(), jobAlias,
                jobBuild.getNumber(), phaseName, null, jobBuild.getIconColor()
                        .getImage(), jobBuild.getDurationString(),
                jobBuild.getUrl(), jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild,
            Result result, String jobAlias) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(), jobAlias,
                jobBuild.getNumber(), phaseName, result, jobBuild.getIconColor().getImage(),
                jobBuild.getDurationString(), jobBuild.getUrl(), jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild,
            Result result, boolean retry, String jobAlias) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(), jobAlias,
                jobBuild.getNumber(), phaseName, result, jobBuild.getIconColor().getImage(),
                jobBuild.getDurationString(), jobBuild.getUrl(), retry, false, jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    private void abortSubBuild(MultiJobBuild multiJobBuild, MultiJobProject multiJobProject,
							   AbstractBuild<?, ?> jobBuild, String jobAlias) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(), jobAlias,
                jobBuild.getNumber(), phaseName, Result.ABORTED, BallColor.ABORTED.getImage(), "", jobBuild.getUrl(), false, true, jobBuild);
        multiJobBuild.addSubBuild(subBuild);
    }

    @SuppressWarnings("rawtypes")
    private synchronized void addBuildEnvironmentVariables(MultiJobBuild thisBuild,
            AbstractBuild jobBuild, BuildListener listener) {
        // Env variables map
        Map<String, String> variables = new HashMap<String, String>();

        // Fetch the map of existing environment variables
        try {

            EnvInjectLogger logger = new EnvInjectLogger(listener);
            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            Map<String, String> previousEnvVars = variableGetter
                    .getEnvVarsPreviousSteps(thisBuild, logger);

            // Get current envVars
            variables = new HashMap<String, String>(
                    previousEnvVars);

        } catch (Throwable throwable) {
            listener.getLogger()
                    .println(
                            "[MultiJob] - [ERROR] - Problems occurs on fetching env vars as a build step: "
                                    + throwable.getMessage());
        }

        String jobName = jobBuild.getProject().getName();
        String jobNameSafe = jobName.replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase();
        String buildNumber = Integer.toString(jobBuild.getNumber());
        String buildResult = jobBuild.getResult().toString();
        String buildName = jobBuild.getDisplayName().toString();

        // If the job is run a second time, store the first job's number and result with unique keys
        if (variables.get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe) != null) {
            String runCount = variables.get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe);
            if (runCount.equals("1")) {
                String firstBuildNumber = variables.get(jobNameSafe + "_BUILD_NUMBER");
                String firstBuildResult = variables.get(jobNameSafe + "_BUILD_RESULT");
                variables.put(jobNameSafe + "_" + runCount + "_BUILD_NUMBER", firstBuildNumber);
                variables.put(jobNameSafe + "_" + runCount + "_BUILD_RESULT", firstBuildResult);
            }
        }

        // These will always reference the last build
        variables.put("LAST_TRIGGERED_JOB_NAME", jobName);
        variables.put(jobNameSafe + "_BUILD_NUMBER", buildNumber);
        variables.put(jobNameSafe + "_BUILD_RESULT", buildResult);
        variables.put(jobNameSafe + "_BUILD_NAME", buildName);

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
            variables.put(jobNameSafe + "_" + runCount + "_BUILD_NUMBER", buildNumber);
            variables.put(jobNameSafe + "_" + runCount + "_BUILD_RESULT", buildResult);
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
            EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter( ws );
            EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars( logger );

            try {
                EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
                Map<String, String> previousEnvVars = variableGetter
                        .getEnvVarsPreviousSteps(build, logger);

                // Get current envVars
                Map<String, String> variables = new HashMap<String, String>(previousEnvVars);
                // Acumule PHASE, PHASENAME and MULTIJOB counters.
                // Values are in variables (current values) and incomingVars.
                Map<String, String> mixtured = CounterHelper.putPhaseAddMultijobAndMergeTheRest(listener, this.phaseName, incomingVars, variables);
                // Resolve variables
                final Map<String, String> resultVariables = envInjectEnvVarsService
                        .getMergedVariables(variables, mixtured);

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
                throwable.printStackTrace();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void prepareActions(AbstractBuild build, AbstractProject project,
            PhaseJobsConfig projectConfig, BuildListener listener,
            List<Action> actions, int index) throws IOException, InterruptedException {
        List<Action> parametersActions = null;
        // if (projectConfig.hasProperties()) {
        parametersActions = (List<Action>) projectConfig.getActions(build, listener, project, projectConfig.isCurrParams());
        actions.addAll(parametersActions);
        // }
        actions.add(new MultiJobAction(build, index));

    }

    private class MultiJobAction implements Action, QueueAction {
        public int buildNumber;
        public int index;

        /**
         * @deprecated
         * Maintain backwards compatibility with previous versions of this class.
         * See https://wiki.jenkins-ci.org/display/JENKINS/Hint+on+retaining+backward+compatibility
         */
        @Deprecated
        private transient AbstractBuild build;

        public MultiJobAction(AbstractBuild build, int index) {
            this.buildNumber = build.getNumber();
            this.index = index;
        }

        public boolean shouldSchedule(List<Action> actions) {
            boolean matches = true;

            for (MultiJobAction action : Util.filter(actions, MultiJobAction.class)) {
                if (action.index != index) {
                    matches = false;
                }
                if (action.buildNumber != buildNumber) {
                    matches = false;
                }
            }

            return !matches;
        }

        protected Object readResolve() {
            if (build != null) {
                buildNumber = build.getNumber();
            }
            return this;
        }

        public String getIconFileName() {
            return null;
        }

        public String getDisplayName() {
            return "this shouldn't be displayed";
        }

        public String getUrlName() {
            return null;
        }
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

    public enum ExecutionType {

        PARALLEL("Running phase jobs in parallel") {
            @Override
            public boolean isParallel() {
                return true;
            }
        },
        SEQUENTIALLY("Running phase jobs sequentially") {
            @Override
            public boolean isParallel() {
                return false;
            }
        };

        final private String label;

        ExecutionType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        abstract public boolean isParallel();
    }

    public void setExecutionType(ExecutionType executionType) {
        this.executionType = executionType;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        boolean resume = false;
        MultiJobResumeControl control = build.getAction(MultiJobResumeControl.class);
        if (null != control) {
            MultiJobBuild prevBuild = (MultiJobBuild) control.getRun();
            for (SubBuild subBuild : prevBuild.getSubBuilds()) {
                Item item = Jenkins.getInstance().getItem(subBuild.getJobName(), prevBuild.getParent(),
                        AbstractProject.class);
                if (item instanceof AbstractProject) {
                    AbstractProject childProject = (AbstractProject) item;
                    AbstractBuild childBuild = childProject.getBuildByNumber(subBuild.getBuildNumber());
                    if (null != childBuild) {
                        if (childBuild.getResult().equals(Result.FAILURE)) {
                            resume = true;
                        }
                    }
                }
            }
            if (resume) {
                EnvInjectLogger logger = new EnvInjectLogger(listener);
                EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
                try {
                    Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(prevBuild, logger);
                    Map<String, String> persistentEnvVars = new HashMap<String, String>();
                    for (String key : previousEnvVars.keySet()) {
                        if(key.startsWith(PERSISTENT_VARS_PREFIX)) {
                            persistentEnvVars.put(key, previousEnvVars.get(key));
                        }
                    }
                    persistentEnvVars.put("RESUMED_BUILD", "true");
                    injectEnvVars(build, listener, persistentEnvVars);
                } catch (Throwable throwable) {
                    listener.getLogger().println("[MultiJob] - [ERROR] - Problems occurs on injecting env vars in prebuild: "
                                            + throwable.getMessage());
                    throwable.printStackTrace();
                }
            }
        }
        return true;
    }
}
