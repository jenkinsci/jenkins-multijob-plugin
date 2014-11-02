package com.tikal.jenkins.plugins.multijob;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ParametersAction;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedBuildParameters extends AbstractBuildParameters {

    private String jobProperties;

    @DataBoundConstructor
    public PredefinedBuildParameters(String jobProperties) {
        this.jobProperties = jobProperties;
    }

    public Action getAction(AbstractBuild<?,?> build, TaskListener listener, AbstractProject project)
            throws IOException, InterruptedException {
        EnvVars env = build.getEnvironment(listener);
//        List actions = project.getActions();
//        ParametersDefinitionProperty parameters=null;
//        for (Object object : actions) {
//            if(object instanceof hudson.model.ParametersDefinitionProperty)
//                parameters = (ParametersDefinitionProperty)object;
//
//        }
        Properties pProp = new Properties();
        pProp.load(new StringInputStream(jobProperties));
        LinkedHashMap<String,ParameterValue> params = new LinkedHashMap<String,ParameterValue>();

//        if (parameters !=null){
//            boolean overwrite=false;
//            for (ParameterDefinition parameterdef : parameters.getParameterDefinitions()) {
//                params.put(parameterdef.getName(),parameterdef.getDefaultParameterValue());
                for (Map.Entry<Object, Object> entry : pProp.entrySet()) {
//                    if (parameterdef.getName().equals(entry.getKey())){
                        //override with multyjob value
                        params.put(entry.getKey().toString(), new StringParameterValue(entry.getKey().toString(),
                                env.expand(entry.getValue().toString())));
                        //((SimpleParameterDefinition)parameterdef).createValue(env.expand(entry.getValue().toString())));
                       // values.add(((SimpleParameterDefinition)parameterdef).createValue(env.expand(entry.getValue().toString())));
//                        break;
                }
//                }
//            }
//         }
        return new ParametersAction(params.values().toArray(new ParameterValue[params.size()]));
    }

    public String getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(String jobProperties) {
        this.jobProperties = jobProperties;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
        @Override
        public String getDisplayName() {
            return "Predefined parameters";
        }
    }
}
