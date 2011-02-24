package com.tikal.jenkins.plugins.reactor.views;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.views.BuildButtonColumn;
import hudson.views.ListViewColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class ReactorListViewColumn extends ListViewColumn {
	public static List<ListViewColumn> createDefaultInitialColumnList() {
		// OK, set up default list of columns:
		// create all instances
		ArrayList<ListViewColumn> r = new ArrayList<ListViewColumn>();
		DescriptorExtensionList<ListViewColumn, Descriptor<ListViewColumn>> all = ListViewColumn.all();

		for (Class<? extends ListViewColumn> d : DEFAULT_COLUMNS) {
			Descriptor<ListViewColumn> des = all.find(d);
			if (des != null) {
				try {
					r.add(des.newInstance(null, null));
				} catch (FormException e) {
					LOGGER.log(Level.WARNING, "Failed to instantiate " + des.clazz, e);
				}
			}
		}
		return r;
	}

	@Override
	public boolean shownByDefault() {
		return false;
	}

	private static final List<Class<? extends ListViewColumn>> DEFAULT_COLUMNS = Arrays.asList(JobColumn.class, StatusColumn.class, WeatherColumn.class,
			LastSuccessColumn.class, BuildButtonColumn.class);

	private static final Logger LOGGER = Logger.getLogger(ReactorListViewColumn.class.getName());
}
