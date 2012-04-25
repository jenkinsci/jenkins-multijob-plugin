package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastBuildColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastBuildColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Build";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
