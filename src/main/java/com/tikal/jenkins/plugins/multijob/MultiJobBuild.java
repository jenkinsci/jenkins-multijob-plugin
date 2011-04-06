package com.tikal.jenkins.plugins.multijob;

import hudson.model.Build;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiJobBuild extends Build<MultiJobProject, MultiJobBuild> {

	public MultiJobBuild(MultiJobProject project) throws IOException {
		super(project);
	}

	ChangeLogSet<? extends Entry> changeLogSet = ChangeLogSet.createEmpty(this);

	@Override
	public ChangeLogSet<? extends Entry> getChangeSet() {
		System.out.println("MultiJobBuild.getChangeSet() " + changeLogSet);
		return changeLogSet;
	}

	public void setChangeLogSet(ChangeLogSet<? extends Entry> changeLogSet) {
		System.out.println("MultiJobBuild.setChangeLogSet() " + changeLogSet);
		this.changeLogSet = changeLogSet;
	}

	public MultiJobBuild(MultiJobProject project, File buildDir) throws IOException {
		super(project, buildDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		run(new RunnerImpl());
	}

	protected class RunnerImpl extends Build<MultiJobProject, MultiJobBuild>.RunnerImpl {

	}

	private List<SubBuild> subBuilds;

	public List<SubBuild> getSubBuilds() {
		if (subBuilds == null) {
			subBuilds = new ArrayList<SubBuild>();
		}
		return subBuilds;
	}

	public static class SubBuild {
		final String parentJobName;
		final int parentBuildNumber;
		final String jobName;
		final int buildNumber;

		public SubBuild(String parentJobName, int parentBuildNumber, String jobName, int buildNumber) {
			this.parentJobName = parentJobName;
			this.parentBuildNumber = parentBuildNumber;
			this.jobName = jobName;
			this.buildNumber = buildNumber;
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
			return "SubBuild [parentJobName=" + parentJobName + ", parentBuildNumber=" + parentBuildNumber + ", jobName=" + jobName + ", buildNumber="
					+ buildNumber + "]";
		}
	}
}
