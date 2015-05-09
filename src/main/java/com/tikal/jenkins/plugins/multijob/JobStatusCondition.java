package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractProject;

public enum JobStatusCondition {
    CHANGED_SINCE_LAST_BUILD("[%s] subjob has changes since last build.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    BUILD_ALWAYS_IS_ENABLED("[%s] the 'build always' feature is enabled.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    NOT_CHANGED_SINCE_LAST_BUILD("[%s] subjob has no changes since last build. ") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    NO_BUILD_ONLY_IF_SCM_CHANGES("[%s] the 'build only if scm changes' feature is disabled.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    UNKNOWN_STATUS("[%s] subjob has unknown status.") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    IS_DISABLED("Skipping %s. This subjob has been disabled.") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    IS_DISABLED_AT_PHASECONFIG("Warning: %s subjob is disabled.") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    DOESNT_CONTAINS_LASTBUILD("[%s] subjob does not contain lastbuild.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    LASTBUILD_RESULT_IS_WORSE_THAN_UNSTABLE("[%s] subjob last build result is worse than unstable.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    WORKSPACE_IS_EMPTY("[%s] subjob workspace is empty.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    };

    private final String message;

    /**
     *
     * @return <code>true</code> the job will be builded, <code>false</code> the job will not be builded.
     */
    public abstract boolean isBuildable();

    private JobStatusCondition(String message) {
        this.message = "    >> Job status: " + message;
    }

    public String getMessage() {
        return this.message;
    }

    public String getMessage(AbstractProject subjob) {
        return String.format(this.getMessage(), subjob.getName());
    }
}