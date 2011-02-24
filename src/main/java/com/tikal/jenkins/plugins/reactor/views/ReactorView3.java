package com.tikal.jenkins.plugins.reactor.views;
//package com.tikal.jenkins.plugins.reactor.views;
//
//import hudson.Extension;
//import hudson.Indenter;
//import hudson.Util;
//import hudson.model.TopLevelItem;
//import hudson.model.ViewDescriptor;
//import hudson.model.ViewGroup;
//import hudson.model.AbstractProject;
//import hudson.model.Hudson;
//import hudson.model.ListView;
//import hudson.model.Project;
//import hudson.tasks.Builder;
//import hudson.util.FormValidation;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.SortedSet;
//import java.util.TreeMap;
//import java.util.TreeSet;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;
//
//import javax.servlet.ServletException;
//
//import org.kohsuke.stapler.DataBoundConstructor;
//import org.kohsuke.stapler.QueryParameter;
//
//import com.tikal.jenkins.plugins.reactor.ReactorBuilder;
//import com.tikal.jenkins.plugins.reactor.TikalReactorProject;
//
//public class ReactorView extends ListView {
//
//	@DataBoundConstructor
//	public ReactorView(String name) {
//		super(name);
//	}
//
//	public ReactorView(String name, ViewGroup owner) {
//		super(name, owner);
//	}
//
//	@Extension
//	public static final class DescriptorImpl extends ViewDescriptor {
//		public String getDisplayName() {
//			return "Reactor View";
//		}
//
//		/**
//		 * Checks if the include regular expression is valid.
//		 */
//		public FormValidation doCheckIncludeRegex(@QueryParameter String value) throws IOException, ServletException, InterruptedException {
//			String v = Util.fixEmpty(value);
//			if (v != null) {
//				try {
//					Pattern.compile(v);
//				} catch (PatternSyntaxException pse) {
//					return FormValidation.error(pse.getMessage());
//				}
//			}
//			return FormValidation.ok();
//		}
//	}
//
//	public synchronized List<TopLevelItem> getItems() {
//		List<TopLevelItem> items = super.getItems();
//
//		List<TopLevelItem> out = new ArrayList<TopLevelItem>();
//		for (TopLevelItem item : items) {
//			if (item instanceof TikalReactorProject) {
//				TikalReactorProject project = (TikalReactorProject) item;
//				if (project.getUpstreamProjects().size() == 0) {
//					addSubprojects(project, out, 0);
//				}
//			}
//		}
//
//		// TreeMap<String, TopLevelItem> tree = new TreeMap<String,
//		// TopLevelItem>(new JobNameComparator());
//		//
//		// for (TopLevelItem item : items) {
//		// tree.put(createFullName(item), item);
//		// }
//		// Collections.sort(items, new JobNameComparator());
//		List<TopLevelItem> items2 = new ArrayList<TopLevelItem>(out);
//		return items2;
//	}
//
//	private void addSubprojects(TopLevelItem item, List<TopLevelItem> out, int nestLevel) {
//		out.add(item);
//		//out.add((TopLevelItem)new NestableItem((AbstractProject)item, nestLevel));
//		if (item instanceof TikalReactorProject) {
//			TikalReactorProject project = (TikalReactorProject) item;
//			List<Builder> builders = project.getBuilders();
//			for (Builder builder : builders) {
//				if (builder instanceof ReactorBuilder) {
//					ReactorBuilder reactorBuilder = (ReactorBuilder) builder;
//					String[] jobNames = reactorBuilder.getJobNames().split(" ");
//					for (String jobName : jobNames) {
//						addSubprojects(Hudson.getInstance().getItem(jobName), out, nestLevel + 1);
//					}
//				}
//			}
//		}
//	}
//
//	private String createFullName(TopLevelItem item) {
//		String fullName = createFullName2(item);
//		System.out.println(item.getName() + " " + fullName);
//		return fullName;
//	}
//
//	private String createFullName2(TopLevelItem item) {
//		StringBuilder nameBuilder = new StringBuilder();
//		nameBuilder.append(item.getName());
//		if (item instanceof TikalReactorProject) {
//			TikalReactorProject project = (TikalReactorProject) item;
//			List<Builder> builders = project.getBuilders();
//			for (Builder builder : builders) {
//				if (builder instanceof ReactorBuilder) {
//					ReactorBuilder reactorBuilder = (ReactorBuilder) builder;
//					String[] jobNames = reactorBuilder.getJobNames().split(" ");
//					for (String jobName : jobNames) {
//						nameBuilder.append(":");
//						nameBuilder.append(createFullName2(Hudson.getInstance().getItem(jobName)));
//
//					}
//				}
//			}
//		}
//		// if (item instanceof AbstractProject) {
//		// AbstractProject project = (AbstractProject) item;
//		// nameBuilder.append(project.getName());
//		// List<Project> upstreamProjects = project.getUpstreamProjects();
//		// while (upstreamProjects.size() > 0) {
//		// Project upstreamProject = upstreamProjects.get(0);
//		// nameBuilder.insert(0, upstreamProject.getName() + ":");
//		// upstreamProjects = upstreamProject.getUpstreamProjects();
//		// }
//		// }
//		return nameBuilder.toString();
//	}
//
//	public class JobNameComparator implements Comparator<TopLevelItem> {
//
//		public int compare(String name1, String name2) {
//			int depth1 = name1.split(":").length;
//			int depth2 = name2.split(":").length;
//			return depth1 < depth2 ? 1 : depth1 > depth2 ? -1 : 0;
//		}
//
//		// public int compare(TopLevelItem item1, TopLevelItem item2) {
//		// int depth1 = createFullName(item1).split(":").length;
//		// int depth2 = createFullName(item2).split(":").length;
//		// return depth1 < depth2 ? -1 : depth1 > depth2 ? 1 : 0;
//		// }
//		public int compare(TopLevelItem item1, TopLevelItem item2) {
//			String fullName1 = createFullName(item1);
//			String fullName2 = createFullName(item2);
//			return fullName1.compareTo(fullName2);
//		}
//	}
//}
