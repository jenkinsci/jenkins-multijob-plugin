package com.tikal.jenkins.plugins.reactor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.Hudson;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.DependencyGraph.Dependency;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ReactorBuilder extends Builder implements DependecyDeclarer {

	final private List<ReactorSubProjectConfig> subProjects;

	final private String reactorName;

	@DataBoundConstructor
	public ReactorBuilder(String reactorName,
			List<ReactorSubProjectConfig> subProjects) {
		this.reactorName = reactorName;
		this.subProjects = subProjects;
	}

	// @SuppressWarnings("rawtypes")
	// private List<AbstractProject> getSubProjects() {
	// Hudson hudson = Hudson.getInstance();
	// String[] jobNames = getJobNames().split(" ");
	// List<AbstractProject> projects = new
	// ArrayList<AbstractProject>(jobNames.length);
	// for (String jobName : jobNames) {
	// TopLevelItem item = hudson.getItem(jobName);
	// if (item instanceof AbstractProject) {
	// AbstractProject project = (AbstractProject) item;
	// projects.add(project);
	// }
	// }
	// return projects;
	// }

	// @Override
	// @SuppressWarnings("rawtypes")
	// public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
	// BuildListener listener) throws InterruptedException, IOException {
	// List<AbstractProject> projects = getSubProjects();
	//
	// List<Future<Build>> futuresList = new ArrayList<Future<Build>>();
	//
	// for (AbstractProject project : projects) {
	// listener.getLogger().printf("Starting build job - '%s'\n",
	// project.getName());
	//
	// List<Action> actions = new ArrayList<Action>();
	// prepareActions(build, project, actions);
	// Future future = project.scheduleBuild2(project.getQuietPeriod(), new
	// UpstreamCause((Run) build), actions.toArray(new Action[0]));
	// if (future != null) {
	// futuresList.add(future);
	// }
	// }
	// boolean failed = false;
	// for (Future future : futuresList) {
	// if (failed) {
	// future.cancel(true);
	// }
	// try {
	// Build jobBuild = (Build) future.get();
	// Result result = jobBuild.getResult();
	// listener.getLogger().printf("Job '%s' finished: %s.\n",
	// jobBuild.getProject().getName(), result);
	// if (Result.FAILURE.equals(result) || Result.ABORTED.equals(result)) {
	// failed = true;
	// }
	// } catch (ExecutionException e) {
	// failed = true;
	// }
	// }
	// return !failed;
	// }

	@SuppressWarnings("rawtypes")
	private void prepareActions(AbstractBuild build, AbstractProject project,
			List<Action> actions) {
		ParametersAction parametersAction = build
				.getAction(ParametersAction.class);
		actions.add(parametersAction);
	}

	public String getReactorName() {
		return reactorName;
	}

	public List<ReactorSubProjectConfig> getSubProjects() {
		return subProjects;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return jobType.equals(TikalReactorProject.class);
		}

		@Override
		public String getDisplayName() {
			return "Reactor";
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return req.bindJSON(ReactorBuilder.class, formData);
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) {
			save();
			return true;
		}
		

	}

	public void buildDependencyGraph(AbstractProject owner,
			DependencyGraph graph) {
		// ReactorSubProjectConfig[] jobNames = getSubProjects();
		Hudson hudson = Hudson.getInstance();
		if (getSubProjects() == null)
			return;
		for (ReactorSubProjectConfig project : getSubProjects()) {
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
}
