package com.tikal.jenkins.plugins.multijob;

import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ResumeCause extends Cause.UpstreamCause {

	public ResumeCause(Run<?, ?> up) {
		super(up);
	}

	@Override
	public String getShortDescription() {
		return Messages.ResumeCause_ShortDescription(getUpstreamBuild(), getUpstreamUrl());
	}

	@Override
	public void print(TaskListener listener) {
		listener.getLogger().println(Messages.ResumeCause_ShortDescription(getUpstreamBuild(), getUpstreamUrl()));
	}
}
