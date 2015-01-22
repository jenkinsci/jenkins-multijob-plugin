package com.tikal.jenkins.plugins.multijob;


import hudson.model.Action;
import hudson.model.Run;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;
import hudson.model.queue.QueueTaskFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

public final class SubTask {
    final public AbstractProject subJob;
    final public PhaseJobsConfig phaseConfig;
    final public List<Action> actions;
    public QueueTaskFuture<AbstractBuild> future;
    final public MultiJobBuild multiJobBuild;
    public Result result;
    private boolean cancel;

    SubTask(AbstractProject subJob, PhaseJobsConfig phaseConfig, List<Action> actions, MultiJobBuild multiJobBuild) {
        this.subJob = subJob;
        this.phaseConfig = phaseConfig;
        this.actions = actions;
        this.multiJobBuild = multiJobBuild;
        this.cancel = false;
        GenerateFuture();
    }

    public boolean isCancelled() {
        return cancel;
    }

    public void cancelJob() {
        this.cancel = true;
        
        // Cancel the job in the queue
        if (this.future != null) {
            this.future.cancel(false);
        }
    }

    public void GenerateFuture() {
        this.future = (QueueTaskFuture<AbstractBuild>) subJob.scheduleBuild2(subJob.getQuietPeriod(),
                                            new UpstreamCause((Run) multiJobBuild),
                                                                             actions);
    }
}