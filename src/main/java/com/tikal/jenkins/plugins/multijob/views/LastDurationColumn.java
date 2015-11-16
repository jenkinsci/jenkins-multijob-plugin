package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastDurationColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public LastDurationColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "MultiJob - Last Duration";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
