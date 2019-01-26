package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class JobColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public JobColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return " MultiJob - Job";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
