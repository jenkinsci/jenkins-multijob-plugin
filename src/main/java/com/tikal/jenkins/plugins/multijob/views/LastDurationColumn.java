package com.tikal.jenkins.plugins.multijob.views;

import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastDurationColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastDurationColumn() {
	}

	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Last Duration";
		}
	}
}
