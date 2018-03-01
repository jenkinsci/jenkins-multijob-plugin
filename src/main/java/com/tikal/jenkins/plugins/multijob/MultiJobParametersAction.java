package com.tikal.jenkins.plugins.multijob;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Restricted(NoExternalUse.class)
public class MultiJobParametersAction extends ParametersAction {

    private List<ParameterValue> parameters;

    public MultiJobParametersAction(@Nonnull List<ParameterValue> parameters) {
        super(parameters);
        this.parameters = parameters;
    }

    public MultiJobParametersAction(ParameterValue... parameters) {
        this(Arrays.asList(parameters));
    }

    @Override
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public ParameterValue getParameter(String name) {
        for (ParameterValue parameter : parameters) {
            if (parameter != null && parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    @Extension
    public static final class MultiJobParametersActionEnvironmentContributor extends EnvironmentContributor {

        @Override
        public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
            MultiJobParametersAction action = r.getAction(MultiJobParametersAction.class);
            if (action != null) {
                for (ParameterValue p : action.getParameters()) {
                    envs.putIfNotNull(p.getName(), String.valueOf(p.getValue()));
                }
            }
        }
    }
}
