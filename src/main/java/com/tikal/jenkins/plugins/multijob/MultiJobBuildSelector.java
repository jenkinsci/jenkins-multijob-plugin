package com.tikal.jenkins.plugins.multijob;

import java.util.Optional;
import java.util.logging.Logger;

import hudson.matrix.MatrixRun;
import hudson.plugins.copyartifact.BuildFilter;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.Messages;
import hudson.plugins.copyartifact.SimpleBuildSelectorDescriptor;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;

/**
 * Copy artifacts from the build that was part of this MultiJob build.
 * @author Ray Sennewald
 */
public class MultiJobBuildSelector extends BuildSelector {
    private static final Logger LOGGER = Logger.getLogger(MultiJobBuildSelector.class.getName());

    @DataBoundConstructor
    public MultiJobBuildSelector() { }

    @Override
    public Run<?, ?> getBuild(Job<?, ?> job, EnvVars env, BuildFilter filter, Run<?, ?> parent) {
        MultiJobBuild multiJobBuild = null;
        // Are we in the MultiJob itself and trying to get an artifact from a Phase Build?
        if (parent instanceof MultiJobBuild) {
            multiJobBuild = (MultiJobBuild)parent;
        }
        // Nope, look for Upstream MultiJob that triggered this run (Are we in a Phase job?)
        else {
            // Matrix run is triggered by its parent project, so check causes of parent build:
            for (Cause cause : parent instanceof MatrixRun
                    ? ((MatrixRun)parent).getParentBuild().getCauses() : parent.getCauses()) {
                if (cause instanceof UpstreamCause) {
                    // We need only builds with upstream cause
                } else {
                    LOGGER.warning(String.format("'%s' Has no upstream build.", parent.getFullDisplayName()));
                    break;
                }
                UpstreamCause upstreamCause = (UpstreamCause)cause;
                Job upstreamJob = Jenkins.getInstance().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
                Run upstreamRun = Optional.ofNullable(upstreamJob).map(j -> j.getBuildByNumber(upstreamCause.getUpstreamBuild())).orElse(null);

                if (upstreamRun != null && upstreamRun instanceof MultiJobBuild) {
                    multiJobBuild = (MultiJobBuild)upstreamRun;
                }
                // We need only first cause, don't care about the previous
                break;
            }
        }
        if (multiJobBuild == null) {
            LOGGER.warning(String.format("'%s' is not found to be part of a MultiJob Project.", parent.getFullDisplayName()));
            return null;
        }
        // Get the run for our source Job in the current MultiJob Project's Build
        for (MultiJobBuild.SubBuild subBuild : multiJobBuild.getSubBuilds()) {
            // Find Job's specific build we want
            if (subBuild.getJobName().equals(job.getDisplayName())) {
                Run run = job.getBuildByNumber(subBuild.getBuildNumber());
                if (filter.isSelectable(run, env)) {
                    return run;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isSelectable(Run<?,?> run, EnvVars env) {
        return true;
    }

    @Extension(optional = true, ordinal=20)
    public static final Descriptor<BuildSelector> DESCRIPTOR =
            new SimpleBuildSelectorDescriptor(
                    MultiJobBuildSelector.class,
                    com.tikal.jenkins.plugins.multijob.Messages._MultiJobBuildSelector_DisplayName());
}
