package com.tikal.jenkins.plugins.multijob;

import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.List;

public class MultiJobResumeBuild implements RunAction2 {

    private transient Run<?, ?> run;

    public MultiJobResumeBuild(Run<?, ?> run) {
        this.run = run;
    }

    public String getIconFileName() {
		return "plugin/jenkins-multijob-plugin/tool32.png";
	}

    public String getDisplayName() {
		return Messages.MultiJobResumeBuild_DisplayName();
	}

    public String getUrlName() {
		return "resume";
	}

    public String getInfo() {
		return "Resume build";
	}

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        // if rebuild plugin is available let it handle build resuming
        if (Utils.rebuildPluginAvailable()) {
            run.replaceAction(new WasResumedAction()); // add or replace existing one
            rsp.sendRedirect2("../rebuild");
            return;
        }

        final MultiJobResumeControl control = new MultiJobResumeControl(run);
        List<Action> actions = Utils.copyBuildCauses(run);
        actions.add(control);
        actions.add(new CauseAction(new ResumeCause(run)));
        Jenkins.getInstance().getQueue().schedule2((Queue.Task) run.getParent(), 0, actions);
        rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + run.getParent().getUrl());
    }

    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }


}
