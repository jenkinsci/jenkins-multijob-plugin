package com.tikal.jenkins.plugins.multijob;

import hudson.model.Build;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;

public class MultiJobBuild extends Build<MultiJobProject, MultiJobBuild> {

	public MultiJobBuild(MultiJobProject project) throws IOException {
		super(project);
	}

	MultiJobChangeLogSet changeSets = new MultiJobChangeLogSet(this);

	@Override
	public ChangeLogSet<? extends Entry> getChangeSet() {
		return super.getChangeSet();
	}

	public void addChangeLogSet(ChangeLogSet<? extends Entry> changeLogSet) {
		this.changeSets.addChangeLogSet(changeLogSet);
	}

	public MultiJobBuild(MultiJobProject project, File buildDir)
			throws IOException {
		super(project, buildDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		run(new RunnerImpl());
	}

	protected class RunnerImpl extends
			Build<MultiJobProject, MultiJobBuild>.RunnerImpl {

	}

	public List<SubBuild> getBuilders() {

		MultiJobBuild multiJobBuild = getParent().getNearestBuild(getNumber());
		List<SubBuild> subBuilds = multiJobBuild.getSubBuilds();

		for (SubBuild subBuild : subBuilds) {
			Run build = getBuild(subBuild);
			if (build != null) {
				subBuild.setIcon(build.getIconColor().getImage());
				subBuild.setDuration(build.getDurationString());
				subBuild.setUrl(build.getUrl());
			} else {
				subBuild.setIcon("grey.png");
				subBuild.setDuration("not built yet");
				subBuild.setUrl(null);
			}
		}

		return subBuilds;
	}

	private Run getBuild(SubBuild subBuild) {

		Run build = null;
		List<AbstractProject> downstreamProjects = getProject()
				.getDownstreamProjects();
		for (AbstractProject downstreamProject : downstreamProjects) {
			List upstreamProjects = downstreamProject.getUpstreamProjects();
			if (upstreamProjects.contains(getProject())) {
				if (subBuild.getJobName().equalsIgnoreCase(
						downstreamProject.getName())) {
					build = downstreamProject.getBuildByNumber(subBuild
							.getBuildNumber());
				}
			}
		}
		return build;
	}

	public void addSubBuild(String parentJobName, int parentBuildNumber,
			String jobName, int buildNumber, String phaseName, AbstractBuild refBuild) {

		SubBuild subBuild = new SubBuild(parentJobName, parentBuildNumber,
				jobName, buildNumber, phaseName);

		for (SubBuild subbuild : getSubBuilds ())
		{
			if (subbuild.getJobName().equals(jobName))
			{
				getSubBuilds ().remove (subbuild);
				break;
			}
		}
		
		getSubBuilds().add(subBuild);

	}

	private List<SubBuild> subBuilds;

	public List<SubBuild> getSubBuilds() {

		if (subBuilds == null)
			subBuilds = new ArrayList<SubBuild>();

		return subBuilds;
	}

	public static class SubBuild {

		final String parentJobName;
		final int parentBuildNumber;
		final String jobName;
		final int buildNumber;
		final String phaseName;

		private String icon;
		private String duration;
		private String url;

		public SubBuild(String parentJobName, int parentBuildNumber,
				String jobName, int buildNumber, String phaseName) {
			this.parentJobName = parentJobName;
			this.parentBuildNumber = parentBuildNumber;
			this.jobName = jobName;
			this.buildNumber = buildNumber;
			this.phaseName = phaseName;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void setDuration(String duration) {
			this.duration = duration;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public String getDuration() {
			return duration;
		}

		public String getIcon() {
			return icon;
		}

		public String getUrl() {
			return url;
		}

		public String getPhaseName() {
			return phaseName;
		}

		public String getParentJobName() {
			return parentJobName;
		}

		public int getParentBuildNumber() {
			return parentBuildNumber;
		}

		public String getJobName() {
			return jobName;
		}

		public int getBuildNumber() {
			return buildNumber;
		}

		@Override
		public String toString() {
			return "SubBuild [parentJobName=" + parentJobName
					+ ", parentBuildNumber=" + parentBuildNumber + ", jobName="
					+ jobName + ", buildNumber=" + buildNumber + "]";
		}
	}
}
