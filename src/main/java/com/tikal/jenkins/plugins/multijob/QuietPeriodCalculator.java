package com.tikal.jenkins.plugins.multijob;

import groovy.util.Eval;
import hudson.model.BuildListener;

import java.util.logging.Level;
import java.util.logging.Logger;

class QuietPeriodCalculator {

	private final static Logger LOG = Logger.getLogger(QuietPeriodCalculator.class.getName());
	private final BuildListener listener;
	private final String displayName;

	QuietPeriodCalculator() {
		this(null, null);
	}

	QuietPeriodCalculator(final BuildListener listener, final String displayNameOrNull) {
		this.listener = listener;
		this.displayName = displayNameOrNull == null ? "" : displayNameOrNull + ": ";
	}

	int calculate(String quietPeriodGroovy, int index) {

		if (quietPeriodGroovy == null) {
			return 0;
		}
		try {
			return calculateOrThrow(quietPeriodGroovy, index);
		} catch (Throwable t) {
			final String message =
					"Error calculating quiet time for index " + index + " and quietPeriodGroovy [" + quietPeriodGroovy +
							"]: " + t.getMessage() + "; returning 0";
			LOG.log(Level.WARNING, message, t);
			log(message);
			return 0;
		}

	}

	int calculateOrThrow(final String quietPeriodGroovy, final int index) {

		final Integer quietPeriod = (Integer) Eval.me("index", index, quietPeriodGroovy);
		log(displayName + "Quiet period groovy=[" + quietPeriodGroovy + "], index=" + index + " -> quietPeriodGroovy=" + quietPeriod);
		return quietPeriod;
	}

	private void log(final String s) {
		if (listener != null) {
			listener.getLogger().println(s);
		}
	}
}
