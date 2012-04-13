package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastFailureStatusColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastFailureStatusColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Failure Status";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
