package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class StatusColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public StatusColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "MultiJob  - Status";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
