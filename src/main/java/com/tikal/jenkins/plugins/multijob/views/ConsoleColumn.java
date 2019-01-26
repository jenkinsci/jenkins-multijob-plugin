package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class ConsoleColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public ConsoleColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "MultiJob - Console";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
