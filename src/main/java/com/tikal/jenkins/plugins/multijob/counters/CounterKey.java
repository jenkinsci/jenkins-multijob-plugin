package com.tikal.jenkins.plugins.multijob.counters;

import hudson.model.Result;

/**
 * These are the keys of the counters that we will manage to report at phase level and multijob project level.
 *
 * <p>Every time a phase ends:</p>
 * <ol>
 *      <li>We inject phase variables with the count of the jobs, one for every key.</li>
 *      <li>We update multijob variables with the number (counter) of the jobs, one for every key.</li>
 * </ol>
 *
 * <p>We can use these new variables at job condition, so we can write now conditions as follows
 * (of course, these new variables are available in the next phase):</p>
 *
 * <ul>
 *      <li><b>${PHASE_SUCCESSFUL} &gt; 1</b>: The number of <b>SUCCESSFUL</b> jobs are greater than 1.</li>
 *      <li><b>${PHASE_ABORTED} == 0 || ${PHASE_UNSTABLE} == 2</b>:
 *      The number of <b>ABORTED</b> jobs are equals to 0,
 *      or the number of <b>UNSTABLE</b> jobs are equals to 2.</li>
 * </ul>
 *
 */
public enum CounterKey {
    /**
     * The name of the new build variable which stores the number of successful jobs.
     * In this context, the SUCCESSFUL state means SUCCESS OR UNSTABLE results.
     */
    SUCCESSFUL {
        @Override
        public boolean appliesTo(Result result) {
            return result.isBetterOrEqualTo(Result.UNSTABLE);
        }
    },
    /**
     * The name of the new build variable which stores the number of successful jobs.
     * In this context, the STABLE state means SUCESS results.
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
     */
    UNSTABLE {
        @Override
        public boolean appliesTo(Result result) {
            return Result.UNSTABLE.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of failed jobs.
     * In this context, the FAILED state means FAILED results.
     */
    FAILED {
        @Override
        public boolean appliesTo(Result result) {
            return Result.FAILURE.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of aborted jobs.
     * In this context, the ABORTED state means the ABORTED result.
     */
    ABORTED {
        @Override
        public boolean appliesTo(Result result) {
            return Result.ABORTED.equals(result);
        }
    },
    /**
     * The name of the new build variable which stores the number of skipped jobs.
     * In this context, the SKIPPED state means the SKIPPED jobs.
     */
    SKIPPED {
        @Override
        public boolean appliesTo(Result result) {
            return false;
        }
    };

    /**
     * A convenient static array of all multijob and phase keys.
     */
    public static final String[] KEYS;

    static {
        final String[] keys = new String[CounterKey.values().length * 2];
        int index = 0;
        for (CounterKey key: CounterKey.values()) {
            keys[index++] = key.getMultiJobKey();
            keys[index++] = key.getPhaseKey();
        }
        KEYS = keys;
    }

    /**
     * Checks if a result applies to the CounterKey.
     * Every time a job has finished we must update the counter or counters this result applies to.
     * This methods reports if the result applies to the CounterKey.
     *
     * @param result the {@link Result} of the job that we checked if applies to the CounterKey.
     * @return <code>true</code> when the result applies to the counterKey,
     *      <code>false</code> if not applies.
     * @see Result
     */
    public abstract boolean appliesTo(Result result);

    /**
     * The name of the key that it is associated with the multijob project and this key.
     */
    private final String multiJobKey = "MULTIJOB_" + this.name();

    /**
     * The name of the key that it is associated with the phase and this key.
     */
    private final String phaseKey = "PHASE_" + this.name();

    /**
     * Returns the name of the key associated with the multijob project and the counterKey.
     * @return a String with the name of key that will be used to store the counter.
     */
    public String getMultiJobKey() {
        return this.multiJobKey;
    }

    /**
     * Returns the name of the key associated with the phase and the counterKey.
     * @return a String with the name of key that will be used to store the counter.
     */
    public String getPhaseKey() {
        return this.phaseKey;
    }


    /**
     * A convenient way to encapsulate logic to avoid an IllegalArgumentException when
     * we need to find a CounterKey value and the key doesn't exists.
     *
     * @param key the name of the CounterKey that we want to search.
     * @return the <code>CounterKey</code> or <code>null</code> if it doesn't exists.
     */
    public static CounterKey safetyValueOf(String key) {
        try {
            return CounterKey.valueOf(key);
        } catch (IllegalArgumentException cause) {
            return null;
        }
    }
};