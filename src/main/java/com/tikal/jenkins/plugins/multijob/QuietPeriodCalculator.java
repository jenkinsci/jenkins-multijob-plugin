package com.tikal.jenkins.plugins.multijob;

import groovy.util.Eval;
import hudson.model.BuildListener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class QuietPeriodCalculator {

	private final static Logger LOG = Logger.getLogger(QuietPeriodCalculator.class.getName());
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
		} catch (Throwable t) {
			final String message =
					"Error calculating quiet time for index " + index + " and quietPeriodGroovy [" + quietPeriodGroovy +
							"]: " + t.getMessage() + "; returning 0";
			LOG.log(Level.WARNING, message, t);
			log(message);
			return 0;
		}

	}

	public int calculateOrThrow(final String quietPeriodGroovy, final int index) {

		assertPositiveIndex(index);

		final Integer quietPeriod = (Integer) Eval.me(INDEX, index, quietPeriodGroovy);
		log(displayName + "Quiet period groovy=[" + quietPeriodGroovy + "], index=" + index + " -> quietPeriodGroovy=" + quietPeriod);
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
