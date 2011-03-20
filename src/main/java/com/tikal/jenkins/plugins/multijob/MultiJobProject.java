package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.model.DependencyGraph;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.util.DescribableList;
import hudson.views.ListViewColumn;

import java.util.List;

import com.tikal.jenkins.plugins.multijob.views.MultiJobListViewColumn;
import com.tikal.jenkins.plugins.multijob.views.MultiJobView;

public class MultiJobProject extends Project<MultiJobProject, MultiJobBuild> implements TopLevelItem {

	@SuppressWarnings("rawtypes")
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

		@SuppressWarnings("rawtypes")
		public MultiJobProject newInstance(ItemGroup itemGroup, String name) {
			return new MultiJobProject(itemGroup, name);
		}
	}

	@Override
	protected void buildDependencyGraph(DependencyGraph graph) {
		super.buildDependencyGraph(graph);
	}

	public boolean isTopMost() {
		return getUpstreamProjects().size() == 0;
	}

	public MultiJobView getView() {
		MultiJobView view = new MultiJobView("");
		return view;
	}

	public String getRootUrl() {
		return Hudson.getInstance().getRootUrl();
	}
}
