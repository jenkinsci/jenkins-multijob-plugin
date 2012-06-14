package com.tikal.jenkins.plugins.multijob.test;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;

import com.tikal.jenkins.plugins.multijob.MultiJobProject;

public class MultiJobProjectTestCase extends HudsonTestCase {
	 protected MultiJobProject createMultiJobProject(String name) throws IOException {
	     return hudson.createProject(MultiJobProject.class,name);
	 }

	public void test() throws Exception {
	}
}
