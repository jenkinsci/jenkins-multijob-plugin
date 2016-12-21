package com.tikal.jenkins.plugins.multijob;


import hudson.model.Action;
import hudson.model.Run;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import java.io.IOException;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Queue.Executable;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.ParameterizedJobMixIn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public final class SubTask {
    final public Job subJob;
    final public PhaseJobsConfig phaseConfig;
    final public List<Action> actions;
    public QueueTaskFuture<? extends Executable> future;
    final public MultiJobBuild multiJobBuild;
    public Result result;
    private boolean cancel;
    private boolean isShouldTrigger;

    SubTask(Job subJob, PhaseJobsConfig phaseConfig, List<Action> actions, MultiJobBuild multiJobBuild, boolean isShouldTrigger) {
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

    public void generateFuture() {
        Cause cause = new UpstreamCause((Run) multiJobBuild);
        List<Action> queueActions = actions;
        if (cause != null) {
            queueActions.add(new CauseAction(cause));
        }

        // Includes both traditional projects via AbstractProject and Workflow Job
        if (subJob instanceof ParameterizedJobMixIn.ParameterizedJob) {
            final ParameterizedJobMixIn<?, ?> parameterizedJobMixIn = new ParameterizedJobMixIn() {
                @Override
                protected Job<?, ?> asJob() {
                    return subJob;
                }
            };
            this.future = parameterizedJobMixIn.scheduleBuild2(0, queueActions.toArray(new Action[queueActions.size()]));
        }
    }
}