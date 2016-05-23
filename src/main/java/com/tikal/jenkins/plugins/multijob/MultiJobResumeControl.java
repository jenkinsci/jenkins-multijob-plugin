package com.tikal.jenkins.plugins.multijob;

import hudson.model.Action;
import hudson.model.Run;

public class MultiJobResumeControl implements Action {

    private transient Run<?, ?> run;

    public MultiJobResumeControl(Run<?, ?> run) {
        this.run = run;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    public Run<?, ?> getRun() {
        return run;
    }
}
