package com.tikal.jenkins.plugins.multijob.test.testutils;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

public class WaitForJobsJenkinsRule extends JenkinsRule {

	private static final Logger logger = LoggerFactory.getLogger(WaitForJobsJenkinsRule.class);
	private static final Lock buildStartLock = new ReentrantLock();
	private static final Condition buildStartCondition = buildStartLock.newCondition();
	private static final Lock buildCompletedLock = new ReentrantLock();
	private static final Condition buildCompletedCondition = buildCompletedLock.newCondition();
	@Extension
	public static final RunListener<Run<?, ?>> runListener = new RunListener<Run<?, ?>>() {

		@Override
		public void onStarted(hudson.model.Run<?, ?> run, TaskListener listener) {
			logger.info("{}", run);
			buildStartLock.lock();
			try {
				buildStartCondition.signalAll();
			} finally {
				buildStartLock.unlock();
			}
		};

		@Override
		public void onCompleted(hudson.model.Run<?, ?> run, TaskListener listener) {
			logger.info("{}", run);
			buildCompletedLock.lock();
			try {
				buildCompletedCondition.signalAll();
			} finally {
				buildCompletedLock.unlock();
			}
		}
	};

	public void waitForAnyBuildStart(long time, TimeUnit unit)
			throws InterruptedException {
		logger.info("Waiting {} {}...", time, unit);
		buildStartLock.lock();
		try {
			boolean await = buildStartCondition.await(time, unit);
			logger.info("Done waiting, success = {}", await);
			assertTrue(await);
		} finally {
			buildStartLock.unlock();
		}
	}

	public void waitForAnyBuildCompletion(long time, TimeUnit unit)
			throws InterruptedException {
		logger.info("Waiting {} {}...", time, unit);
		buildCompletedLock.lock();
		try {
			boolean await = buildCompletedCondition.await(time, unit);
			logger.info("Done waiting, success = {}", await);
			assertTrue(await);
		} finally {
			buildCompletedLock.unlock();
		}
	}
}
