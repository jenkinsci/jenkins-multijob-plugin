package com.tikal.jenkins.plugins.multijob;

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
import hudson.model.FileParameterValue;
import hudson.model.Hudson;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class PhaseJobsConfig implements
		Describable<PhaseJobsConfig> {

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

   public Descriptor<PhaseJobsConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@DataBoundConstructor
	public PhaseJobsConfig(String jobName, String jobProperties) {
		this.jobName = jobName;
		this.jobProperties = jobProperties;
	}

	@Extension
    public static class DescriptorImpl extends Descriptor<PhaseJobsConfig> {
        @Override
        public String getDisplayName() {
            return "Phase Jobs Config"; 
        }

		public AutoCompletionCandidates doAutoCompleteJobName(
				@QueryParameter String value) {
			AutoCompletionCandidates c = new AutoCompletionCandidates();
			for (TopLevelItem job : Hudson.getInstance().getItems()) {
				String localJobName=job.getName();
				if (localJobName.toLowerCase()
						.startsWith(value.toLowerCase()))
						c.add(job.getName());
			}
			return c;
		}
		public FormValidation doCheckJobName(@QueryParameter String value) {
			FormValidation result = FormValidation.errorWithMarkup("Invalid job name");
			if (!value.isEmpty()) {
				for (TopLevelItem job : Hudson.getInstance().getItems()) {
					String localJobName=job.getName();
					if (localJobName.toLowerCase()
							.equals(value.toLowerCase()))
						result=FormValidation.ok();
				}
			} else {
				result=FormValidation.ok();
			}
			
			return result;
		}
		
		 public String doFillJobProperties(@QueryParameter String jobName) {
			 	
	            return "fill=in";
	        }

	}

	public List<ParameterValue> getJobParameters(AbstractBuild<?, ?> build, TaskListener listener) {
		ParametersAction action = build.getAction(ParametersAction.class);
		List<ParameterValue> values = new ArrayList<ParameterValue>(action.getParameters().size());
		if (action != null) {
			for (ParameterValue value : action.getParameters())
				// FileParameterValue is currently not reusable, so omit these:
				if (!(value instanceof FileParameterValue))
					values.add(value);
		}

		return values;
		
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
