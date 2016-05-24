package com.tikal.jenkins.plugins.multijob;

import hudson.console.HyperlinkNote;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ResumeCause extends Cause.UpstreamCause {

	private Boolean afterRestart;

	public ResumeCause(Run<?, ?> up) {
		super(up);
		this.afterRestart = false;
	}

	public ResumeCause(Run<?, ?> up, Boolean afterRestart) {
		this(up);
		this.afterRestart = null != afterRestart ? afterRestart : false;
	}

	@Override
	public String getShortDescription() {
		if (afterRestart) {
			return Messages.ResumeCauseAfterRestart_ShortDescription(getUpstreamBuild(), getUpstreamUrl());
		}
		return Messages.ResumeCause_ShortDescription(getUpstreamBuild(), getUpstreamUrl());
	}

	@Override
	public void print(TaskListener listener) {
		StringBuilder sb = new StringBuilder();
		sb.append("[MultiJob] Resume build ");
		sb.append(HyperlinkNote.encodeTo(getUpstreamUrl(), "#" + String.valueOf(getUpstreamBuild())));
		if (afterRestart) {
			sb.append(" after Jenkins restart");
		}
		listener.getLogger().println(sb.toString());
	}
}
