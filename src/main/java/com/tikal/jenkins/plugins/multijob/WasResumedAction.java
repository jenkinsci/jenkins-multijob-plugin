package com.tikal.jenkins.plugins.multijob;

import hudson.model.Action;

/**
 * Indicates that the build was resumed. Being marked as inactive on performing the new build.
 * The reason for this class is to handle build resuming correctly via rebuild plugin.
 */
public final class WasResumedAction implements Action {
    private boolean active = true;

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    @Override
    public String toString() {
        return "WasResumedAction{" +
                "active=" + active +
                '}';
    }
}
