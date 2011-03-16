package com.tikal.jenkins.plugins.multijob;

import hudson.model.Build;

import java.io.File;
import java.io.IOException;

public class MultiJobBuild extends Build<MultiJobProject, MultiJobBuild> {

	public MultiJobBuild(MultiJobProject project) throws IOException {
		super(project);
	}

	public MultiJobBuild(MultiJobProject project, File buildDir) throws IOException {
		super(project, buildDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		run(new RunnerImpl());
	}

	protected class RunnerImpl extends Build<MultiJobProject, MultiJobBuild>.RunnerImpl {

	}
}
