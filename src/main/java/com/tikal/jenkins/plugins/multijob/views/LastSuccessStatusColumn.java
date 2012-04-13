package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastSuccessStatusColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastSuccessStatusColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Success Status";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
