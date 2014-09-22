package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.listeners.ItemListener;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Plugin extends hudson.Plugin {

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
}
