package com.tikal.jenkins.plugins.multijob;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;

public class FileBuildParameters extends AbstractBuildParameters {

    private final String propertiesFile;

    @DataBoundConstructor
    public FileBuildParameters(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public Action getAction(AbstractBuild<?,?> build, TaskListener listener, AbstractProject project)
            throws IOException, InterruptedException {

        EnvVars env = build.getEnvironment(listener);

        String resolvedPropertiesFile = env.expand(propertiesFile);
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            return null;
        }
        FilePath f = workspace.child(resolvedPropertiesFile);
        if (!f.exists()) {
            listener
                    .getLogger()
                    .println(
                            "[parameterizedtrigger] Could not trigger downstream project, as properties file "
                                    + resolvedPropertiesFile
                                    + " did not exist.");
            return null;
        }

        String s = f.readToString();
        s = env.expand(s);
        Properties p = new Properties();
        try (StringInputStream is = new StringInputStream(s)) {
            p.load(is);
        }

        List<ParameterValue> values = new ArrayList<ParameterValue>();
        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            values.add(new StringParameterValue(entry.getKey().toString(),
                    entry.getValue().toString()));
        }

        return new ParametersAction(values);

    }

    public String getPropertiesFile() {
        return propertiesFile;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
        @Override
        public String getDisplayName() {
            return "Parameters from properties file";
        }
    }

}
