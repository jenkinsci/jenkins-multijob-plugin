//package com.tikal.jenkins.plugins.reactor;
//
//import hudson.Extension;
//import hudson.Indenter;
//import hudson.model.DependencyGraph;
//import hudson.model.ItemGroup;
//import hudson.model.TopLevelItem;
//import hudson.model.Hudson;
//import hudson.model.Job;
//import hudson.model.Project;
//
//public class TikalReactorProject extends Project<TikalReactorProject, TikalReactorBuild> implements TopLevelItem {
//
//	private TikalReactorProject(ItemGroup parent, String name) {
//		super(parent, name);
//	}
//
//	public TikalReactorProject(Hudson parent, String name) {
//		super(parent, name);
//	}
//
//	@Override
//	protected Class<TikalReactorBuild> getBuildClass() {
//		return TikalReactorBuild.class;
//	}
//
//	@Override
//	public Hudson getParent() {
//		return Hudson.getInstance();
//	}
//
//	public DescriptorImpl getDescriptor() {
//		return DESCRIPTOR;
//	}
//
//	@Extension(ordinal = 1000)
//	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
//
//	public static final class DescriptorImpl extends AbstractProjectDescriptor {
//		public String getDisplayName() {
//			return "Reactor Project";
//		}
//
//		public TikalReactorProject newInstance(String name) {
//			return new TikalReactorProject(Hudson.getInstance(), name);
//		}
//	}
//
//	public boolean isTopmostReactor() {
//		return getUpstreamProjects().size() == 0;
//	}
//}
