package com.tikal.jenkins.plugins.reactor;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TopLevelItem;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import com.tikal.jenkins.plugins.reactor.ReactorBuilder.DescriptorImpl;

public class ReactorSubProjectConfig implements Action , 
		Describable<ReactorSubProjectConfig> {

	private String jobName;

	@Exported
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

	@Extension
	public static final class DescriptorImpl extends ReactorSubProjectDescriptor{
		/**
		 * This method provides auto-completion items for the 'Jobs' field.
		 * Stapler finds this method via the naming convention.
		 * 
		 * @param value
		 *            The text that the user entered.
		 */
		public AutoCompletionCandidates doAutoCompleteState(
				@QueryParameter String value) {
			AutoCompletionCandidates c = new AutoCompletionCandidates();
			for (TopLevelItem jobName : Hudson.getInstance().getItems())
				if (jobName.getName().toLowerCase().startsWith(
						value.toLowerCase()))
					c.add(jobName.getName());
			return c;
		}
	}

	public String getDisplayName() {
		// TODO Auto-generated method stub
		return getClass().getSimpleName();
	}

	public String getIconFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUrlName() {
		// TODO Auto-generated method stub
		return  getClass().getSimpleName();
	}

	public ReactorSubProjectConfig(String jobName) {
		super();
		this.jobName = jobName;
	}

}
