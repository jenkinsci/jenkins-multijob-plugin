package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.ProminentProjectAction;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

public class MultiJobResumeBuild implements ProminentProjectAction {

    private final AbstractBuild<?, ?> build;

    public MultiJobResumeBuild(AbstractBuild<?, ?> build) {
		this.build = build;
	}

    public String getIconFileName() {
		return "plugin/jenkins-multijob-plugin/tool32.png";
	}

    public String getDisplayName() {
		return "Resume build";
	}

    public String getUrlName() {
		return "resume";
	}

    public String getInfo() {
		return "Resume build";
	}

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        AbstractProject<?, ?> project = build.getProject();
        MultiJobResumeControl control = new MultiJobResumeControl(build);
        Cause.UserIdCause cause = build.getCause(Cause.UserIdCause.class);
        project.scheduleBuild(0, cause, control);

        rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + project.getUrl());
    }
}
