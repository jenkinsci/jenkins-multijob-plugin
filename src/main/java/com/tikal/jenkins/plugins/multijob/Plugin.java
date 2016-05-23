package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Plugin extends hudson.Plugin implements Describable<Plugin> {

    public static final Descriptor<Plugin> DESCRIPTOR = new DescriptorImpl();
    private static Plugin instance;
    private long timestamp;
    private Map<String, Boolean> columns = new HashMap<String, Boolean>();
    private final List<String> columnKeys = new ArrayList<String>() {{
        add("job");
        add("status");
        add("weather");
        add("build");
        add("last-success");
        add("last-failure");
        add("last-duration");
        add("console");
        add("run");
    }};

    @Override
    public void start() throws Exception {
        super.start();
        load();
        instance = Jenkins.getInstance().getPlugin(Plugin.class);
        init();
    }

    public Descriptor<Plugin> getDescriptor() {
        return DESCRIPTOR;
    }

    private void init() {
        for (String key : columnKeys) {
            if (!columns.containsKey(key)) {
                columns.put(key, true);
            }
        }
    }

    public void setColumnState(String key, boolean value) throws IOException {
        columns.put(key, value);
        timestamp = System.nanoTime();
        save();
    }

    public boolean isShowColumn(String key) {
        Boolean res = columns.get(key);
        if (null == res) {
            res = true;
        }
        return res;
    }

    public Map<String, Boolean> getColumnMap() {
        return columns;
    }

    public boolean isUserPropertyTooOld(long userTimestamp) {
        return userTimestamp < timestamp;
    }

    public static Plugin getInstance() {
        return instance;
    }

    /**
     * If a job is renamed, update all multiJob-jobs with the new name.
     */
    @Extension
    public static final class RenameListener extends ItemListener {
        @Override
        public void onRenamed(Item renamedItem, String oldName, String newName) {
            Collection<TopLevelItem> items = Jenkins.getInstance().getItems();
            for (TopLevelItem item : items) {
                if (item instanceof MultiJobProject) {
                    boolean changed = false;
                    List<Builder> builders = null;
                    MultiJobProject project = (MultiJobProject) item;
                    builders = project.getBuilders();
                    if (builders != null) {
                        for (Builder builder : builders) {
                            if (builder instanceof MultiJobBuilder) {
                                MultiJobBuilder multiJobBuilder = (MultiJobBuilder) builder;
                                changed |= multiJobBuilder.onJobRenamed(oldName,
                                        newName);
                                if (changed)
                                    try {
                                        project.save();
                                    } catch (IOException e) {
                                        Logger.getLogger(
                                                RenameListener.class.getName())
                                                .log(Level.WARNING,
                                                        "Failed to persist project setting during rename from "
                                                                + oldName
                                                                + " to "
                                                                + newName, e);
                                    }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onDeleted(Item deletedItem) {
            String oldName = deletedItem.getName();
            Collection<TopLevelItem> items = Jenkins.getInstance().getItems();
            for (TopLevelItem item : items) {
                if (item instanceof MultiJobProject) {
                    boolean changed = false;
                    List<Builder> builders = null;
                    MultiJobProject project = (MultiJobProject) item;
                    builders = project.getBuilders();
                    if (builders != null) {
                        for (Builder builder : builders) {
                            if (builder instanceof MultiJobBuilder) {
                                MultiJobBuilder multiJobBuilder = (MultiJobBuilder) builder;
                                changed |= multiJobBuilder.onJobDeleted(oldName);
                                if (changed)
                                    try {
                                        int phaseJobsCounter = multiJobBuilder.getPhaseJobs().size();
                                        if(phaseJobsCounter==0){
                                            project.getBuildersList().remove(multiJobBuilder);
                                        }
                                        project.save();
                                    } catch (IOException e) {
                                        Logger.getLogger(
                                                RenameListener.class.getName())
                                                .log(Level.WARNING,
                                                        "Failed to persist project setting during remove of "
                                                                + oldName, e);
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Plugin> {

        @Override
        public String getDisplayName() {
            return "";
        }


    }
}
