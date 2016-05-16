package com.tikal.jenkins.plugins.multijob.node;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * This parameter allows sub-builds of multi-job build (started after resume/rebuild) to be executed on the same node
 * that these parameters in the original build (that was resumed/rebuilded).
 */
@SuppressWarnings("unused") // used by Jenkins
public final class SameNodeAsPrevParameters extends AbstractBuildParameters {

    @DataBoundConstructor
    public SameNodeAsPrevParameters() {
    }

    @Override
    public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, DontTriggerException {
        return new SameNodeAsPrevAction();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
        @Override
        public String getDisplayName() {
            return "Build on the same node as the previous build";
        }
    }
}
