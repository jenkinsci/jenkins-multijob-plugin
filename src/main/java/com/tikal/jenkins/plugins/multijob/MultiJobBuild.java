package com.tikal.jenkins.plugins.multijob;

import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class MultiJobBuild extends Build<MultiJobProject, MultiJobBuild> {

	private List<SubBuild> subBuilds;
	private MultiJobChangeLogSet changeSets = new MultiJobChangeLogSet(this);
	private Map<String, SubBuild> subBuildsMap = new HashMap<String, SubBuild>();

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
		execute(new MultiJobRunnerImpl());
	}

	public List<SubBuild> getBuilders() {
		MultiJobBuild multiJobBuild = getParent().getNearestBuild(getNumber());
		return multiJobBuild.getSubBuilds();
	}

	public String getBuildParams(SubBuild subBuild) {
		try {
			AbstractProject project = (AbstractProject) Jenkins.getInstance()
					.getItem(subBuild.getJobName());
			Run build = project.getBuildByNumber(subBuild.getBuildNumber());
			ParametersAction action = build.getAction(ParametersAction.class);
			List<ParameterValue> parameters = action.getParameters();
			StringBuffer buffer = new StringBuffer();
			for (ParameterValue parameterValue : parameters) {
				StringParameterValue stringParameter;
				try {
					stringParameter = ((StringParameterValue) parameterValue);
				} catch (Exception e) {
					continue;
				}
				String value = stringParameter.value;
				String name = stringParameter.getName();
				buffer
					.append("<input type='text' size='15' value='").append(name) .append("' readonly/>&nbsp;")
					.append("<input type='text' size='35' value='").append(value).append("' readonly/><br/>");
			}
			return buffer.toString();
		} catch (Exception e) {
			return "Failed to retrieve build parameters.";
		}
	}

	public void addSubBuild(SubBuild subBuild) {
		String key = subBuild.getPhaseName().concat(subBuild.getJobName())
				.concat(String.valueOf(subBuild.getBuildNumber()));
		if (subBuildsMap.containsKey(key)) {
			SubBuild e = subBuildsMap.get(key);
			Collections.replaceAll(getSubBuilds(), e, subBuild);
		} else {
			getSubBuilds().add(subBuild);
		}
		subBuildsMap.put(key, subBuild);
	}

	@Exported
	public List<SubBuild> getSubBuilds() {
		if (subBuilds == null)
			subBuilds = new CopyOnWriteArrayList<SubBuild>();
		return subBuilds;
	}

	protected class MultiJobRunnerImpl extends
			Build<MultiJobProject, MultiJobBuild>.BuildExecution {
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
		private final Result result;
		private final String icon;
		private final String duration;
		private final String url;

		public SubBuild(String parentJobName, int parentBuildNumber,
				String jobName, int buildNumber, String phaseName,
				Result result, String icon, String duration, String url) {
			this.parentJobName = parentJobName;
			this.parentBuildNumber = parentBuildNumber;
			this.jobName = jobName;
			this.buildNumber = buildNumber;
			this.phaseName = phaseName;
			this.result = result;
			this.icon = icon;
			this.duration = duration;
			this.url = url;
		}

		@Exported
		public String getDuration() {
			return duration;
		}

		@Exported
		public String getIcon() {
			return icon;
		}

		@Exported
		public String getUrl() {
			return url;
		}

		@Exported
		public String getPhaseName() {
			return phaseName;
		}

		@Exported
		public String getParentJobName() {
			return parentJobName;
		}

		@Exported
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
