package com.tikal.jenkins.plugins.multijob;


import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.BallColor;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Run;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;
import java.util.ArrayList;
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
        if (this.future != null) {
            this.future.cancel(true);
        }
    }

    public void GenerateFuture() {
        this.future = subJob.scheduleBuild2(subJob.getQuietPeriod(),
                                            new UpstreamCause((Run) multiJobBuild),
                                            actions.toArray(new Action[0]));
    }
}