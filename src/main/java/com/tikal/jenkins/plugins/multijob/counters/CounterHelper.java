package com.tikal.jenkins.plugins.multijob.counters;

import java.util.HashMap;
import java.util.Map;

public final class CounterHelper {
    private CounterHelper() {}


    public static Map<String, String> accumulateAndMerge(
        final Map<String, String> incomingVars,
        final Map<String, String> previousEnvVars) {
        
        final Map<String, String> mixtured = new HashMap<String, String>(previousEnvVars);

        // It is necessary iterate through the incomingVars map, because
        // it may contain counters and non counter variables.
        for(Map.Entry<String, String> entry: incomingVars.entrySet()) {
            final String key = entry.getKey();
            final CounterKey counterKey = CounterKey.safetyValueOf(key);
            String incomingValue = entry.getValue();
            if (counterKey == null) {
                mixtured.put(key, incomingValue);
            } else {
                // Updating PHASEJOB_?????_KEY
                mixtured.put(counterKey.getPhaseJobKey(), zeroIfNull(incomingValue));

                // Updating MULTIJOB_?????_KEY
                final String multijobKey = counterKey.getMultiJobKey();
                final String previousValue = zeroIfNull(previousEnvVars.get(multijobKey));
                incomingValue = zeroIfNull(incomingValue);
                mixtured.put(
                    multijobKey, 
                    String.valueOf(Integer.parseInt(incomingValue) + Integer.parseInt(previousValue))
                );
            }
        }

        return mixtured;
    }

    private static String zeroIfNull(String value) {
        if ( value == null || value.trim().length() == 0 ) {
            return "0";
        }
        return value;
    }
}