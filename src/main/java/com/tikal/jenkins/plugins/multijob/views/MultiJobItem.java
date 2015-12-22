package com.tikal.jenkins.plugins.multijob.views;

import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;

public class MultiJobItem {

	private int itemId;
	private int parentItemId;

	private boolean isProject;
	private boolean isBuild;
	private boolean isConditional;

	private int buildNumber;

	private String name;
	private String url;
	private String buildName;
	private String buildUrl;
	private Result result;
	private String status;
	private String statusIconColor;
	private HealthReport healthReport;
	private String weather;
	private String weatherIconUrl;
	private String lastSuccess;
	private String lastFailure;
	private String lastDuration;

	public MultiJobItem(Job<?, ?> project, int buildNumber, int itemId, int parentItemId) {
		this.itemId = itemId;
		this.parentItemId = parentItemId;
		this.isProject = true;
		if (0 != buildNumber) {
			this.buildNumber = buildNumber;
		} else {
			this.buildNumber = null != project.getLastBuild() ? project.getLastBuild().getNumber() : 0;
		}
		this.isBuild = 0 == this.buildNumber ? false : true;
		this.name = project.getFullDisplayName();
		if (0 != buildNumber) {
			Run<?, ?> run = project.getBuildByNumber(buildNumber);
			this.buildName = name + " #" + buildNumber;
			this.buildUrl = Jenkins.getInstance().getRootUrl() + run.getUrl();
			this.result = run.getResult();
			this.statusIconColor = run.getIconColor().getImage();
		} else {
			this.result = Result.NOT_BUILT;
			this.statusIconColor = "nobuilt.png";
		}
		this.url = Jenkins.getInstance().getRootUrl() + project.getUrl();
		this.status = null != this.result ? this.result.toString() : "Not built yet";
		this.healthReport = project.getBuildHealth();
		this.weather = project.getBuildHealth().getDescription();
		this.weatherIconUrl = project.getBuildHealth().getIconUrl();
		this.lastSuccess = null != project.getLastSuccessfulBuild() ? project.getLastSuccessfulBuild()
			.getTimestampString() : "N/A";
		this.lastFailure = null != project.getLastFailedBuild() ? project.getLastFailedBuild()
			.getTimestampString() : "N/A";
		this.lastDuration = null != project.getLastBuild() ? project.getLastBuild().getDurationString() : "N/A";
	}

	public MultiJobItem(String name, Result result, String statusIconColor, HealthReport healthReport,
	                    boolean isConditional, int itemId, int
		parentItemId) {
		this.isConditional = isConditional;
		this.itemId = itemId;
		this.parentItemId = parentItemId;
		this.isProject = false;
		this.isBuild = false;
		this.name = name;
		this.result = result;
		this.status = result.toString();
		this.statusIconColor = statusIconColor;
		this.weather = healthReport.getDescription();
		this.weatherIconUrl = healthReport.getIconUrl();
		this.lastSuccess = "";
		this.lastFailure = "";
		this.lastDuration = "";
	}

	public int getItemId() {
		return itemId;
	}

	public int getParentItemId() {
		return parentItemId;
	}

	public boolean isConditional() {
		return isConditional;
	}

	public boolean isProject() {
		return isProject;
	}

	public boolean isBuild() {
		return isBuild;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getBuildName() {
		return buildName;
	}

	public String getBuildUrl() {
		return buildUrl;
	}

	public Result getResult() {
		return result;
	}

	public String getStatus() {
		return status;
	}

	public String getStatusIconColor() {
		return statusIconColor;
	}

	public HealthReport getHealthReport() {
		return healthReport;
	}

	public String getWeather() {
		return weather;
	}

	public String getWeatherIconUrl() {
		return weatherIconUrl;
	}

	public String getLastSuccess() {
		return lastSuccess;
	}

	public String getLastFailure() {
		return lastFailure;
	}

	public String getLastDuration() {
		return lastDuration;
	}

	public String getRun() {
		return null;
	}

}
