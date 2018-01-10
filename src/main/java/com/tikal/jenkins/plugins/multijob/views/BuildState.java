package com.tikal.jenkins.plugins.multijob.views;

import hudson.model.AbstractProject;

public class BuildState {

    final String jobName;

    final int previousBuildNumber;

    final int lastBuildNumber;

    final int lastSuccessBuildNumber;

    final int lastFailureBuildNumber;

    public BuildState(String jobName, int previousBuildNumber,
                      int lastBuildNumber, int lastSuccessBuildNumber, int lastFailureBuildNumber) {
        this.jobName = jobName;
        this.previousBuildNumber = previousBuildNumber;
        this.lastBuildNumber = lastBuildNumber;
        this.lastSuccessBuildNumber = lastSuccessBuildNumber;
        this.lastFailureBuildNumber = lastFailureBuildNumber;
    }

    public String getJobName() {
        return jobName;
    }

    public int getPreviousBuildNumber() {
        return previousBuildNumber;
    }

    public int getLastBuildNumber() {
        return lastBuildNumber;
    }

    public int getLastSuccessBuildNumber() {
        return lastSuccessBuildNumber;
    }

    public int getLastFailureBuildNumber() {
        return lastFailureBuildNumber;
    }
}
