package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastBuildStatusColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastBuildStatusColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Build Status";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
