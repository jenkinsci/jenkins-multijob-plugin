package com.tikal.jenkins.plugins.multijob;


import hudson.model.Action;
import hudson.model.Run;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause.UpstreamCause;
import hudson.model.queue.QueueTaskFuture;

import java.util.List;


public final class SubTask {
    final public AbstractProject subJob;
    final public PhaseJobsConfig phaseConfig;
    final public List<Action> actions;
    public QueueTaskFuture<AbstractBuild> future;
    final public MultiJobBuild multiJobBuild;
    final transient public BuildListener listener;
    public Result result;
    private boolean cancel;

    SubTask(AbstractProject subJob, PhaseJobsConfig phaseConfig, List<Action> actions, MultiJobBuild multiJobBuild) throws InstantiationException {
        this(subJob, phaseConfig, actions, multiJobBuild, null);
    }

    SubTask(AbstractProject subJob, PhaseJobsConfig phaseConfig, List<Action> actions, MultiJobBuild multiJobBuild, BuildListener listener) throws InstantiationException  {
        this.subJob = subJob;
        this.phaseConfig = phaseConfig;
        this.actions = actions;
        this.multiJobBuild = multiJobBuild;
        this.cancel = false;
        this.listener = listener;
        GenerateFuture();
    }

    public boolean isCancelled() {
        return cancel;
    }

    public void cancelJob() {
        this.cancel = true;
        
        // Cancel the job in the queue
        if (this.future != null) {
            if (this.future.cancel(false)) {
                this.future = null; // Mark the build cancelled
            }
        }
    }

    public void GenerateFuture() throws InstantiationException {
        if (!isCancelled()) {
            for (int i = 0; i < 12; i++) { // Retry maximum 60 seconds
                future = (QueueTaskFuture<AbstractBuild>) subJob.scheduleBuild2(subJob.getQuietPeriod(),
                                                                                new UpstreamCause((Run) multiJobBuild),
                                                                                actions);
                if (future != null || isCancelled()) {
                    break;
                } else {
                    listener.getLogger().println(String.format("[MultiJob] Error adding %s to Jenkins queue, retrying in 5 seconds.", subJob.getName()));
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    cancelJob();
                    break;
                }
            }
        }

        if (future == null) {
            throw new InstantiationException();
        }
    }
}