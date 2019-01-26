package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastSuccessColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public LastSuccessColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "MultiJob - Last Success";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
