package com.tikal.jenkins.plugins.reactor;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TopLevelItem;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import com.tikal.jenkins.plugins.reactor.ReactorBuilder.DescriptorImpl;

public class ReactorSubProjectConfig implements  
		Describable<ReactorSubProjectConfig> {

	private String jobName;

	
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public ReactorSubProjectDescriptor getDescriptor() {
		// TODO Auto-generated method stub
		return (ReactorSubProjectDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
	}

	

	public String getDisplayName() {
		// TODO Auto-generated method stub
		return getClass().getSimpleName();
	}

	@DataBoundConstructor
	public ReactorSubProjectConfig(String jobName) {
		super();
		this.jobName = jobName;
	}

}
