package com.tikal.jenkins.plugins.multijob;

import jenkins.model.Jenkins;

import hudson.Extension;
import hudson.model.DependencyGraph;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.util.AlternativeUiTextProvider;

import com.tikal.jenkins.plugins.multijob.views.MultiJobView;

public class MultiJobProject extends Project<MultiJobProject, MultiJobBuild>
		implements TopLevelItem {


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
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, getDescriptor().getDisplayName());
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
        return new MultiJobView("");
	}

	public String getRootUrl() {
		return Jenkins.getInstance().getRootUrl();
	}
}
