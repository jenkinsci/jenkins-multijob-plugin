package com.tikal.jenkins.plugins.multijob.views;

public class BuildState {

	final int previousBuildNumber;

	final int lastBuildNumber;

	final int lastSuccessBuildNumber;

	final int lastFailureBuildNumber;

	public BuildState(int previousBuildNumber, int lastBuildNumber, int lastSuccessBuildNumber, int lastFailureBuildNumber) {
		this.previousBuildNumber = previousBuildNumber;
		this.lastBuildNumber = lastBuildNumber;
		this.lastSuccessBuildNumber = lastSuccessBuildNumber;
		this.lastFailureBuildNumber = lastFailureBuildNumber;
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
