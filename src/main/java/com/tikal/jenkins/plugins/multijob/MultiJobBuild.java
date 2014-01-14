package com.tikal.jenkins.plugins.multijob;

import hudson.model.Action;
import hudson.model.BallColor;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class MultiJobBuild extends Build<MultiJobProject, MultiJobBuild> {

	// private Map<String, SubBuild> currentSubBuilds = new
	// LinkedHashMap<String, MultiJobBuild.SubBuild>();
	private MultiJobChangeLogSet changeSets = new MultiJobChangeLogSet(this);

	public MultiJobBuild(MultiJobProject project) throws IOException {
		super(project);
	}

	@Override
	public ChangeLogSet<? extends Entry> getChangeSet() {
		return super.getChangeSet();
	}

	public void addChangeLogSet(ChangeLogSet<? extends Entry> changeLogSet) {
		if (changeLogSet != null) {
			this.changeSets.addChangeLogSet(changeLogSet);
		}
	}

	public MultiJobBuild(MultiJobProject project, File buildDir)
			throws IOException {
		super(project, buildDir);
	}

	@Override
	public synchronized void doStop(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		super.doStop(req, rsp);
	}

	@Override
	public void addAction(Action a) {
		super.addAction(a);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		run(new MultiJobRunnerImpl());
	}

	public List<SubBuild> getBuilders() {
		MultiJobBuild multiJobBuild = getParent().getNearestBuild(getNumber());
		List<SubBuild> subBuilds = multiJobBuild.getSubBuilds();
		for (SubBuild subBuild : subBuilds) {
			Run build = getBuild(subBuild);
			if (build != null) {
				subBuild.setResult(build.getResult());
				subBuild.setIcon(build.getIconColor().getImage());
				subBuild.setDuration(build.getDurationString());
				subBuild.setUrl(build.getUrl());
			} else {
				subBuild.setIcon(BallColor.NOTBUILT.getImage());
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

	public void addSubBuild(MultiJobBuilder multiJobBuilder,
			String parentJobName, int parentBuildNumber, String jobName,
			int buildNumber, String phaseName) {
		String key = phaseName.concat(jobName).concat(
				String.valueOf(buildNumber));
		if (!filterSubBuilds.contains(key)) {
			filterSubBuilds.add(key);
			SubBuild subBuild = new SubBuild(parentJobName, parentBuildNumber,
					jobName, buildNumber, phaseName);
			getSubBuilds().add(subBuild);
		}
	}

	private List<SubBuild> subBuilds;
	private Set<String> filterSubBuilds = new HashSet<String>();

	@Exported
	public List<SubBuild> getSubBuilds() {
		if (subBuilds == null)
			subBuilds = new CopyOnWriteArrayList<SubBuild>();
		return subBuilds;
	}

	protected class MultiJobRunnerImpl extends
			Build<MultiJobProject, MultiJobBuild>.RunnerImpl {
		@Override
		public Result run(BuildListener listener) throws Exception {
			Result result = super.run(listener);
			if (isAborted())
				return Result.ABORTED;
			if (isFailure())
				return Result.FAILURE;
			if (isUnstable())
				return Result.UNSTABLE;
			return result;
		}

		private boolean isAborted() {
			return evaluateResult(Result.FAILURE);
		}

		private boolean isFailure() {
			return evaluateResult(Result.UNSTABLE);
		}

		private boolean isUnstable() {
			return evaluateResult(Result.SUCCESS);
		}

		private boolean evaluateResult(Result result) {
			List<SubBuild> builders = getBuilders();
			for (SubBuild subBuild : builders) {
				Result buildResult = subBuild.getResult();
				if (buildResult != null && buildResult.isWorseThan(result)) {
					return true;
				}
			}
			return false;
		}
	}

	@ExportedBean(defaultVisibility = 999)
	public static class SubBuild {

		private final String parentJobName;
		private final int parentBuildNumber;
		private final String jobName;
		private final int buildNumber;
		private final String phaseName;

		private Result result;
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

		@Exported
		public String getJobName() {
			return jobName;
		}

		@Exported
		public int getBuildNumber() {
			return buildNumber;
		}

		public void setResult(Result result) {
			this.result = result;
		}

		@Exported
		public Result getResult() {
			return result;
		}

		@Override
		public String toString() {
			return "SubBuild [parentJobName=" + parentJobName
					+ ", parentBuildNumber=" + parentBuildNumber + ", jobName="
					+ jobName + ", buildNumber=" + buildNumber + "]";
		}
	}
}