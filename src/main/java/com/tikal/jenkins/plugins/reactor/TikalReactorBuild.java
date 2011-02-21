package com.tikal.jenkins.plugins.reactor;

import hudson.model.Build;

import java.io.File;
import java.io.IOException;

public class TikalReactorBuild extends Build<TikalReactorProject, TikalReactorBuild> {

	public TikalReactorBuild(TikalReactorProject project) throws IOException {
		super(project);
	}

	public TikalReactorBuild(TikalReactorProject project, File buildDir) throws IOException {
		super(project, buildDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		run(new RunnerImpl());
	}

	protected class RunnerImpl extends Build<TikalReactorProject, TikalReactorBuild>.RunnerImpl {

	}
}
