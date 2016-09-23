package com.tikal.jenkins.plugins.multijob.views;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Result;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
	private String weather;
	private String weatherIconUrl;
	private String lastSuccess;
	private String lastFailure;
	private String lastDuration;
	private int healthScore;

	public MultiJobItem(Job<?, ?> project, int buildNumber, int itemId, int parentItemId, String lastSuccess, String
			lastFailure) {
		this.itemId = itemId;
		this.parentItemId = parentItemId;
		this.isProject = true;
		if (0 != buildNumber) {
			this.buildNumber = buildNumber;
		} else {
			this.buildNumber = null != project.getLastBuild() ? project.getLastBuild().getNumber() : 0;
		}
		this.isBuild = 0 != this.buildNumber;
		this.name = project.getDisplayName();
		this.url = "/".concat(project.getParent().getUrl()).concat(project.getShortUrl());
		if (0 != buildNumber) {
			AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) project.getBuildByNumber(buildNumber);
			this.buildName = "#".concat(String.valueOf(buildNumber));
			this.buildUrl = url.concat(String.valueOf(buildNumber));

			this.result = build.getResult();
			this.statusIconColor = build.getIconColor().getImage();
			this.lastDuration = build.getDurationString();
		} else {
			this.result = Result.NOT_BUILT;
			this.statusIconColor = "nobuilt.png";
			this.lastDuration = "N/A";
		}
		this.status = null != this.result ? this.result.toString() : "Not built yet";
		HealthReport health = project.getBuildHealth();
		this.weather = health.getDescription();
		this.weatherIconUrl = health.getIconUrl();
		this.healthScore = health.getScore();
		this.lastSuccess = lastSuccess;
		this.lastFailure = lastFailure;
	}


    public MultiJobItem(Job<?, ?> project, int buildNumber, int itemId, int parentItemId, String lastSuccess, String
            lastFailure, Result minSuccessResult) {
        this.itemId = itemId;
        this.parentItemId = parentItemId;
        this.isProject = true;
        if (0 != buildNumber) {
            this.buildNumber = buildNumber;
        } else {
            this.buildNumber = null != project.getLastBuild() ? project.getLastBuild().getNumber() : 0;
        }
        this.isBuild = 0 != this.buildNumber;
        this.name = project.getDisplayName();
        this.url = "/".concat(project.getParent().getUrl()).concat(project.getShortUrl());
        if (0 != buildNumber) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) project.getBuildByNumber(buildNumber);
            this.buildName = "#".concat(String.valueOf(buildNumber));
            this.buildUrl = url.concat(String.valueOf(buildNumber));
            this.result = minSuccessResult.isWorseOrEqualTo(build.getResult()) ? Result.SUCCESS : build.getResult();
            this.statusIconColor = build.getIconColor().getImage();
            this.lastDuration = build.getDurationString();
        } else {
            this.result = Result.NOT_BUILT;
            this.statusIconColor = "nobuilt.png";
            this.lastDuration = "N/A";
        }
        this.status = null != this.result ? this.result.toString() : "Not built yet";
        HealthReport health = project.getBuildHealth();
        this.weather = health.getDescription();
        this.weatherIconUrl = health.getIconUrl();
        this.healthScore = health.getScore();
        this.lastSuccess = lastSuccess;
        this.lastFailure = lastFailure;
    }


    public MultiJobItem(String name, Result result, String statusIconColor, String weather, String weatherIconUrl,
						int healthScore, boolean isConditional, int itemId, int
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
		this.weather = weather;
		this.weatherIconUrl = weatherIconUrl;
		this.healthScore = healthScore;
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

	public int getBuildNumber() {
		return buildNumber;
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

	public String getWeather() {
		return weather;
	}

	public String getWeatherIconUrl() {
		return weatherIconUrl;
	}

	public int getHealthScore() {
		return healthScore;
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

	@Override
	public int hashCode() {
		return new HashCodeBuilder(getItemId() % 2 == 0 ? getItemId() + 1 : getItemId(), 31).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj || obj.getClass() != this.getClass()) {
			return false;
		}
		MultiJobItem item = (MultiJobItem) obj;
		return item.getItemId() == this.getItemId();
	}
}
