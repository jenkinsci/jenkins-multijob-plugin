package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TaskListener;

import java.io.IOException;

public abstract class AbstractBuildParameters extends AbstractDescribableImpl<AbstractBuildParameters> {

    public abstract Action getAction(AbstractBuild<?,?> build, TaskListener listener, AbstractProject project)
            throws IOException, InterruptedException;

    public static class DontTriggerException extends Exception {}
}
