//package com.tikal.jenkins.plugins.reactor.views;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.SortedSet;
//import java.util.TreeSet;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;
//
//import javax.servlet.ServletException;
//
//import org.apache.commons.collections.CollectionUtils;
//import org.kohsuke.stapler.DataBoundConstructor;
//import org.kohsuke.stapler.QueryParameter;
//
//import hudson.Extension;
//import hudson.Util;
//import hudson.model.AbstractProject;
//import hudson.model.Hudson;
//import hudson.model.Messages;
//import hudson.model.TopLevelItem;
//import hudson.model.ViewDescriptor;
//import hudson.model.ViewGroup;
//import hudson.model.ListView;
//import hudson.util.FormValidation;
//import hudson.views.ViewJobFilter;
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
//    public synchronized List<TopLevelItem> getItems() {
//        SortedSet<String> names = new TreeSet<String>(jobNames);
//
//        if (includePattern != null) {
//            for (TopLevelItem item : Hudson.getInstance().getItems()) {
//                String itemName = item.getName();
//                if (includePattern.matcher(itemName).matches()) {
//                    names.add(itemName);
//                }
//            }
//        }
//
//        List<TopLevelItem> items = new ArrayList<TopLevelItem>(names.size());
//        for (String n : names) {
//            TopLevelItem item = Hudson.getInstance().getItem(n);
//            // Add if no status filter or filter matches enabled/disabled status:
//            if(item!=null && (statusFilter == null || !(item instanceof AbstractProject)
//                              || ((AbstractProject)item).isDisabled() ^ statusFilter))
//                items.add(item);
//        }
//
//        // check the filters
//        Iterable<ViewJobFilter> jobFilters = getJobFilters();
//        List<TopLevelItem> allItems = Hudson.getInstance().getItems();
//    	for (ViewJobFilter jobFilter: jobFilters) {
//    		items = jobFilter.filter(items, allItems, this);
//    	}
//        // for sanity, trim off duplicates
//        items = new ArrayList<TopLevelItem>(new LinkedHashSet<TopLevelItem>(items));
//        
//        return items;
//    }
//}
