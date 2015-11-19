package com.tikal.jenkins.plugins.multijob.listeners;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;

/**
 * Controls which a child job of {@link MultiJobBuild} to rebuild
 *
 * <p>
 * Plugins can implement this extension point to filter out a childs of MultiJob project ti build.
 * </p>
 */
public abstract class MultiJobListener implements ExtensionPoint {

	public abstract void onStart(AbstractBuild<?, ?> build, MultiJobBuild multiJobBuild);

	public abstract boolean isComplete(AbstractBuild<?, ?> build, MultiJobBuild multiJobBuild);

	public static void fireOnStart(AbstractBuild<?, ?> build, MultiJobBuild multiJobBuild) {
		for (MultiJobListener l : all()) {
			l.onStart(build, multiJobBuild);
		}
	}

	public static boolean fireOnComplete(AbstractBuild<?, ?> build, MultiJobBuild multiJobBuild) {
		for (MultiJobListener l : all()) {
			if (!l.isComplete(build, multiJobBuild)) {
				return false;
			}
		}
		return true;
	}

	public static ExtensionList<MultiJobListener> all() {
		return Jenkins.getInstance().getExtensionList(MultiJobListener.class);
	}
}
