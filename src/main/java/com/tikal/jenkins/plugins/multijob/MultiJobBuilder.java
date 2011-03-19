package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
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

import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;

public class MultiJobBuilder extends Builder implements DependecyDeclarer {

	private String phaseName;
	private List<PhaseJobsConfig> phaseJobs;
	private ContinuationCondition continuationCondition = ContinuationCondition.SUCCESSFUL;

	@DataBoundConstructor
	public MultiJobBuilder(String phaseName, List<PhaseJobsConfig> phaseJobs, ContinuationCondition continuationCondition) {
		this.phaseName = phaseName;
		this.phaseJobs = Util.fixNull(phaseJobs);
		this.continuationCondition = continuationCondition;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		Hudson hudson = Hudson.getInstance();
		MultiJobBuild thisBuild = (MultiJobBuild) build;
		MultiJobProject thisProject = thisBuild.getProject();
		Map<AbstractProject, PhaseJobsConfig> projects = new HashMap<AbstractProject, PhaseJobsConfig>(phaseJobs.size());
		for (PhaseJobsConfig project : phaseJobs) {
			TopLevelItem item = hudson.getItem(project.getJobName());
			if (item instanceof AbstractProject) {
				AbstractProject job = (AbstractProject) item;
				projects.put(job, project);
			}
		}

		List<Future<Build>> futuresList = new ArrayList<Future<Build>>();

		for (AbstractProject project : projects.keySet()) {
			listener.getLogger().printf("Starting build job %s.\n", HyperlinkNote.encodeTo('/' + project.getUrl(), project.getFullName()));
			PhaseJobsConfig projectConfig = projects.get(project);
			List<Action> actions = new ArrayList<Action>();
			prepareActions(build, project, projectConfig, listener, actions);
			Future future = project.scheduleBuild2(project.getQuietPeriod(), new UpstreamCause((Run) build), actions.toArray(new Action[0]));
			if (future != null) {
				futuresList.add(future);
			}
		}
		boolean failed = false;
		for (Future future : futuresList) {
			if (failed) {
				future.cancel(true);
			}
			try {
				Build jobBuild = (Build) future.get();
				Result result = jobBuild.getResult();
				listener.getLogger().printf("Job '%s' finished: %s.\n",
						HyperlinkNote.encodeTo('/' + jobBuild.getProject().getUrl(), jobBuild.getProject().getFullName()), result);
				if (!continuationCondition.isContinue(jobBuild)) {
					failed = true;
				}
				addSubBuild(thisBuild, thisProject, jobBuild);
			} catch (ExecutionException e) {
				failed = true;
			}
		}
		return !failed;
	}

	@SuppressWarnings("rawtypes")
	private void addSubBuild(MultiJobBuild thisBuild, MultiJobProject thisProject, Build jobBuild) {
		SubBuild subBuild = new SubBuild(thisProject.getName(), thisBuild.getNumber(), jobBuild.getProject().getName(), jobBuild.getNumber());
		thisBuild.getSubBuilds().add(subBuild);
	}

	@SuppressWarnings("rawtypes")
	private void prepareActions(AbstractBuild build, AbstractProject project, PhaseJobsConfig projectConfig, BuildListener listener, List<Action> actions)
			throws IOException, InterruptedException {
		ParametersAction parametersAction = null;
		if (projectConfig.hasProperties())
			parametersAction = (ParametersAction) projectConfig.getAction(build, listener);
		else
			parametersAction = build.getAction(ParametersAction.class);

		actions.add(parametersAction);
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
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return req.bindJSON(MultiJobBuilder.class, formData);
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) {
			save();
			return true;
		}

	}

	@SuppressWarnings("rawtypes")
	public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
		Hudson hudson = Hudson.getInstance();
		List<PhaseJobsConfig> phaseJobsConfigs = getPhaseJobs();

		if (phaseJobsConfigs == null)
			return;
		for (PhaseJobsConfig project : phaseJobsConfigs) {
			TopLevelItem topLevelItem = hudson.getItem(project.getJobName());
			if (topLevelItem instanceof AbstractProject) {
				Dependency dependency = new Dependency(owner, (AbstractProject) topLevelItem) {

					@Override
					public boolean shouldTriggerBuild(AbstractBuild build, TaskListener listener, List<Action> actions) {
						return false;
					}

				};
				graph.addDependency(dependency);
			}
		}
	}

	public boolean onJobRenamed(String oldName, String newName) {
		boolean changed = false;
		for (Iterator<PhaseJobsConfig> i = phaseJobs.iterator(); i.hasNext();) {
			PhaseJobsConfig phaseJobs = (PhaseJobsConfig) i.next();
			String jobName = phaseJobs.getJobName();
			if (newName != null && jobName.trim().equals(oldName)) {
				phaseJobs.setJobName(newName);
				changed = true;
			} else if (newName == null) {
				i.remove();
			}
		}
		return changed;
	}

	public boolean onJobDeleted(String oldName) {
		return onJobRenamed(oldName, null);
	}

	@SuppressWarnings("rawtypes")
	public static enum ContinuationCondition {

		SUCCESSFUL("Successful") {
			@Override
			public boolean isContinue(Build build) {
				return build.getResult().equals(Result.SUCCESS);
			}
		},
		UNSTABLE("Stable or Unstable but not Failed") {
			@Override
			public boolean isContinue(Build build) {
				return build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
			}
		},
		COMPLETED("Complete (always continue)") {
			@Override
			public boolean isContinue(Build build) {
				return build.getResult().isBetterOrEqualTo(Result.FAILURE);
			}
		};

		abstract public boolean isContinue(Build build);

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

	public void setContinuationCondition(ContinuationCondition continuationCondition) {
		this.continuationCondition = continuationCondition;
	}
}
