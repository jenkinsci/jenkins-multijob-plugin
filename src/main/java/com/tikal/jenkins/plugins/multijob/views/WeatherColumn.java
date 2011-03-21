package com.tikal.jenkins.plugins.multijob.views;

import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class WeatherColumn extends MultiJobListViewColumn {
	@DataBoundConstructor
	public WeatherColumn() {
	}

	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Weather";
		}
	}
}
