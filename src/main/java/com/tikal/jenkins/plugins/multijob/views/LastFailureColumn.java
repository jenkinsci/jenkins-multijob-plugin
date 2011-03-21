package com.tikal.jenkins.plugins.multijob.views;

import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastFailureColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastFailureColumn() {
	}

	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Last Failure";
		}
	}
}
