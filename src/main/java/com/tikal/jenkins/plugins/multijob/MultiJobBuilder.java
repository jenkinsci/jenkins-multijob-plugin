package com.tikal.jenkins.plugins.multijob;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
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
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectBuilderContributionAction;
import org.jenkinsci.plugins.envinject.service.EnvInjectActionSetter;
import org.jenkinsci.plugins.envinject.service.EnvInjectEnvVars;
import org.jenkinsci.plugins.envinject.service.EnvInjectVariableGetter;

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
		MultiJobBuild thisBuild = (MultiJobBuild) build;
		MultiJobProject thisProject = thisBuild.getProject();
		Map<AbstractProjectKey, PhaseJobsConfig> projects = new HashMap<AbstractProjectKey, PhaseJobsConfig>(
				phaseJobs.size());

		for (PhaseJobsConfig project : phaseJobs) {
			TopLevelItem item = hudson.getItem(project.getJobName());
			if (item instanceof AbstractProject) {
				AbstractProject job = (AbstractProject) item;
				projects.put(new AbstractProjectKey(job), project);
			}
		}

		List<Future<Build>> futuresList = new ArrayList<Future<Build>>();
		List<AbstractProject> projectList = new ArrayList<AbstractProject>();
		for (AbstractProjectKey projectKey : projects.keySet()) {
			AbstractProject project = projectKey.getProject();
			listener.getLogger().printf(
					"Starting build job %s.\n",
					HyperlinkNote.encodeTo('/' + project.getUrl(),
							project.getFullName()));

			PhaseJobsConfig projectConfig = projects.get(projectKey);
			List<Action> actions = new ArrayList<Action>();
			prepareActions(build, project, projectConfig, listener, actions);
			Future future = project.scheduleBuild2(project.getQuietPeriod(),
					new UpstreamCause((Run) build),
					actions.toArray(new Action[0]));
			if (future != null) {
				futuresList.add(future);
				projectList.add(project);
			}
			// Wait a second before next build start.
				Thread.sleep(1000);
		}

		boolean failed = false;
		boolean canContinue = true;
		while (!futuresList.isEmpty() && !failed) {
			for (Future future : futuresList) {
				AbstractProject project = projectList.get(futuresList
						.indexOf(future));
				if (future.isDone() && !future.isCancelled()) {
					try {
						AbstractBuild jobBuild = (AbstractBuild) future.get();
						Result result = jobBuild.getResult();
						ChangeLogSet<Entry> changeLogSet = jobBuild
								.getChangeSet();
						if (changeLogSet != null) {
							((MultiJobBuild) build)
									.addChangeLogSet(changeLogSet);
						}
						listener.getLogger().println(
								"Finished Build : "
										+ HyperlinkNote.encodeTo(
												"/" + jobBuild.getUrl() + "/",
												String.valueOf(jobBuild
														.getDisplayName()))
										+ " of Job : "
										+ HyperlinkNote.encodeTo('/' + jobBuild
												.getProject().getUrl(),
												jobBuild.getProject()
														.getFullName())
										+ " with status :"
										+ HyperlinkNote.encodeTo(
												'/' + jobBuild.getUrl()
														+ "/console/",
												result.toString()));
						if (!continuationCondition.isContinue(jobBuild)) {
							failed = true;
						}
						addSubBuild(thisBuild, thisProject, jobBuild);
						addBuildEnvironmentVariables(thisBuild, jobBuild, listener);
						projectList.remove(project);
						futuresList.remove(future);
						break;
					} catch (ExecutionException e) {
						failed = true;
					}
				} else if (project.isBuilding()) {
					addSubBuild(thisBuild, thisProject,
							(AbstractBuild) project.getLastBuild());
				}
			}
			// Wait a second before next check.
			try{
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				for(Future<Build> future : futuresList)
					future.cancel(true);
				throw new InterruptedException();
			}
		}
		if (failed) {
			for (Future future : futuresList)
				future.cancel(true);
		}
		canContinue = !failed;
		return canContinue;
	}

	@SuppressWarnings("rawtypes")
	private void addSubBuild(MultiJobBuild thisBuild,
			MultiJobProject thisProject, AbstractBuild jobBuild) {
		thisBuild.addSubBuild(thisProject.getName(), thisBuild.getNumber(),
				jobBuild.getProject().getName(), jobBuild.getNumber(),
				phaseName, jobBuild);
	}

	@SuppressWarnings("rawtypes")
	private void addBuildEnvironmentVariables(MultiJobBuild thisBuild, AbstractBuild jobBuild, BuildListener listener) {
		// Env variables map
		Map<String, String> variables = new HashMap<String, String>();
		
		String jobName = jobBuild.getProject().getName();
		String jobNameSafe = jobName.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
		String buildNumber = Integer.toString(jobBuild.getNumber());
		String buildResult = jobBuild.getResult().toString();
		
		// These will always reference the last build
		variables.put("LAST_TRIGGERED_JOB_NAME", jobName);
		variables.put(jobNameSafe + "_BUILD_NUMBER", buildNumber);
		variables.put(jobNameSafe + "_BUILD_RESULT", buildResult);

		if (variables.get("TRIGGERED_JOB_NAMES") == null) {
			variables.put("TRIGGERED_JOB_NAMES", jobName);
		} else {
			String triggeredJobNames = variables.get("TRIGGERED_JOB_NAMES") + "," + jobName;
			variables.put("TRIGGERED_JOB_NAMES", triggeredJobNames);
		}

		if (variables.get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe) == null) {
			variables.put("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe, "1");
		} else {
			String runCount = Integer.toString(Integer.parseInt(variables.get("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe)) + 1);
			variables.put("TRIGGERED_BUILD_RUN_COUNT_" + jobNameSafe, runCount);
		}


		//Set the new build variables map
		addEnvVars(thisBuild, listener, variables);
	}
	
	private void addEnvVars(AbstractBuild<?,?> build, BuildListener listener, Map<String,String> incomingVars) {
		
		EnvInjectLogger logger = new EnvInjectLogger(listener);
        FilePath ws = build.getWorkspace();
        EnvInjectActionSetter envInjectActionSetter = new EnvInjectActionSetter(ws);
        EnvInjectEnvVars envInjectEnvVarsService = new EnvInjectEnvVars(logger);

        try {

            EnvInjectVariableGetter variableGetter = new EnvInjectVariableGetter();
            Map<String, String> previousEnvVars = variableGetter.getEnvVarsPreviousSteps(build, logger);

            //Get current envVars
            Map<String, String> variables = new HashMap<String, String>(previousEnvVars);

            //Resolve variables
            final Map<String, String> resultVariables = envInjectEnvVarsService.getMergedVariables(variables, incomingVars);

            //Set the new build variables map
            build.addAction(new EnvInjectBuilderContributionAction(resultVariables));

            //Add or get the existing action to add new env vars
            envInjectActionSetter.addEnvVarsToEnvInjectBuildAction(build, resultVariables);
        } catch (Throwable throwable) {
            listener.getLogger().println("[MultiJob] - [ERROR] - Problems occurs on injecting env vars as a build step: " + throwable.getMessage());
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

		ParametersAction currParametersAction = null;
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

	private final static class AbstractProjectKey {

		private AbstractProject project;

		AbstractProjectKey(AbstractProject project) {
			this.project = project;
		}

		public AbstractProject getProject() {
			return project;
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
			public boolean isContinue(AbstractBuild build) {
				return build.getResult().equals(Result.SUCCESS);
			}
		},
		UNSTABLE("Stable or Unstable but not Failed") {
			@Override
			public boolean isContinue(AbstractBuild build) {
				return build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
			}
		},
		COMPLETED("Complete (always continue)") {
			@Override
			public boolean isContinue(AbstractBuild build) {
				return build.getResult().equals(Result.ABORTED) ? true : 
					build.getResult().isBetterOrEqualTo(Result.FAILURE);
			}
		};

		abstract public boolean isContinue(AbstractBuild build);

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
