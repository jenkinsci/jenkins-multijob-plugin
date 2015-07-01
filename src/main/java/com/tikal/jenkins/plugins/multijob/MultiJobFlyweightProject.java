package com.tikal.jenkins.plugins.multijob;

import com.tikal.jenkins.plugins.multijob.views.MultiJobView;
import hudson.Extension;
import hudson.model.DependencyGraph;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.TopLevelItem;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.AlternativeUiTextProvider;
import jenkins.model.Jenkins;

public class MultiJobFlyweightProject extends Project<MultiJobFlyweightProject, MultiJobFlyweightBuild>
	implements TopLevelItem, Queue.FlyweightTask {

	@SuppressWarnings("rawtypes")
	private MultiJobFlyweightProject(ItemGroup parent, String name) {
		super(parent, name);
	}

	public MultiJobFlyweightProject(Hudson parent, String name) {
		super(parent, name);
	}

	@Override
	protected Class<MultiJobFlyweightBuild> getBuildClass() {
		return MultiJobFlyweightBuild.class;
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

		@Override
		public String getDisplayName() {
			return "MultiJob Flyweight Project";
		}

		@Override
		public MultiJobFlyweightProject newInstance(ItemGroup group, String s) {
			return new MultiJobFlyweightProject(group, s);
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

	public AbstractTestResultAction<?> getTestResultAction() {
		MultiJobFlyweightBuild b = getLastCompletedBuild();
		return b != null ? b.getAction(AbstractTestResultAction.class) : null;
	}
}
