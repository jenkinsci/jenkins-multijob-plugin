package com.tikal.jenkins.plugins.multijob.counters;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import hudson.model.Result;


/**
 * A manager for the different counters that can exist in a phase.
 * <p>A manager process the results of the jobs, the result of the phase
 * and transforms the counters and their values in the data structure
 * needed by the Multijob.injectEnvVars.</p>
 * <p>These are the variables that will be injected:</p>
 * <p><b>The variables related to the last phase execution:</b></p>
 * <ul>
 *      <li><b>PHASEJOB_RESULT</b>: with the result (as string) of the previous phase.</li>
 *      <li><b>PHASEJOB_SUCCESSFUL</b>: the number of jobs with UNSTABLE or SUCCESS results.</li>
 *      <li><b>PHASEJOB_STABLE</b>: the number of jobs with SUCCESS results.</li>
 *      <li><b>PHASEJOB_UNSTABLE</b>: the number of jobs with UNSTABLE results.</li>
 *      <li><b>PHASEJOB_FAILED</b>: the number of jobs with FAILED results.</li>
 *      <li><b>PHASEJOB_ABORTED</b>: the number of jobs with ABORTED results.</li>
 *      <li><b>PHASEJOB_SKIPPED</b>: the number of jobs with has been skipped.</li>
 * </ul>
 * <p><b>The variables related to current multijob execution:</b></p>
 * <ul>
 *      <li><b>MULTIJOB_RESULT</b>: with the result (as string) of the previous phase.</li>
 *      <li><b>MULTIJOB_SUCCESSFUL</b>: the number of jobs with UNSTABLE or SUCCESS results.</li>
 *      <li><b>MULTIJOB_STABLE</b>: the number of jobs with SUCCESS results.</li>
 *      <li><b>MULTIJOB_UNSTABLE</b>: the number of jobs with UNSTABLE results.</li>
 *      <li><b>MULTIJOB_FAILED</b>: the number of jobs with FAILED results.</li>
 *      <li><b>MULTIJOB_ABORTED</b>: the number of jobs with ABORTED results.</li>
 *      <li><b>MULTIJOB_SKIPPED</b>: the number of jobs with has been skipped.</li>
 * </ul>
 */
public final class CounterManager {
    /**
     * Name of the key that will be used to store, as an environment property, the result of the phase.
     * <p>We compute the result of the phase as the worse of the results of all triggered jobs.</p>
     */
    public static final String PHASEJOB_RESULT = "PHASEJOB_RESULT";

    /**
     * We store all values of the counters as a Map.
     */
    private final Map<CounterKey, AtomicInteger> counters;

    /**
     * The phase result. It is computed as the worse of the results of all triggered jobs.
     */
    private Result phaseJobResult = Result.SUCCESS;

    public CounterManager() {
        final Map<CounterKey, AtomicInteger> counters = new HashMap<CounterKey, AtomicInteger>(CounterKey.values().length);
        for(final CounterKey key: CounterKey.values()) {
            counters.put(key, new AtomicInteger(0));
        }

        this.counters = counters;
    }

    /**
     * Process the result of the job.
     * <p>The processing of the result of the job is an operation that involucres two steps:</p>
     * <ol>
     *      <li>First, we increment all of the counters this result applies to.</li>
     *      <li>Second, we update the <code>phaseJobResult</code> variable if the result if worse than current.</li>
     * </ol>
     *
     * @param result the result of the job to be processed.
     */
    public void process(Result result) {
        for(final CounterKey key: CounterKey.values()) {
            if (key.appliesTo(result)) {
                this.counters.get(key).incrementAndGet();
            }
        }
        if (result.isWorseThan(phaseJobResult)) {
            phaseJobResult = result;
        }
    }

    /**
     * A convenient way to process a skipped job.
     */
    public void processSkipped() {
        this.counters.get(CounterKey.SKIPPED).incrementAndGet();
    }

    /**
     * A convenient way to process an aborted job.
     */
    public void processAborted() {
        this.counters.get(CounterKey.ABORTED).incrementAndGet();
    }

    /**
     * Returns a map with the variables and their values, to be injected.
     * @return a map with the variables related to phase and multijob.
     */
    public Map<String, String> toMap() {
        final Map<String, String> map = new HashMap<String, String>(this.counters.size());
        map.put(PHASEJOB_RESULT, phaseJobResult.toString());

        for(CounterKey key: CounterKey.values()) {
            map.put(key.name(), String.valueOf(this.counters.get(key)));
        }
        return map;
    }

    @Override
    public String toString() {
        return counters.toString();
    }
}
