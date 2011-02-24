package com.tikal.jenkins.plugins.reactor.views;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class WeatherColumn extends ReactorListViewColumn {
	@DataBoundConstructor
	public WeatherColumn() {
	}

	@Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return "Weather";
		}
	}
}
