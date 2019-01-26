package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastFailureColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public LastFailureColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "MultiJob - Last Failure";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
