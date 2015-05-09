package com.tikal.jenkins.plugins.multijob.counters;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import hudson.model.Result;




public final class CounterManager {
    public static final String PHASEJOB_RESULT = "PHASEJOB_RESULT";

    private final Map<CounterKey, AtomicInteger> counters;
    private Result phaseJobResult = Result.SUCCESS;

    public CounterManager() {
        final Map<CounterKey, AtomicInteger> counters = new HashMap<CounterKey, AtomicInteger>(CounterKey.values().length);
        for(final CounterKey key: CounterKey.values()) {
            counters.put(key, new AtomicInteger(0));
        }

        this.counters = counters;
    }


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

    public void processSkipped() {
        this.counters.get(CounterKey.SKIPPED).incrementAndGet();
    }

    public void processAborted() {
        this.counters.get(CounterKey.ABORTED).incrementAndGet();
    }

    /**
     * Map keys are name of CounterKey enums.
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
