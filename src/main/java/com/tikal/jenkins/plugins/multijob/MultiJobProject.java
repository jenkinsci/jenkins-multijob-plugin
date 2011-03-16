package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.Indenter;
import hudson.model.DependencyGraph;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Project;

public class MultiJobProject extends Project<MultiJobProject, MultiJobBuild> implements TopLevelItem {

	private MultiJobProject(ItemGroup parent, String name) {
		super(parent, name);
	}

	public MultiJobProject(Hudson parent, String name) {
		super(parent, name);
	}

	@Override
	protected Class<MultiJobBuild> getBuildClass() {
		return MultiJobBuild.class;
	}

	@Override
	public Hudson getParent() {
		return Hudson.getInstance();
	}

	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension(ordinal = 1000)
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends AbstractProjectDescriptor {
		public String getDisplayName() {
			return "MultiJob Project";
		}

		public MultiJobProject newInstance(String name) {
			return new MultiJobProject(Hudson.getInstance(), name);
		}
	}

	@Override
	protected void buildDependencyGraph(DependencyGraph graph) {
		// TODO Auto-generated method stub
		super.buildDependencyGraph(graph);
	}

	public boolean isTopMost() {
		return getUpstreamProjects().size() == 0;
	}
}
