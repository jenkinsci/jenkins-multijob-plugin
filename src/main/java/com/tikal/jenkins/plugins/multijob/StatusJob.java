package com.tikal.jenkins.plugins.multijob;

import hudson.model.Job;

/**
 * The different statuses of the job which may or not triggered the build.
 * <p>These are used to compute the {@link MultiJobBuilder#JOB_IS_BUILDABLE} variable.
 * There is only one status available for each job, and the current check sequence is 
 * described at MultiJobBuilder.getScmChange().</p>
 *      
 */
public enum StatusJob {
    /**
     * The job has scm changes since last build.
     */
    CHANGED_SINCE_LAST_BUILD("[%s] subjob has changes since last build.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    /**
     * The 'Build Always' feature is enabled.
     */
    BUILD_ALWAYS_IS_ENABLED("[%s] the 'build always' feature is enabled.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    /**
     * The job has no scm changes since last build.
     */
    NOT_CHANGED_SINCE_LAST_BUILD("[%s] subjob has no changes since last build. ") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    /**
     * The 'Build Only If Scm Changes' feature is disabled.
     */
    BUILD_ONLY_IF_SCM_CHANGES_DISABLED("[%s] the 'build only if scm changes' feature is disabled.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    /**
     * Unknown status. It is impossible to identify the status of the job.
     */
    UNKNOWN_STATUS("[%s] subjob has unknown status.") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    /**
     * The job is disabled.
     */
    IS_DISABLED("Skipping [%s]. This subjob has been disabled.") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    /**
     * The job in the phase configuration is disabled.
     */
    IS_DISABLED_AT_PHASECONFIG("Warning: [%s] subjob in the phase configuration is disabled.") {
        @Override
        public boolean isBuildable() {
            return false;
        }
    },
    /**
     * The job doesn't contains lastbuild.
     */
    DOESNT_CONTAINS_LASTBUILD("[%s] subjob does not contain lastbuild.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    /**
     * Lastbuild result is worse than unstable.
     */
    LASTBUILD_RESULT_IS_WORSE_THAN_UNSTABLE("[%s] subjob last build result is worse than unstable.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    /**
     * Workspace is empty.
     */
    WORKSPACE_IS_EMPTY("[%s] subjob workspace is empty.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    },
    /**
     * The job doesn't contains workspace.
     */
    DOESNT_CONTAINS_WORKSPACE("[%s] subjob does not contain workspace.") {
        @Override
        public boolean isBuildable() {
            return true;
        }
    };

    /**
     * The message associated to this status job.
     */
    private final String message;

    /**
     * Every status job must report about if the job must be built or not.
     *
     * @return <code>true</code> the job will be built, <code>false</code> the job will not be built.
     */
    public abstract boolean isBuildable();

    private StatusJob(String message) {
        this.message = "    >> Job status: " + message;
    }

    /**
     * Returns the message associated to the status job as is.
     * @return a text with the message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Formats the message associated to the status job.
     * @param subjob the job associated to this status job.
     * @return a text with the formatted message.
     */
    public String getMessage(Job subjob) {
        return String.format(this.getMessage(), subjob.getName());
    }
}