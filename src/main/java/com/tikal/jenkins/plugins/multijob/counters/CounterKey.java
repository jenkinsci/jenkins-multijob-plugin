package com.tikal.jenkins.plugins.multijob.counters;

import hudson.model.Result;

public enum CounterKey {
    /**
     * The name of the new build variable which stores the number of successful jobs.
     * In this context, the SUCCESSFUL state means SUCCESS AND UNSTABLE results.
     * @since 1.0.0
     */
    SUCCESSFUL {
        @Override
        public boolean appliesTo(Result result) {
            return result.isBetterOrEqualTo(Result.UNSTABLE);
        }
    },
    /**
     * The name of the new build variable which stores the number of failed jobs.
     * In this context, the FAILED state means FAILED AND NOT_BUILT results.
     * @since 1.0.0
     */
    FAILED {
        @Override
        public boolean appliesTo(Result result) {
            return Result.FAILURE.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of successful jobs.
     * In this context, the STABLE state means SUCESS results.
     * @since 1.0.0
     */
    STABLE {
        @Override
        public boolean appliesTo(Result result) {
            return Result.SUCCESS.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of unstable jobs.
     * In this context, the UNSTABLE state means the UNSTABLE result.
     * @since 1.0.0
     */
    UNSTABLE {
        @Override
        public boolean appliesTo(Result result) {
            return Result.UNSTABLE.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of aborted jobs.
     * In this context, the ABORTED state means the ABORTED result.
     * @since 1.0.0
     */
    ABORTED {
        @Override
        public boolean appliesTo(Result result) {
            return Result.ABORTED.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of skipped jobs.
     * In this context, the SKIPPED state means the SKIPPED and DISABLED jobs.
     * @since 1.0.0
     */
    SKIPPED {
        @Override
        public boolean appliesTo(Result result) {
            return false;
        }
    };

    public static final String[] KEYS;

    static {
        final String[] keys = new String[CounterKey.values().length * 2];
        int index = 0;
        for (CounterKey key: CounterKey.values()) {
            keys[index++] = key.getMultiJobKey();
            keys[index++] = key.getPhaseJobKey();
        }
        KEYS = keys;
    }

    public abstract boolean appliesTo(Result result);

    private final String multiJobKey = "MULTIJOB_" + this.name() + "_COUNTER";
    private final String phaseJobKey = "PHASEJOB_" + this.name() + "_COUNTER";

    public String getMultiJobKey() {
        return this.multiJobKey;
    }

    public String getPhaseJobKey() {
        return this.phaseJobKey;
    }

    public static String[] getKeys() {
        final String[] keys = new String[CounterKey.values().length * 2];
        int index = 0;
        for (CounterKey key: CounterKey.values()) {
            keys[index++] = key.getMultiJobKey();
            keys[index++] = key.getPhaseJobKey();
        }
        return keys;
    }

    public static CounterKey safetyValueOf(String key) {
        try {
            return CounterKey.valueOf(key);
        } catch (IllegalArgumentException cause) {
            return null;
        }
    }
};