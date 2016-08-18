package com.tikal.jenkins.plugins.multijob.views;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.views.BuildButtonColumn;
import hudson.views.ListViewColumn;
import org.jenkins.plugins.builton.BuiltOnColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class MultiJobListViewColumn extends ListViewColumn {
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

    @SuppressWarnings("unchecked")
    private static final List<Class<? extends ListViewColumn>> DEFAULT_COLUMNS = Arrays.asList(StatusColumn.class, WeatherColumn.class, JobColumn.class,
            LastSuccessColumn.class, LastFailureColumn.class, LastDurationColumn.class, ConsoleColumn.class, BuildButtonColumn.class, BuiltOnColumn.class);

    private static final Logger LOGGER = Logger.getLogger(MultiJobListViewColumn.class.getName());
}
