package com.tikal.jenkins.plugins.multijob;


import hudson.model.Action;
import hudson.model.Run;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;

import java.util.List;
import java.util.concurrent.Future;

public final class SubTask {
    final public AbstractProject subJob;
    final public PhaseJobsConfig phaseConfig;
    final public List<Action> actions;
    public Future<AbstractBuild> future;
    final public MultiJobBuild multiJobBuild;
    public Result result;
    private boolean cancel;
    private boolean isShouldTrigger;

    SubTask(AbstractProject subJob, PhaseJobsConfig phaseConfig, List<Action> actions, MultiJobBuild multiJobBuild, boolean isShouldTrigger) {
        this.subJob = subJob;
        this.phaseConfig = phaseConfig;
        this.actions = actions;
        this.multiJobBuild = multiJobBuild;
        this.cancel = false;
        this.isShouldTrigger = isShouldTrigger;
    }

    public boolean isShouldTrigger() {
        return isShouldTrigger;
    }

    public boolean isCancelled() {
        return cancel;
    }

    public void cancelJob() {
        this.cancel = true;
    }

    public void GenerateFuture() {
        this.future = subJob.scheduleBuild2(subJob.getQuietPeriod(),
                                            new UpstreamCause((Run) multiJobBuild),
                                            actions.toArray(new Action[actions.size()]));
    }
}