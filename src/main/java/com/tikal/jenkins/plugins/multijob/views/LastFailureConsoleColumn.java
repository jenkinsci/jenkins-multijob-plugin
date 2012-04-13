package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastFailureConsoleColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastFailureConsoleColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Failure Console";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
