package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

public class MultiJobResumeBuild implements RunAction2 {

    private transient final AbstractBuild<?, ?> build;

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
        Cause cause = !build.getCauses().isEmpty() ? build.getCauses().get(0) : null;
        project.scheduleBuild(0, cause, control);

        rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + project.getUrl());
    }

    public void onAttached(Run<?, ?> run) {
    }

    public void onLoad(Run<?, ?> run) {
    }
}
