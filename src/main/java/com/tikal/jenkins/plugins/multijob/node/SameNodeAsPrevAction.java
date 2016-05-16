package com.tikal.jenkins.plugins.multijob.node;

import hudson.model.Action;

/**
 * This action doesn't do anything: it's just a marker for adding another action/parameter to define
 * where the build should be executed (e.g {@link hudson.plugins.parameterizedtrigger.NodeAction})
 */
public final class SameNodeAsPrevAction implements Action {
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }
}
