package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractBuild;
import hudson.model.Action;

public class MultiJobResumeControl implements Action {

	private final AbstractBuild<?, ?> build;

	public MultiJobResumeControl(AbstractBuild<?, ?> build) {
		this.build = build;
	}

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return "Resume action";
	}

	public String getUrlName() {
		return "resumecontrol";
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}
}
