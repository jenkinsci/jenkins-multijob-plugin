package com.tikal.jenkins.plugins.reactor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ReactorSubProjectConfig implements
		Describable<ReactorSubProjectConfig> {

	private String jobName;
	private String jobProperties;

	public String getJobProperties() {
		return jobProperties;
	}

	public void setJobProperties(String jobProperties) {
		this.jobProperties = jobProperties;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

   public Descriptor<ReactorSubProjectConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@DataBoundConstructor
	public ReactorSubProjectConfig(String jobName, String jobProperties) {
		super();
		this.jobName = jobName;
		this.jobProperties = jobProperties;
	}

	@Extension
    public static class DescriptorImpl extends Descriptor<ReactorSubProjectConfig> {
        @Override
        public String getDisplayName() {
            return "Phase Config"; 
        }

		public AutoCompletionCandidates doAutoCompleteJobName(
				@QueryParameter String value) {
			AutoCompletionCandidates c = new AutoCompletionCandidates();
			for (TopLevelItem jobName : Hudson.getInstance().getItems())
				if (jobName.getName().toLowerCase()
						.startsWith(value.toLowerCase()))
					c.add(jobName.getName());
			return c;
		}
    }

	public Action getAction(AbstractBuild build, TaskListener listener)
			throws IOException, InterruptedException {

		EnvVars env = build.getEnvironment(listener);

		Properties p = new Properties();
		p.load(new StringInputStream(jobProperties));

		List<ParameterValue> values = new ArrayList<ParameterValue>();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			values.add(new StringParameterValue(entry.getKey().toString(), env
					.expand(entry.getValue().toString())));
		}

		return new ParametersAction(values);
	}
	
	public boolean hasProperties() {
		return !this.jobProperties.isEmpty();
	}
}
