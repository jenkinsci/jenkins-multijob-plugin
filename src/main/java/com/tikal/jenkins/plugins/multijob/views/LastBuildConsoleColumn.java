package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastBuildConsoleColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastBuildConsoleColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Build Console";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
