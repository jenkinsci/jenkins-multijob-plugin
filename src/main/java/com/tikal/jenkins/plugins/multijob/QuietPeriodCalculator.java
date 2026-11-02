package com.tikal.jenkins.plugins.multijob;

import groovy.lang.Binding;
import hudson.model.BuildListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;

public class QuietPeriodCalculator {

    private static final Logger LOG = Logger.getLogger(QuietPeriodCalculator.class.getName());
    private static final String INDEX = "index";
    private final BuildListener listener;
    private final String displayName;

    public QuietPeriodCalculator() {
        this(null, null);
    }

    QuietPeriodCalculator(final BuildListener listener, final String displayNameOrNull) {
        this.listener = listener;
        this.displayName = displayNameOrNull == null ? "" : displayNameOrNull + ": ";
    }

    public int calculate(String quietPeriodGroovy, int index) {

        if (quietPeriodGroovy == null) {
            return 0;
        }

        assertPositiveIndex(index);

        try {
            return calculateOrThrow(quietPeriodGroovy, index);
        } catch (RejectedAccessException e) {
            throw e;
        } catch (Throwable t) {
            final String message = "Error calculating quiet time for index " + index + " and quietPeriodGroovy ["
                    + quietPeriodGroovy + "]: " + t.getMessage() + "; returning 0";
            LOG.log(Level.WARNING, message, t);
            log(message);
            return 0;
        }
    }

    public int calculateOrThrow(final String quietPeriodGroovy, final int index) {

        assertPositiveIndex(index);

        final Object result;
        try {
            Binding binding = new Binding();
            binding.setVariable(INDEX, index);
            result = new SecureGroovyScript(quietPeriodGroovy, true, null)
                    .configuring(ApprovalContext.create())
                    .evaluate(Jenkins.get().getPluginManager().uberClassLoader, binding, listener);
        } catch (RejectedAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final int quietPeriod = ((Number) result).intValue();
        log(displayName + "Quiet period groovy=[" + quietPeriodGroovy + "], index=" + index + " -> quietPeriodGroovy="
                + quietPeriod);
        return quietPeriod;
    }

    private static void assertPositiveIndex(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("positive index expected, got " + index);
        }
    }

    private void log(final String s) {
        if (listener != null) {
            listener.getLogger().println(s);
        }
    }
}
