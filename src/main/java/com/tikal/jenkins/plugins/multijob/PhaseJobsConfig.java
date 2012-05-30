package com.tikal.jenkins.plugins.multijob;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BooleanParameterDefinition;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Descriptor;
import hudson.model.FileParameterValue;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
//import hudson.scm.SubversionSCM;
//import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

//import com.tikal.jenkins.plugins.multijob.scm.MultiJobScm;

public class PhaseJobsConfig implements Describable<PhaseJobsConfig> {

	private String jobName;
	private String jobProperties;
	private boolean currParams;
	private boolean exposedSCM;

	public boolean isExposedSCM() {
		return currParams;
	}

	public void setExposedSCM(boolean exposedSCM) {
		this.exposedSCM = exposedSCM;
	}
	
	public boolean isCurrParams() {
		return currParams;
	}

	public void setCurrParams(boolean currParams) {
		this.currParams = currParams;
	}

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
	public PhaseJobsConfig(String jobName, String jobProperties, boolean currParams) {
		this.jobName = jobName;
		this.jobProperties = jobProperties;
		this.currParams = currParams;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<PhaseJobsConfig> {
		@Override
		public String getDisplayName() {
			return "Phase Jobs Config";
		}

		public AutoCompletionCandidates doAutoCompleteJobName(@QueryParameter String value) {
			AutoCompletionCandidates c = new AutoCompletionCandidates();
			for (String localJobName : Hudson.getInstance().getJobNames()) {
				if (localJobName.toLowerCase().startsWith(value.toLowerCase()))
					c.add(localJobName);
			}
			return c;
		}

		public FormValidation doCheckJobName(@QueryParameter String value) {
			FormValidation result = FormValidation.errorWithMarkup("Invalid job name");
			if (value.isEmpty()) {
				result = FormValidation.errorWithMarkup("Job name must not be empty");
			} else {
				for (String localJobName : Hudson.getInstance().getJobNames()) {
					if (localJobName.toLowerCase().equals(value.toLowerCase())) {
                        //savePhaseJobConfigParameters(localJobName);
                        result = FormValidation.ok();
					}
				}
			}
			return result;
		}
		
		

        private void savePhaseJobConfigParameters(String localJobName) {
            AbstractProject project = ((AbstractProject) Hudson.getInstance().getItem(localJobName));
            List<ParameterDefinition> parameterDefinitions = getParameterDefinition(project);
            StringBuilder sb = new StringBuilder();
        //    ArrayList<ModuleLocation> scmLocation = null;
            for (ParameterDefinition pdef : parameterDefinitions) {
                String paramValue = null;
                if (pdef instanceof StringParameterDefinition) {
                    StringParameterDefinition stringParameterDefinition = (StringParameterDefinition) pdef;
                    paramValue = stringParameterDefinition.getDefaultParameterValue().value;
                } else if (pdef instanceof BooleanParameterDefinition) {
                    BooleanParameterDefinition booleanParameterDefinition = (BooleanParameterDefinition) pdef;
                    paramValue = String.valueOf(booleanParameterDefinition.getDefaultParameterValue().value);
                }
                sb.append(pdef.getName()).append("=").append(paramValue).append("\n");
            }
            

            AbstractProject item = getCurrentJob();
            if (item instanceof MultiJobProject) {
                MultiJobProject parentProject = (MultiJobProject) item;
                List<Builder> builders = parentProject.getBuilders();
                if (builders != null) {
                    for (Builder builder : builders) {
                        if (builder instanceof MultiJobBuilder) {
                            MultiJobBuilder multiJobBuilder = (MultiJobBuilder) builder;
                            List<PhaseJobsConfig> phaseJobs = multiJobBuilder.getPhaseJobs();
                            for (PhaseJobsConfig phaseJob : phaseJobs) {
                                if (phaseJob.getJobName().equals(localJobName)) {
                                    phaseJob.setJobProperties(sb.toString());
//                                    if (phaseJob.isExposedSCM()){
//                                    	if (parentProject.getScm().getType().equals(MultiJobScm.class.getName())){
//                                    		((MultiJobScm)parentProject.getScm()).addScm(project, project.getScm());
//                                    	}
//                                    }
                                    save();
                                }
                            }
                        }
                    }
                 
                }
//                if (parentProject.getScm().getType().equals(MultiJobScm.class.getName()) && scmLocation !=null){
//                	scmLocation.addAll(Arrays.asList(((SubversionSCM)parentProject.getScm()).getLocations()));
//                	SubversionSCM scm =new SubversionSCM(scmLocation, ((SubversionSCM)parentProject.getScm()).getWorkspaceUpdater(),((SubversionSCM)parentProject.getScm()).getBrowser(),((SubversionSCM)parentProject.getScm()).getExcludedRegions(),((SubversionSCM)parentProject.getScm()).getExcludedUsers(), ((SubversionSCM)parentProject.getScm()).getExcludedRevprop(),((SubversionSCM)parentProject.getScm()).getExcludedCommitMessages(), ((SubversionSCM)parentProject.getScm()).getIncludedRegions());
//                	try {
//						parentProject.setScm(scm);
//					} catch (IOException e) {
//						e.fillInStackTrace();
//					}
//                }
            }
            
        }

        private AbstractProject getCurrentJob() {
            String nameUrl = Descriptor.getCurrentDescriptorByNameUrl();
            String jobName = nameUrl.substring(nameUrl.lastIndexOf("/")+1);
            return (AbstractProject) Hudson.getInstance().getItem(jobName);
        }

        public List<ParameterDefinition> getParameterDefinition(AbstractProject project) {
			List<ParameterDefinition> list = new ArrayList<ParameterDefinition>();
			Map<JobPropertyDescriptor, JobProperty> map = project.getProperties();
			for (Map.Entry<JobPropertyDescriptor, JobProperty> entry : map.entrySet()) {
				JobProperty property = entry.getValue();
				if (property instanceof ParametersDefinitionProperty) {
					ParametersDefinitionProperty pdp = (ParametersDefinitionProperty) property;
					for (ParameterDefinition parameterDefinition : pdp.getParameterDefinitions()) {
						if (parameterDefinition instanceof StringParameterDefinition || parameterDefinition instanceof BooleanParameterDefinition
								|| parameterDefinition instanceof ChoiceParameterDefinition) {
							list.add(parameterDefinition);
						}
					}
				}
			}
			return list;
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

	public Action getAction(AbstractBuild build, TaskListener listener) throws IOException, InterruptedException {

		EnvVars env = build.getEnvironment(listener);

		Properties p = new Properties();
		p.load(new StringInputStream(jobProperties));

		List<ParameterValue> values = new ArrayList<ParameterValue>();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			values.add(new StringParameterValue(entry.getKey().toString(), env.expand(entry.getValue().toString())));
		}

		return new ParametersAction(values);
	}

	public boolean hasProperties() {
		return !this.jobProperties.isEmpty();
	}
}
