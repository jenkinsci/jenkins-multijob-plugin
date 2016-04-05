package com.tikal.jenkins.plugins.multijob.views;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.Plugin;
import hudson.model.AbstractProject;
import hudson.model.HealthReport;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Messages;
import hudson.model.Result;
import hudson.model.User;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultiView {

	private List<MultiJobItem> multiJobItems;
	private HashMap<String, Map<String, List<MultiJobBuild.SubBuild>>> subBuilds;

	public MultiView(MultiJobProject multiJobProject) {
		this.multiJobItems = new ArrayList<MultiJobItem>();
		this.subBuilds = new HashMap<String, Map<String, List<MultiJobBuild.SubBuild>>>();
		MultiJobBuild build = multiJobProject.getLastBuild();

		int buildNumber = null == build ? 0 : build.getNumber();
		addBuildsLevel(subBuilds, build);

		addTopLevelProject(multiJobProject, buildNumber, multiJobItems);

		initUserProperty();
	}

	private void initUserProperty() {
		User user = Jenkins.getInstance().getUser(Jenkins.getAuthentication().getName());
		TableProperty property = user.getProperty(TableProperty.class);
		if (null == property) {
			property = TableProperty.DESCRIPTOR.newInstance(user);
			try {
				user.addProperty(property);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (Plugin.getInstance().isUserPropertyTooOld(property.getTimestamp())) {
			property.setColumnProps(Plugin.getInstance().getColumnMap());
		}
	}

	private void addBuildsLevel(Map<String, Map<String, List<MultiJobBuild.SubBuild>>> ret, MultiJobBuild build) {
		if (null != build) {
			for (MultiJobBuild.SubBuild subBuild : build.getBuilders()) {
				String phaseName = subBuild.getPhaseName();
				String jobName = subBuild.getJobName();
				Map<String, List<MultiJobBuild.SubBuild>> map = ret.get(phaseName);
				if (null == map) {
					map = new HashMap<String, List<MultiJobBuild.SubBuild>>();
				}
				List<MultiJobBuild.SubBuild> subList = map.get(jobName);
				if (null == subList) {
					subList = new LinkedList<MultiJobBuild.SubBuild>();
				}
				subList.add(subBuild);
				map.put(jobName, subList);
				ret.put(phaseName, map);
				Item it = Jenkins.getInstance().getItem(subBuild.getJobName(), build.getProject().getParent(),
														AbstractProject.class);
				if (null != it) {
					if (it instanceof MultiJobProject) {
						MultiJobProject prj = (MultiJobProject) it;
						MultiJobBuild b = prj.getBuildByNumber(subBuild.getBuildNumber());
						if (null != b) {
							addBuildsLevel(ret, b);
						}
					}
				}
			}
		}
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

		Map<String, List<MultiJobBuild.SubBuild>> phaseProjects = subBuilds.get(phaseName);
		if (null == phaseProjects) {
			phaseProjects = new HashMap<String, List<MultiJobBuild.SubBuild>>();
		}

		List<MultiJobItem> childs = new ArrayList<MultiJobItem>();
		for (PhaseJobsConfig projectConfig : subProjects) {
			Item it = Jenkins.getInstance().getItem(projectConfig.getJobName(), project.getParent(), AbstractProject
				.class);
			MultiJobBuild.SubBuild subBuild = null;
			List<MultiJobBuild.SubBuild> subBuilds = phaseProjects.get(it.getName());
			if (null != subBuilds && !subBuilds.isEmpty()) {
				subBuild = subBuilds.remove(0);
			}
			phaseProjects.put(it.getName(), subBuilds);
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

		for (MultiJobItem item : childs) {
			if (null != item.getResult()) {
				result = item.getResult().isWorseThan(result) ? item.getResult() : result;
				iconColor = result.color.getImage();
			} else {
				result = Result.NOT_BUILT;
				iconColor = item.getStatusIconColor();
			}

			HealthReport itemHealth = new HealthReport(item.getHealthScore(), item.getWeatherIconUrl(), item
					.getWeather());
			healthReport = HealthReport.min(healthReport, itemHealth);

		}

		MultiJobItem item = new MultiJobItem(phaseName, result, iconColor, healthReport.getDescription(),
											 healthReport.getIconUrl(), healthReport.getScore(), isConditional,
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
