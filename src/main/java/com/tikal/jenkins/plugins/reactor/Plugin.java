package com.tikal.jenkins.plugins.reactor;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.listeners.ItemListener;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Plugin extends hudson.Plugin {

	/**
	 * If a job is renamed, update all reactor-jobs with the new name.
	 */
	@Extension
	public static final class RenameListener extends ItemListener {
		@Override
		public void onRenamed(Item renamedItem, String oldName, String newName) {
			Collection<TopLevelItem> items = Hudson.getInstance().getItems();
			for (TopLevelItem item : items) {
				if (item instanceof TikalReactorProject) {
					boolean changed = false;
					List<Builder> builders = null;
					TikalReactorProject project = (TikalReactorProject) item;
					builders = project.getBuilders();
					if (builders != null) {
						for (Builder builder : builders) {
							if (builder instanceof ReactorBuilder) {
								ReactorBuilder reactorBuilder = (ReactorBuilder) builder;
								changed |= reactorBuilder.onJobRenamed(oldName,
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
			Collection<TopLevelItem> items = Hudson.getInstance().getItems();
			for (TopLevelItem item : items) {
				if (item instanceof TikalReactorProject) {
					boolean changed = false;
					List<Builder> builders = null;
					TikalReactorProject project = (TikalReactorProject) item;
					builders = project.getBuilders();
					if (builders != null) {
						for (Builder builder : builders) {
							if (builder instanceof ReactorBuilder) {
								ReactorBuilder reactorBuilder = (ReactorBuilder) builder;
								changed |= reactorBuilder.onJobRenamed(oldName,
										null);
								if (changed)
									try {
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
