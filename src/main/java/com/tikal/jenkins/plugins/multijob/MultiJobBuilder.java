package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.BallColor;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.json.JSONObject;

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

public class MultiJobBuilder extends Builder implements DependecyDeclarer {

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

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		Hudson hudson = Hudson.getInstance();
		MultiJobBuild multiJobBuild = (MultiJobBuild) build;
		MultiJobProject thisProject = multiJobBuild.getProject();

		Map<PhaseSubJob, PhaseJobsConfig> phaseSubJobs = new HashMap<PhaseSubJob, PhaseJobsConfig>(
				phaseJobs.size());

		for (PhaseJobsConfig phaseJobConfig : phaseJobs) {
			TopLevelItem item = hudson.getItem(phaseJobConfig.getJobName());
			if (item instanceof AbstractProject) {
				AbstractProject job = (AbstractProject) item;
				phaseSubJobs.put(new PhaseSubJob(job), phaseJobConfig);
			}
		}

		List<SubTask> subTasks = new ArrayList<SubTask>();
		for (PhaseSubJob phaseSubJob : phaseSubJobs.keySet()) {
			AbstractProject subJob = phaseSubJob.job;
			if (subJob.isDisabled()) {
				listener.getLogger().println(
						String.format(
								"Skipping %s. This Job has been disabled.",
								subJob.getName()));
				continue;
			}

			reportStart(listener, subJob);
			PhaseJobsConfig phaseConfig = phaseSubJobs.get(phaseSubJob);
			List<Action> actions = new ArrayList<Action>();
			prepareActions(multiJobBuild, subJob, phaseConfig, listener,
					actions);

			while (subJob.isInQueue()) {
				TimeUnit.SECONDS.sleep(subJob.getQuietPeriod());
			}

			Future<AbstractBuild> future = null;
			if (!phaseConfig.isDisableJob()) {
				future = subJob.scheduleBuild2(subJob.getQuietPeriod(),
						new UpstreamCause((Run) multiJobBuild),
						actions.toArray(new Action[0]));
			}

			if (future != null) {
				subTasks.add(new SubTask(future, phaseConfig));
			} else {
				listener.getLogger().println(
						String.format("Warning: %s sub job is disabled.",
								subJob.getName()));
			}
		}

		if (subTasks.size() < 1)
			return true;

		ExecutorService executor = Executors
				.newFixedThreadPool(subTasks.size());
		Set<Result> jobResults = new HashSet<Result>();
		BlockingQueue<SubTask> queue = new ArrayBlockingQueue<SubTask>(
				subTasks.size());
		for (SubTask subTask : subTasks) {
			Runnable worker = new SubJobWorker(thisProject, multiJobBuild,
					listener, subTask, queue);
			executor.execute(worker);
		}

		executor.shutdown();
		int resultCounter = 0;
		while (!executor.isTerminated()) {
			SubTask subTask = queue.take();
			resultCounter++;
			if (subTask.result != null) {
				jobResults.add(subTask.result);
				checkPhaseTermination(subTask, subTasks);
			}
			if (subTasks.size() == resultCounter)
				break;
		}

		executor.shutdownNow();

		for (Result result : jobResults) {
			if (!continuationCondition.isContinue(result)) {
				return false;
			}
		}

		return true;

	}

	private final class SubJobWorker extends Thread {

		MultiJobProject multiJobProject;
		MultiJobBuild multiJobBuild;
		BuildListener listener;
		SubTask subTask;
		BlockingQueue<SubTask> queue;

		SubJobWorker(MultiJobProject multiJobProject,
				MultiJobBuild multiJobBuild, BuildListener listener,
				SubTask subTask, BlockingQueue<SubTask> queue) {
			this.multiJobBuild = multiJobBuild;
			this.multiJobProject = multiJobProject;
			this.listener = listener;
			this.subTask = subTask;
			this.queue = queue;
		}

		public void run() {
			Result result = null;
			try {
				QueueTaskFuture<AbstractBuild> future = (QueueTaskFuture<AbstractBuild>) subTask.future;
				AbstractBuild jobBuild = null;
				while (true) {
					if (future.isCancelled() && jobBuild == null) {
						updateSubBuild(multiJobBuild, multiJobProject,
								subTask.phaseConfig);
						break;
					}
					try {
						jobBuild = (AbstractBuild) future.getStartCondition().get(5,
								TimeUnit.SECONDS);
						updateSubBuild(multiJobBuild, multiJobProject, jobBuild);
					} catch (Exception e) {
						if (e instanceof TimeoutException)
							continue;
						else {
							throw e;
						}
					}
					if (future.isDone())
						break;
				}
				if (jobBuild != null) {
					result = jobBuild.getResult();
					updateSubBuild(multiJobBuild, multiJobProject, jobBuild,
							result);
					ChangeLogSet<Entry> changeLogSet = jobBuild.getChangeSet();
					multiJobBuild.addChangeLogSet(changeLogSet);
					reportFinish(listener, jobBuild, result);
					addBuildEnvironmentVariables(multiJobBuild, jobBuild,
							listener);
					subTask.result = result;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			queue.add(subTask);
		}

	}

	boolean checkPhaseTermination(SubTask subTask, List<SubTask> subTasks) {
		try {
			KillPhaseOnJobResultCondition killCondition = subTask.phaseConfig
					.getKillPhaseOnJobResultCondition();
			if (killCondition.equals(KillPhaseOnJobResultCondition.NEVER))
				return false;
			if (killCondition.isKillPhase(subTask.result)) {
				for (SubTask _subTask : subTasks)
					_subTask.future.cancel(true);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static final class SubTask {
		Future<AbstractBuild> future;
		PhaseJobsConfig phaseConfig;
		Result result;

		SubTask(Future<AbstractBuild> future, PhaseJobsConfig phaseConfig) {
			this.future = future;
			this.phaseConfig = phaseConfig;
		}
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
						+ " with status :"
						+ HyperlinkNote.encodeTo('/' + jobBuild.getUrl()
								+ "/console", result.toString()));
	}

	private void updateSubBuild(MultiJobBuild multiJobBuild,
			MultiJobProject multiJobProject, PhaseJobsConfig phaseConfig) {
		SubBuild subBuild = new SubBuild(multiJobProject.getName(),
				multiJobBuild.getNumber(), phaseConfig.getJobName(), 0,
				phaseName, null, BallColor.NOTBUILT.getImage(), "not built", "");
		multiJobBuild.addSubBuild(subBuild);
	}

	private void updateSubBuild(MultiJobBuild multiJobBuild,
			MultiJobProject multiJobProject, AbstractBuild jobBuild) {
		SubBuild subBuild = new SubBuild(multiJobProject.getName(),
				multiJobBuild.getNumber(), jobBuild.getProject().getName(),
				jobBuild.getNumber(), phaseName, null, jobBuild.getIconColor()
						.getImage(), jobBuild.getDurationString(),
				jobBuild.getUrl());
		multiJobBuild.addSubBuild(subBuild);
	}

	private void updateSubBuild(MultiJobBuild multiJobBuild,
			MultiJobProject multiJobProject, AbstractBuild jobBuild,
			Result result) {
		SubBuild subBuild = new SubBuild(multiJobProject.getName(),
				multiJobBuild.getNumber(), jobBuild.getProject().getName(),
				jobBuild.getNumber(), phaseName, result, jobBuild
						.getIconColor().getImage(),
				jobBuild.getDurationString(), jobBuild.getUrl());
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

	@SuppressWarnings("rawtypes")
	private void prepareActions(AbstractBuild build, AbstractProject project,
			PhaseJobsConfig projectConfig, BuildListener listener,
			List<Action> actions) throws IOException, InterruptedException {
		List<Action> parametersActions = null;
		// if (projectConfig.hasProperties()) {
		parametersActions = (List<Action>) projectConfig.getActions(build,
				listener, project, projectConfig.isCurrParams());
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
		Hudson hudson = Hudson.getInstance();
		List<PhaseJobsConfig> phaseJobsConfigs = getPhaseJobs();

		if (phaseJobsConfigs == null)
			return;
		for (PhaseJobsConfig project : phaseJobsConfigs) {
			TopLevelItem topLevelItem = hudson.getItem(project.getJobName());
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

		SUCCESSFUL("Successful") {
			@Override
			public boolean isContinue(Result result) {
				return result.equals(Result.SUCCESS);
			}
		},
		UNSTABLE("Stable or Unstable but not Failed") {
			@Override
			public boolean isContinue(Result result) {
				return result.isBetterOrEqualTo(Result.UNSTABLE);
			}
		},
		COMPLETED("Complete (always continue)") {
			@Override
			public boolean isContinue(Result result) {
				return result.equals(Result.ABORTED) ? true : result
						.isBetterOrEqualTo(Result.FAILURE);
			}
		},
		FAILURE("Failed") {
			@Override
			public boolean isContinue(Result result) {
				return result.equals(Result.ABORTED) ? false : result
						.isWorseOrEqualTo(Result.FAILURE);
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
