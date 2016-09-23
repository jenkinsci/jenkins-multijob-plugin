package com.tikal.jenkins.plugins.multijob;

import com.cisco.jenkins.plugins.script.PipingTask;
import com.cisco.jenkins.plugins.script.ScriptRunner;
import com.cisco.jenkins.plugins.script.config.ConfigFactory;
import com.cisco.jenkins.plugins.script.config.ScriptConfig;
import com.sonyericsson.rebuild.RebuildCause;
import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import com.tikal.jenkins.plugins.multijob.counters.CounterHelper;
import com.tikal.jenkins.plugins.multijob.counters.CounterManager;
import com.tikal.jenkins.plugins.multijob.listeners.MultiJobListener;
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
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Computer;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Executor;
import hudson.model.Item;
import hudson.model.Queue.QueueAction;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.remoting.Pipe;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.StreamCopyThread;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectBuilder;
import org.jenkinsci.plugins.envinject.EnvInjectBuilderContributionAction;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

public class MultiJobBuilder extends Builder implements DependecyDeclarer {
    /**
     * The name of the parameter in the build.getBuildVariables() to enable the job build, regardless
     * of scm changes.
     */
    public static final String BUILD_ALWAYS_KEY = "hudson.scm.multijob.build.always";

    private String phaseName;
    private List<PhaseJobsConfig> phaseJobs;
    private ContinuationCondition continuationCondition = ContinuationCondition.SUCCESSFUL;
    private boolean enableGroovyScript;
    private boolean isUseScriptFile;
    private String scriptPath;
    private String scriptText;
    private boolean isScriptOnSlave;
    private String bindings;
    private boolean isRunOnSlave;
    private IgnorePhaseResult ignorePhaseResult = IgnorePhaseResult.NEVER;
    private ExecutionType executionType = ExecutionType.PARALLEL;

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

    @DataBoundConstructor
    public MultiJobBuilder(String phaseName, List<PhaseJobsConfig> phaseJobs,
            ContinuationCondition continuationCondition, boolean enableGroovyScript, ScriptLocation scriptLocation,
                           String bindings, boolean isRunOnSlave,
                           ExecutionType executionType, IgnorePhaseResult ignorePhaseResult) {
        this.phaseName = phaseName;
        this.phaseJobs = Util.fixNull(phaseJobs);
        this.continuationCondition = continuationCondition;
        this.enableGroovyScript = enableGroovyScript;
        if (null != scriptLocation) {
            this.scriptText = Util.fixNull(scriptLocation.getScriptText());
            this.isUseScriptFile = scriptLocation.isUseFile();
            this.scriptPath = Util.fixNull(scriptLocation.getScriptPath());
            this.isScriptOnSlave = scriptLocation.isScriptOnSlave();
        } else {
            this.scriptText = "";
            this.scriptPath = "";
            this.isUseScriptFile = false;
            this.isScriptOnSlave = false;
        }
        this.bindings = Util.fixNull(bindings);
        if (null == executionType) {
            this.executionType = ExecutionType.PARALLEL;
        } else {
            this.executionType = executionType;
        }
        this.isRunOnSlave = isRunOnSlave;
        this.ignorePhaseResult = ignorePhaseResult;
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
     *          then returns <code>{@link StatusJob#BUILD_ON_SCM_CHANGES_ONLY}</code>.</li>
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
        if ( lastBuild.getResult().isWorseThan(Result.UNSTABLE) ) {
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
            return (Boolean) Eval.me(expandToken(condition, build, listener).toLowerCase().trim());
        } catch (Exception e) {
            listener.getLogger().println("Can't evaluate expression, false is assumed: " + e.toString());
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean perform(final AbstractBuild<?, ? > build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        MultiJobBuild multiJobBuild = (MultiJobBuild) build;
        MultiJobProject thisProject = multiJobBuild.getProject();

        if (null == executionType) {
            executionType = ExecutionType.PARALLEL;
        }

        Computer computer = Computer.currentComputer();
        boolean isMasterNode = true;
        if (null != computer) {
            isMasterNode = computer.getNode().getDescriptor() instanceof Jenkins.DescriptorImpl;
        }

        if (enableGroovyScript) {
            if (isRunOnSlave && !isMasterNode) {
                ScriptConfig scriptConfig = ConfigFactory.getConfig(!isRunOnSlave, isScriptOnSlave, isUseScriptFile,
                                                                    scriptPath, scriptText);
                Pipe pipe = Pipe.createRemoteToLocal();
                PipingTask piping = new PipingTask(pipe, scriptConfig);
                piping.addVarMap(Utils.getBindings(bindings));
                piping.addPropMap(Utils.getEnvVars(build, listener));
                launcher.getChannel().callAsync(piping);
                String threadId = UUID.randomUUID().toString();
                Thread t = new StreamCopyThread(threadId, pipe.getIn(), listener.getLogger());
                t.start();
                t.join();
            } else {
                ScriptRunner runner = new ScriptRunner(build, listener);
                runner.bindVariablesMap(Utils.getBindings(bindings));
                if (isUseScriptFile) {
                    if (isScriptOnSlave) {
                        runner.evaluateOnSlaveFs(expandToken(scriptPath, build, listener));
                    } else {
                        runner.evaluateFromWorkspace(expandToken(scriptPath, build, listener));
                    }
                } else {
                    runner.evaluate(scriptText);
                }
            }
        }

        if (Utils.rebuildPluginAvailable()) {
            RebuildCause rebuildCause = null;
            int buildNumber = 0;
            for (Cause cause : build.getCauses()) {
                if (cause instanceof RebuildCause) {
                    RebuildCause r = (RebuildCause) cause;
                    if (r.getUpstreamBuild() > buildNumber) {
                        rebuildCause = r;
                        buildNumber = r.getUpstreamBuild();
                    }
                }
            }
            if (rebuildCause != null) {
                MultiJobBuild prevBuild = (MultiJobBuild) rebuildCause.getUpstreamRun();
                WasResumedAction wasResumedAction = prevBuild.getAction(WasResumedAction.class);
                if (wasResumedAction != null && wasResumedAction.isActive()) {
                    build.addAction(new MultiJobResumeControl(prevBuild));
                    build.addAction(new CauseAction(new ResumeCause(prevBuild)));
                    wasResumedAction.deactivate();
                } // else it's a plain rebuild
            }
        }

        boolean resume = false;
        Map<String, SubBuild> successBuildMap = new HashMap<String, SubBuild>();
        Map<String, SubBuild> failedBuildMap = new HashMap<String, SubBuild>();
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
                        if (childBuild.getResult().equals(Result.FAILURE)
                                || childBuild.getResult().equals(Result.ABORTED)) {
                            resume = true;
                            failedBuildMap.put(childProject.getUrl(), subBuild);
                        } else {
                            successBuildMap.put(childProject.getUrl(), subBuild);
                        }
                    }
                }
            }
            if (!resume) {
                successBuildMap.clear();
            }
        }

        Jenkins jenkins = Jenkins.getInstance();
        Map<PhaseSubJob, PhaseJobsConfig> phaseSubJobs = new LinkedHashMap<PhaseSubJob, PhaseJobsConfig>();
        final CounterManager phaseCounters = new CounterManager();

        for (PhaseJobsConfig phaseJobConfig : phaseJobs) {
            Item item = jenkins.getItem(phaseJobConfig.getJobName(), multiJobBuild.getParent(), AbstractProject.class);
            if (item instanceof AbstractProject) {
                AbstractProject job = (AbstractProject) item;
                phaseSubJobs.put(new PhaseSubJob(job), phaseJobConfig);
            }
        }

        List<SubTask> subTasks = new ArrayList<SubTask>();
        int index = 0;
        for (PhaseSubJob phaseSubJob : phaseSubJobs.keySet()) {
            index++;

            AbstractProject subJob = phaseSubJob.job;

            // To be coherent with final results, we need to do this here.
            PhaseJobsConfig phaseConfig = phaseSubJobs.get(phaseSubJob);
            if (null == phaseConfig.getResumeCondition()) {
                phaseConfig.setResumeCondition(PhaseJobsConfig.ResumeCondition.SKIP);
            }
            StatusJob jobStatus = getScmChange(subJob, phaseConfig, multiJobBuild, listener, launcher);
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

            if (phaseConfig.getEnableCondition() && phaseConfig.getCondition() != null) {
                if (!evalCondition(phaseConfig.getCondition(), build, listener)) {
                    listener.getLogger().println(String.format("Skipping %s. Condition is evaluate to false.", subJob.getName()));
                    phaseCounters.processSkipped();
                    continue;
                }
                // This is needed because if no condition to eval, the legacy buildOnlyIfSCMChanges feature is still available,
                // so we don't need to change our job configuration.
            }


            if (phaseConfig.getEnableJobScript()) {
                boolean jobScriptEvalRes = true;
                if (phaseConfig.isRunJobScriptOnSlave() && !isMasterNode) {
                    ScriptConfig scriptConfig = ConfigFactory.getConfig(!phaseConfig.isRunJobScriptOnSlave(),
                                                                        phaseConfig.isJobScriptOnSlaveNode(),
                                                                        phaseConfig.isUseScriptFile(),
                                                                        phaseConfig.getScriptPath(),
                                                                        phaseConfig.getJobScript());
                    Pipe pipe = Pipe.createRemoteToLocal();
                    PipingTask piping = new PipingTask(pipe, scriptConfig);
                    piping.addVarMap(Utils.getBindings(phaseConfig.getJobBindings()));
                    piping.addPropMap(Utils.getEnvVars(build, listener));
                    Future<Boolean> task = launcher.getChannel().callAsync(piping);
                    String threadId = UUID.randomUUID().toString();
                    Thread t = new StreamCopyThread(threadId, pipe.getIn(), listener.getLogger());
                    t.start();
                    t.join();
                    try {
                        jobScriptEvalRes = task.get();
                    } catch (ExecutionException e) {
                        listener.getLogger().println(String.format("Skipping %s. Script evaluation is failed. ", subJob
                                .getName()));
                        phaseCounters.processSkipped();
                        jobScriptEvalRes = false;
                    }
                } else {
                    ScriptRunner runner = new ScriptRunner(build, listener);
                    Map<Object, Object> binding = new HashMap<Object, Object>();
                    binding.putAll(Utils.parseProperties(phaseConfig.getJobBindings()));
                    runner.bindVariablesMap(binding);
                    if (phaseConfig.isUseScriptFile() && null != phaseConfig.getScriptPath()) {
                        if (phaseConfig.isJobScriptOnSlaveNode()) {
                            jobScriptEvalRes = runner
                                    .evaluateOnSlaveFs(expandToken(phaseConfig.getScriptPath(), build, listener));
                        } else {
                            jobScriptEvalRes = runner
                                    .evaluateFromWorkspace(expandToken(phaseConfig.getScriptPath(), build, listener));
                        }
                    } else if (null != phaseConfig.getJobScript()) {
                        jobScriptEvalRes = runner.evaluate(phaseConfig.getJobScript());
                    }
                }
                if (!jobScriptEvalRes) {
                    listener.getLogger().println(String.format("Skipping %s. Script is evaluate to false.", subJob
                            .getName()));
                    phaseCounters.processSkipped();
                    continue;
                }
            }

            if (!jobStatus.isBuildable()) {
                phaseCounters.processSkipped();
                continue;
            }

            boolean isStart;

            if (phaseConfig.getResumeCondition().isEvaluate()) {
                if (phaseConfig.isResumeScriptOnSlaveNode() && !isMasterNode) {
                    ScriptConfig scriptConfig = ConfigFactory.getConfig(!phaseConfig.isResumeScriptOnSlaveNode(),
                                                                        phaseConfig.isResumeScriptOnSlaveNode(),
                                                                        phaseConfig.isUseResumeScriptFile(),
                                                                        phaseConfig.getResumeScriptPath(),
                                                                        phaseConfig.getResumeScriptText());
                    Pipe pipe = Pipe.createRemoteToLocal();
                    PipingTask piping = new PipingTask(pipe, scriptConfig);
                    piping.addVarMap(Utils.getBindings(phaseConfig.getResumeBindings()));
                    piping.addPropMap(Utils.getEnvVars(build, listener));
                    Future<Boolean> task = launcher.getChannel().callAsync(piping);
                    String threadId = UUID.randomUUID().toString();
                    Thread t = new StreamCopyThread(threadId, pipe.getIn(), listener.getLogger());
                    t.start();
                    t.join();
                    try {
                        isStart = task.get();
                    } catch (ExecutionException e) {
                        listener.getLogger().println(String.format("Skipping %s. Script evaluation is failed. ", subJob
                                .getName()));
                        phaseCounters.processSkipped();
                        isStart = true;
                    }
                } else {
                    ScriptRunner runner = new ScriptRunner(build, listener);
                    Map<Object, Object> binding = new HashMap<Object, Object>();
                    binding.putAll(Utils.parseProperties(phaseConfig.getResumeBindings()));
                    runner.bindVariablesMap(binding);
                    if (phaseConfig.isUseResumeScriptFile()) {
                        if (phaseConfig.isResumeScriptOnSlaveNode()) {
                            isStart = runner
                                    .evaluateOnSlaveFs(expandToken(phaseConfig.getResumeScriptPath(), build, listener));
                        } else {
                            isStart = runner.evaluateFromWorkspace(expandToken(phaseConfig.getResumeScriptPath(),
                                    build, listener));
                        }
                    } else {
                        isStart = runner.evaluate(phaseConfig.getResumeScriptText());
                    }
                }
            } else {
                isStart = phaseConfig.getResumeCondition().isStart();
            }

            if (isStart && successBuildMap.containsKey(subJob.getUrl())) {
                successBuildMap.remove(subJob.getUrl());
                listener.getLogger().println(String.format("Job %s will be executed. Script or condition is evaluate " +
                                                                   "to true.", subJob.getName()));
            }

            reportStart(listener, subJob);
            List<Action> actions = new ArrayList<Action>();

            if (resume) {
                SubBuild subBuild = failedBuildMap.get(subJob.getUrl());
                if (null != subBuild) {
                    AbstractProject prj = Jenkins.getInstance().getItem(subBuild.getJobName(), multiJobBuild.getParent(),
                            AbstractProject.class);
                    AbstractBuild childBuild = prj.getBuildByNumber(subBuild.getBuildNumber());
                    MultiJobResumeControl childControl = new MultiJobResumeControl(childBuild);
                    actions.add(childControl);
                }
            }

            prepareActions(multiJobBuild, subJob, phaseConfig, listener, actions, index);

            if (jobStatus == StatusJob.IS_DISABLED_AT_PHASECONFIG) {
                phaseCounters.processSkipped();
		continue;
            } else {
                boolean shouldTrigger = (null == successBuildMap.get(subJob.getUrl()));
                subTasks.add(new SubTask(subJob, phaseConfig, actions, multiJobBuild, shouldTrigger));
            }
        }

        if (subTasks.size() < 1) {
            // We inject the variables also when no jobs will be triggered.
            injectEnvVars(build, listener, phaseCounters.toMap());
            return true;
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
                        e.printStackTrace();
                    }
                }
            } else {
                AbstractBuild jobBuild = subTask.subJob.getBuildByNumber(subBuild.getBuildNumber());
                Result result = subBuild.getResult();
                updateSubBuild(multiJobBuild, thisProject, jobBuild, result);
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
                        Result result = subTask.result;
                        jobResults.add(result);
                        phaseCounters.process(result);
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

        return isContinue(jobResults);
    }

    private boolean isContinue(Set<Result> jobResults) {
        for (Result result : jobResults) {
            if (!continuationCondition.isContinue(result)
                    && !ignorePhaseResult.isContinue(result)) {
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

        public Boolean call() throws IOException {
            Result result = null;
            AbstractBuild jobBuild = null;
            try {
                int maxRetries = subTask.phaseConfig.getMaxRetries();
                if (!subTask.phaseConfig.getEnableRetryStrategy()) {
                    maxRetries = 0;
                }

                int retry = 0;
                boolean retryUsingListener = false;
                boolean finish = false;

                if (subTask.isShouldTrigger()) {
                    subTask.GenerateFuture();
                }

                while ((retry <= maxRetries || retryUsingListener) && !finish) {
                    retry++;
                    QueueTaskFuture<AbstractBuild> future = (QueueTaskFuture<AbstractBuild>) subTask.future;
                    MultiJobListener.fireOnStart(future.waitForStart(), subTask.multiJobBuild);
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

                        boolean isComplete = MultiJobListener.fireOnComplete(jobBuild, subTask.multiJobBuild);
                        result = jobBuild.getResult();

                        if (subTask.phaseConfig.getIgnoreJobResult().getMinSuccessResult().isWorseOrEqualTo(result)) {
                            result = Result.SUCCESS;
                        }

                        reportFinish(listener, jobBuild, result);
                        if (!isComplete) {
                            retryUsingListener = true;
                            listener.getLogger().println("Failure detected by job listener, retrying this build.");
                            updateSubBuild(subTask.multiJobBuild, multiJobProject, jobBuild, result, true);
                            subTask.GenerateFuture();
                        } else if (result.isWorseOrEqualTo(Result.FAILURE) && result.isCompleteBuild() && subTask.phaseConfig.getEnableRetryStrategy()) {
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
                }
            }

            if (jobBuild == null) {
                updateSubBuild(subTask.multiJobBuild, multiJobProject, subTask.phaseConfig);
            }
            queue.add(subTask);

            return true;
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
            PhaseJobsConfig.IgnoreJobResult ignoreJobResult = subTask.phaseConfig.getIgnoreJobResult();
            if (killCondition.equals(KillPhaseOnJobResultCondition.NEVER) && subTask.result != Result.ABORTED) {
                return false;
            }
            if (ignoreJobResult.getMinSuccessResult().isWorseOrEqualTo(subTask.result)) {
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
                phaseName, null, BallColor.NOTBUILT.getImage(), "not built", "", null, ignorePhaseResult.getMinSuccessResult());
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, null, jobBuild.getIconColor()
                        .getImage(), jobBuild.getDurationString(),
                jobBuild.getUrl(), jobBuild, ignorePhaseResult.getMinSuccessResult());
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild,
            Result result) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, result, jobBuild.getIconColor().getImage(),
                jobBuild.getDurationString(), jobBuild.getUrl(), jobBuild, ignorePhaseResult.getMinSuccessResult());
        multiJobBuild.addSubBuild(subBuild);
    }

    private void updateSubBuild(MultiJobBuild multiJobBuild,
            MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild,
            Result result, boolean retry) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                jobBuild.getNumber(), phaseName, result, jobBuild.getIconColor().getImage(),
                jobBuild.getDurationString(), jobBuild.getUrl(), retry, false, jobBuild, ignorePhaseResult.getMinSuccessResult());
        multiJobBuild.addSubBuild(subBuild);
    }

    private void abortSubBuild(MultiJobBuild multiJobBuild, MultiJobProject multiJobProject, AbstractBuild<?, ?> jobBuild) {
        SubBuild subBuild = new SubBuild(multiJobProject.getName(),
                                         multiJobBuild.getNumber(), jobBuild.getProject().getName(),
                                         jobBuild.getNumber(), phaseName, Result.ABORTED, BallColor.ABORTED.getImage
                (), "", jobBuild.getUrl(), false, true, jobBuild, ignorePhaseResult.getMinSuccessResult());
        multiJobBuild.addSubBuild(subBuild);
    }

    @SuppressWarnings("rawtypes")
    private void addBuildEnvironmentVariables(MultiJobBuild thisBuild,
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
        String buildName = jobBuild.getDisplayName();


        // If the job is run a second time, store the first job's number and result with unique keys
        if (variables.get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe) != null) {
            String runCount = Integer.toString(Integer.parseInt(variables
                    .get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe)));
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

    private static class MultiJobAction implements Action, QueueAction {

        /**
         * @deprecated Kept here for backward compatibility. For new builds will be {@code null}
         */
        @Deprecated
        public transient AbstractBuild build;

        public int index;
        private int buildNumber;

        public MultiJobAction(AbstractBuild build, int index) {
            this.index = index;
            this.buildNumber = build.getNumber();
        }

        protected Object readResolve() {
            if (0 == buildNumber && null != build) {
                buildNumber = build.getNumber();
            }
            build = null;
            return this;
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

    public String getScriptText() {
        return scriptText;
    }

    public void setScriptText(String scriptText) {
        this.scriptText = scriptText;
    }


    public boolean isEnableGroovyScript() {
        return enableGroovyScript;
    }

    public void setEnableGroovyScript(boolean enableGroovyScript) {
        this.enableGroovyScript = enableGroovyScript;
    }

    public boolean isUseScriptFile() {
        return isUseScriptFile;
    }

    public void setUseScriptFile(boolean isUseScriptFile) {
        this.isUseScriptFile = isUseScriptFile;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getBindings() {
        return bindings;
    }

    public void setBindings(String bindings) {
        this.bindings = bindings;
    }

    public boolean isScriptOnSlave() {
        return isScriptOnSlave;
    }

    public void setScriptOnSlave(boolean isScriptOnSlave) {
        this.isScriptOnSlave = isScriptOnSlave;
    }

    public boolean isRunOnSlave() {
        return isRunOnSlave;
    }

    public void setRunOnSlave(boolean isRunOnSlave) {
        this.isRunOnSlave = isRunOnSlave;
    }

    public enum ExecutionType {

        PARALLEL("Running multiple jobs in parallel") {
            @Override
            public boolean isParallel() {
                return true;
            }
        },
        SEQUENTIAL("Running multiple jobs sequentially") {
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

    public enum IgnorePhaseResult {
        NEVER("Never") {
            @Override
            public boolean isContinue(Result result) {
                return result.equals(Result.SUCCESS);
            }
            @Override
            public Result getMinSuccessResult() { return Result.SUCCESS; }
        },
        UNSTABLE("Stable or Unstable but not Failed") {
            @Override
            public boolean isContinue(Result result) {
                return result.isBetterOrEqualTo(Result.UNSTABLE);
            }
            @Override
            public Result getMinSuccessResult() { return Result.UNSTABLE; }
        },
        ALWAYS("Always") {
            @Override
            public boolean isContinue(Result result) {
                return true;
            }
            @Override
            public Result getMinSuccessResult() { return Result.FAILURE; }
        };

        abstract public boolean isContinue(Result result);
        abstract public Result getMinSuccessResult();

        private IgnorePhaseResult(String label) {
            this.label = label;
        }

        final private String label;

        public String getLabel() {
            return label;
        }
    }

    public void setExecutionType(ExecutionType executionType) {
        this.executionType = executionType;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public IgnorePhaseResult getIgnorePhaseResult() { return ignorePhaseResult; }

    public void setIgnorePhaseResult(IgnorePhaseResult ignorePhaseResult) { this.ignorePhaseResult = ignorePhaseResult; }

}
