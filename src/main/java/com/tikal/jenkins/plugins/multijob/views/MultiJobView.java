package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.Indenter;
import hudson.Util;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.views.ListViewColumn;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;

public class MultiJobView extends ListView {

	@DataBoundConstructor
	public MultiJobView(String name) {
		super(name);
	}

	public MultiJobView(String name, ViewGroup owner) {
		super(name, owner);
	}

	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		public String getDisplayName() {
			return "MultiJob View";
		}

		/**
		 * Checks if the include regular expression is valid.
		 */
		public FormValidation doCheckIncludeRegex(@QueryParameter String value) throws IOException, ServletException, InterruptedException {
			String v = Util.fixEmpty(value);
			if (v != null) {
				try {
					Pattern.compile(v);
				} catch (PatternSyntaxException pse) {
					return FormValidation.error(pse.getMessage());
				}
			}
			return FormValidation.ok();
		}
	}

	@Override
	public List<TopLevelItem> getItems() 
	{
        List<TopLevelItem> items = super.getItems ();
        
		//Expand MultiProjects
		List<TopLevelItem> out = new ArrayList<TopLevelItem>();
		for (TopLevelItem item : items) {
			if (item instanceof MultiJobProject) {
				MultiJobProject project = (MultiJobProject) item;
				if (project.isTopMost()) {
					addTopLevelProject(project, out);
				}
			}
			else
			{
				out.add (item);
			}
		}
	
		return out;
	}

	public List<TopLevelItem> getRootItem(MultiJobProject multiJobProject) {
		List<TopLevelItem> out = new ArrayList<TopLevelItem>();
		addTopLevelProject(multiJobProject, out);
		return out;
	}

	private void addTopLevelProject(MultiJobProject project, List<TopLevelItem> out) {
		addMultiProject(null, project, createBuildState(project), 0, null, out);
	}

	@SuppressWarnings("rawtypes")
	private void addMultiProject(MultiJobProject parent, MultiJobProject project, BuildState buildState, int nestLevel, String phaseName, List<TopLevelItem> out) {
		out.add(new ProjectWrapper(parent, project, buildState, nestLevel));
		List<Builder> builders = project.getBuilders();
		for (Builder builder : builders) {
			int phaseNestLevel = nestLevel + 1;
			if (builder instanceof MultiJobBuilder) {
				MultiJobBuilder reactorBuilder = (MultiJobBuilder) builder;
				List<PhaseJobsConfig> subProjects = reactorBuilder.getPhaseJobs();
				String currentPhaseName = reactorBuilder.getPhaseName();
				PhaseWrapper phaseWrapper = new PhaseWrapper(phaseNestLevel, currentPhaseName);
				out.add(phaseWrapper);
				for (PhaseJobsConfig projectConfig : subProjects) {
					TopLevelItem tli = Hudson.getInstance().getItem(projectConfig.getJobName());
					if (tli instanceof MultiJobProject) {
						MultiJobProject subProject = (MultiJobProject) tli;
						BuildState jobBuildState = createBuildState(buildState, project, subProject);
						phaseWrapper.addChildBuildState(jobBuildState);
						addMultiProject(project, subProject, jobBuildState, phaseNestLevel + 1, currentPhaseName, out);
					} else {
						AbstractProject subProject = (AbstractProject) tli;
						BuildState jobBuildState = createBuildState(buildState, project, subProject);
						phaseWrapper.addChildBuildState(jobBuildState);
						addSimpleProject(project, subProject, jobBuildState, phaseNestLevel + 1, out);
					}
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void addSimpleProject(MultiJobProject parent, AbstractProject project, BuildState buildState, int nestLevel, List<TopLevelItem> out) {
		out.add(new ProjectWrapper(parent, project, buildState, nestLevel));
	}

	@SuppressWarnings({ "rawtypes" })
	private BuildState createBuildState(BuildState parentBuildState, MultiJobProject multiJobProject, AbstractProject project) {
		int previousBuildNumber = 0;
		int lastBuildNumber = 0;
		int lastSuccessBuildNumber = 0;
		int lastFailureBuildNumber = 0;
		MultiJobBuild previousParentBuild = multiJobProject.getBuildByNumber(parentBuildState.getPreviousBuildNumber());
		MultiJobBuild lastParentBuild = multiJobProject.getBuildByNumber(parentBuildState.getLastBuildNumber());
		MultiJobBuild lastParentSuccessBuild = multiJobProject.getBuildByNumber(parentBuildState.getLastSuccessBuildNumber());
		MultiJobBuild lastParentFailureBuild = multiJobProject.getBuildByNumber(parentBuildState.getLastFailureBuildNumber());
		if (previousParentBuild != null) {
			List<SubBuild> subBuilds = previousParentBuild.getSubBuilds();
			for (SubBuild subBuild : subBuilds) {
				if (subBuild.getJobName().equals(project.getName())) {
					previousBuildNumber = subBuild.getBuildNumber();
				}
			}
		}
		if (lastParentBuild != null) {
			List<SubBuild> subBuilds = lastParentBuild.getSubBuilds();
			for (SubBuild subBuild : subBuilds) {
				if (subBuild.getJobName().equals(project.getName())) {
					lastBuildNumber = subBuild.getBuildNumber();
				}
			}
		}
		if (lastParentSuccessBuild != null) {
			List<SubBuild> subBuilds = lastParentSuccessBuild.getSubBuilds();
			for (SubBuild subBuild : subBuilds) {
				if (subBuild.getJobName().equals(project.getName())) {
					AbstractBuild build = (AbstractBuild) project.getBuildByNumber(subBuild.getBuildNumber());
					if (build != null) {
						lastSuccessBuildNumber = subBuild.getBuildNumber();
						break;
					}
				}
			}
		}
		if (lastParentFailureBuild != null) {
			List<SubBuild> subBuilds = lastParentFailureBuild.getSubBuilds();
			for (SubBuild subBuild : subBuilds) {
				if (subBuild.getJobName().equals(project.getName())) {
					AbstractBuild build = (AbstractBuild)   project.getBuildByNumber(subBuild.getBuildNumber());
					if (build != null) {
						lastFailureBuildNumber = subBuild.getBuildNumber();
						break;
					}
				}
			}
		}
		
		return new BuildState(project.getName(), previousBuildNumber, lastBuildNumber, lastSuccessBuildNumber, lastFailureBuildNumber);
	}

	private BuildState createBuildState(MultiJobProject project) {

		MultiJobBuild lastBuild = project.getLastBuild();
		MultiJobBuild previousBuild = lastBuild == null ? null : lastBuild.getPreviousBuild();
		MultiJobBuild lastSuccessfulBuild = project.getLastSuccessfulBuild();
		MultiJobBuild lastFailedBuild = project.getLastFailedBuild();
		return new BuildState(project.getName(), previousBuild == null ? 0 : previousBuild.getNumber(), lastBuild == null ? 0 : lastBuild.getNumber(),
				lastSuccessfulBuild == null ? 0 : lastSuccessfulBuild.getNumber(), lastFailedBuild == null ? 0 : lastFailedBuild.getNumber());
	}

	@Override
	protected void submit(StaplerRequest req) throws ServletException, FormException, IOException 
	{
			super.submit (req);
	}

	protected void initColumns() {
		try {
			Field field = ListView.class.getDeclaredField("columns");
			field.setAccessible(true);
			field.set(this, new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(this, MultiJobListViewColumn.createDefaultInitialColumnList()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public Indenter<Job> createIndenter() {
		return new Indenter<Job>() {

			protected int getNestLevel(Job job) {
				if ((TopLevelItem) job instanceof ProjectWrapper) {
					ProjectWrapper projectWrapper = (ProjectWrapper) (TopLevelItem) job;
					return projectWrapper.getNestLevel();
				}
				return 0;
			}
		};
	}
}
