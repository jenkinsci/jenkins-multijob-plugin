package com.tikal.jenkins.plugins.multijob.views;

import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class StatusColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public StatusColumn() {
	}

	
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Status";
		}
	}
}
