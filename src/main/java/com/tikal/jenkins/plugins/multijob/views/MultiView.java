package com.tikal.jenkins.plugins.multijob.views;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.model.AbstractProject;
import hudson.model.HealthReport;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Messages;
import hudson.model.Result;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiView {

	private List<MultiJobItem> multiJobItems;
	private Map<String, Map<String, MultiJobBuild.SubBuild>> subBuilds;

	public MultiView(MultiJobProject multiJobProject) {
		this.multiJobItems = new ArrayList<MultiJobItem>();
		this.subBuilds = new HashMap<String, Map<String, MultiJobBuild.SubBuild>>();
		MultiJobBuild build = multiJobProject.getLastBuild();
		int buildNumber = 0;
		if (null != build) {
			buildNumber = build.getNumber();
			for (MultiJobBuild.SubBuild subBuild : build.getBuilders()) {
				String phaseName = subBuild.getPhaseName();
				String jobName = subBuild.getJobName();
				Map<String, MultiJobBuild.SubBuild> map = subBuilds.get(phaseName);
				if (null == map) {
					map = new HashMap<String, MultiJobBuild.SubBuild>();
				}
				map.put(jobName, subBuild);
				subBuilds.put(phaseName, map);
			}
		}

		addTopLevelProject(multiJobProject, buildNumber, multiJobItems);
	}

	public List<MultiJobItem> getHierarchy() {
		return multiJobItems;
	}

	private void addTopLevelProject(MultiJobProject project, int buildNumber, List<MultiJobItem> ret) {
		addMultiProject(project, buildNumber, 0, 0, ret);
	}

	@SuppressWarnings("rawtypes")
	private int addProjectFromBuilder(MultiJobProject project, List<MultiJobItem> ret,
	                                   BuildStep builder, int count, int level, boolean isConditional) {
		int currentCount = count + 1;
		int phaseId = currentCount;
		MultiJobBuilder reactorBuilder = (MultiJobBuilder) builder;
		List<PhaseJobsConfig> subProjects = reactorBuilder.getPhaseJobs();
		String phaseName = reactorBuilder.getPhaseName();

		Map<String, MultiJobBuild.SubBuild> phaseProjects = subBuilds.get(phaseName);
		if (null == phaseProjects) {
			phaseProjects = new HashMap<String, MultiJobBuild.SubBuild>();
		}

		List<MultiJobItem> childs = new ArrayList<MultiJobItem>();
		for (PhaseJobsConfig projectConfig : subProjects) {
			Item it = Jenkins.getInstance().getItem(projectConfig.getJobName(), project.getParent(), AbstractProject
				.class);
			MultiJobBuild.SubBuild subBuild = phaseProjects.get(it.getName());
			int buildNumber = null != subBuild ? subBuild.getBuildNumber() : 0;
			if (it instanceof MultiJobProject) {
				MultiJobProject subProject = (MultiJobProject) it;
				currentCount = addMultiProject(subProject, buildNumber, phaseId, currentCount, childs);
			} else {
				Job subProject = (Job) it;
				if (null == subProject) continue;
				addSimpleProject(subProject, buildNumber, ++currentCount, phaseId, childs);
			}
		}

		HealthReport healthReport = new HealthReport(0, "health-80plus.png",
			Messages._HealthReport_EmptyString());
		Result result = Result.SUCCESS;
		String iconColor = result.color.getImage();

		System.out.println("PHASE = " + phaseName);
		for (MultiJobItem item : childs) {
			if (null != item.getResult()) {
				System.out.println("child = " + item.getName() + " result = " + item.getResult().toString());
				result = item.getResult().isWorseThan(result) ? item.getResult() : result;
				iconColor = result.color.getImage();
			} else {
				result = Result.NOT_BUILT;
				iconColor = item.getStatusIconColor();
			}

			healthReport = HealthReport.min(healthReport, item.getHealthReport());
		}

		MultiJobItem item = new MultiJobItem(phaseName, result, iconColor, healthReport, isConditional,
			phaseId, level);
		ret.add(item);
		ret.addAll(childs);

		return currentCount;
	}

	private int addMultiProject(MultiJobProject project, int buildNumber, int level, int count,
	                            List<MultiJobItem> ret) {
		int currentCount = count;
		ret.add(new MultiJobItem(project, buildNumber, ++currentCount, level));

		for (Builder builder : project.getBuilders()) {
			if (builder instanceof MultiJobBuilder) {
				currentCount = addProjectFromBuilder(project, ret, builder, currentCount, count + 1, false);
			} else if (builder instanceof ConditionalBuilder) {
				final List<BuildStep> conditionalBuilders = ((ConditionalBuilder) builder).getConditionalbuilders();
				for (BuildStep buildStep : conditionalBuilders) {
					currentCount = addProjectFromBuilder(project, ret, buildStep, currentCount, count + 1, true);
				}
			} else if (builder instanceof SingleConditionalBuilder) {
				final BuildStep buildStep = ((SingleConditionalBuilder) builder).getBuildStep();
				if (buildStep instanceof MultiJobBuilder) {
					currentCount = addProjectFromBuilder(project, ret, buildStep, currentCount, count + 1,
						true);
				}
			}
		}
		return currentCount;
	}

	@SuppressWarnings("rawtypes")
	private void addSimpleProject(Job project, int buildNumber, int count, int level, List<MultiJobItem> ret) {
		ret.add(new MultiJobItem(project, buildNumber, count, level));
	}

}