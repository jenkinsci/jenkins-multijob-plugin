package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class WeatherColumn extends MultiJobListViewColumn {
    @DataBoundConstructor
    public WeatherColumn() {
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "MultiJob - Weather";
        }
        public boolean shownByDefault() {
            return false;
        }
    }
}
