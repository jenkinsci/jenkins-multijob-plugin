package com.tikal.jenkins.plugins.reactor;

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

public class ReactorBuilder extends Builder implements DependecyDeclarer {

	private List<ReactorSubProjectConfig> subProjects;

	final private String reactorName;

	private ContinuationCondition continuationCondition = ContinuationCondition.SUCCESSFUL;

	@DataBoundConstructor
	public ReactorBuilder(String reactorName, List<ReactorSubProjectConfig> subProjects, ContinuationCondition continuationCondition) {
		this.reactorName = reactorName;
		this.subProjects = (List<ReactorSubProjectConfig>) new ArrayList<ReactorSubProjectConfig>(Util.fixNull(subProjects));
		this.continuationCondition = continuationCondition;
	}
	
	@SuppressWarnings("rawtypes")
	private Map<AbstractProject,ReactorSubProjectConfig> getSubJobs() {
		Hudson hudson = Hudson.getInstance();
		Map<AbstractProject,ReactorSubProjectConfig> projects = new HashMap<AbstractProject,ReactorSubProjectConfig>(
				subProjects.size());
		for (ReactorSubProjectConfig project : subProjects) {
			TopLevelItem item = hudson.getItem(project.getJobName());
			if (item instanceof AbstractProject) {
				AbstractProject job = (AbstractProject) item;
				projects.put(job,project);
			}
		}
		return projects;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		Map<AbstractProject,ReactorSubProjectConfig> projects = getSubJobs();

		List<Future<Build>> futuresList = new ArrayList<Future<Build>>();

		for (AbstractProject project : projects.keySet()) {
			listener.getLogger().printf("Starting build job %s.\n", HyperlinkNote.encodeTo('/'+project.getUrl(), project.getFullName()) );
			ReactorSubProjectConfig projectConfig = projects.get(project);
			List<Action> actions = new ArrayList<Action>();
			prepareActions(build, project, projectConfig ,listener,actions);
			Future future = project.scheduleBuild2(project.getQuietPeriod(),
					new UpstreamCause((Run) build),
					actions.toArray(new Action[0]));
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
						HyperlinkNote.encodeTo('/'+jobBuild.getProject().getUrl(), jobBuild.getProject().getFullName()), result);
				if (!continuationCondition.isContinue(jobBuild)) {
					failed = true;
				}
			} catch (ExecutionException e) {
				failed = true;
			}
		}
		return !failed;
	}

	@SuppressWarnings("rawtypes")
	private void prepareActions(AbstractBuild build, AbstractProject project,
			ReactorSubProjectConfig projectConfig, BuildListener listener,List<Action> actions) throws IOException, InterruptedException {
		ParametersAction parametersAction = null;
		if (projectConfig.hasProperties())
			parametersAction = (ParametersAction) projectConfig.getAction(build,listener);
		else
			parametersAction = build.getAction(ParametersAction.class);
		
		actions.add(parametersAction);
	}

	public String getReactorName() {
		return reactorName;
	}

	public List<ReactorSubProjectConfig> getSubProjects() {
		return subProjects;
	}

	public void setSubProjects(List<ReactorSubProjectConfig> jobs) {
		subProjects = jobs;
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
			return "Reactor Phase";
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
    public boolean onJobRenamed(String oldName, String newName) {
        boolean changed = false;
        for (Iterator i = subProjects.iterator(); i.hasNext(); ) {
        	ReactorSubProjectConfig subProject = (ReactorSubProjectConfig) i.next();
        	String jobName = subProject.getJobName();
        	if (newName != null && jobName.trim().equals(oldName)) {
        		subProject.setJobName(newName);
                changed = true;
        	} else if (newName == null) {
        		i.remove();
        	}
        }
        return changed;
    }

    public boolean onDeleted(String oldName) {
        return onJobRenamed(oldName, null);
    }

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
