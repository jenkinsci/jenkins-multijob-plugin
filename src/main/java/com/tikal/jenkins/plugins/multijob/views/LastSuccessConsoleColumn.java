package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastSuccessConsoleColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastSuccessConsoleColumn() {
	}

    @Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "MultiJob - Last Success Console";
		}
		public boolean shownByDefault() {
	        return false;
	    }		
	}
}
