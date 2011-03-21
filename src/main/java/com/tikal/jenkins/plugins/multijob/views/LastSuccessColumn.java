package com.tikal.jenkins.plugins.multijob.views;

import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class LastSuccessColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public LastSuccessColumn() {
	}

	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Last Success";
		}
	}
}
