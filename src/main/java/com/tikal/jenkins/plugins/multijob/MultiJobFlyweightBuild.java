package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.scm.ChangeLogSet;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@ExportedBean(defaultVisibility = 999)
public class MultiJobFlyweightBuild extends Build<MultiJobFlyweightProject, MultiJobFlyweightBuild> {

	private List<MultiJobBuild.SubBuild> subBuilds;
	private MultiJobChangeLogSet changeSets = new MultiJobChangeLogSet(this);
	private Map<String, MultiJobBuild.SubBuild> subBuildsMap = new HashMap<String, MultiJobBuild.SubBuild>();

	public MultiJobFlyweightBuild(MultiJobFlyweightProject project) throws IOException {
		super(project);
	}

	public MultiJobFlyweightBuild(MultiJobFlyweightProject project, File buildDir) throws IOException {
		super(project, buildDir);
	}

	@Override
	public ChangeLogSet<? extends ChangeLogSet.Entry> getChangeSet() {
		return super.getChangeSet();
	}

	public void addChangeLogSet(ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet) {
		if (changeLogSet != null) {
			this.changeSets.addChangeLogSet(changeLogSet);
		}
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
		execute(new MultiJobFlyweightRunnerImpl());
	}

	public List<MultiJobBuild.SubBuild> getBuilders() {
		MultiJobFlyweightBuild multiJobBuild = getParent().getNearestBuild(getNumber());
		return multiJobBuild.getSubBuilds();
	}

	public String getBuildParams(MultiJobBuild.SubBuild subBuild) {
		try {
			AbstractProject project = (AbstractProject) Jenkins.getInstance()
				.getItem(subBuild.getJobName(), this.getParent(), AbstractProject.class);;
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
				buffer.append(
					"<input type='text' size='15' value='" + name
						+ "' readonly/>")
					.append("&nbsp;")
					.append("<input type='text' size='35' value='" + value
						+ "'/ readonly>").append("</br>");
			}
			return buffer.toString();
		} catch (Exception e) {
			return "Failed to retrieve build parameters.";
		}
	}

	public void addSubBuild(MultiJobBuild.SubBuild subBuild) {
		String key = subBuild.getPhaseName().concat(subBuild.getJobName())
			.concat(String.valueOf(subBuild.getBuildNumber()));
		if (subBuildsMap.containsKey(key)) {
			MultiJobBuild.SubBuild e = subBuildsMap.get(key);
			Collections.replaceAll(getSubBuilds(), e, subBuild);
		} else {
			getSubBuilds().add(subBuild);
		}
		subBuildsMap.put(key, subBuild);
	}

	@Exported
	public List<MultiJobBuild.SubBuild> getSubBuilds() {
		if (subBuilds == null) {
			subBuilds = new CopyOnWriteArrayList<MultiJobBuild.SubBuild>();
		}
		return subBuilds;
	}

	protected class MultiJobFlyweightRunnerImpl extends
		Build<MultiJobFlyweightProject, MultiJobFlyweightBuild>.BuildExecution {
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
			List<MultiJobBuild.SubBuild> builders = getBuilders();
			for (MultiJobBuild.SubBuild subBuild : builders) {
				if (!subBuild.isRetry() && !subBuild.isAbort()) {
					Result buildResult = subBuild.getResult();
					if (buildResult != null && buildResult.isWorseThan(result)) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
