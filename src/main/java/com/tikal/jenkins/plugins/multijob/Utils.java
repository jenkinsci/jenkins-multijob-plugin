package com.tikal.jenkins.plugins.multijob;

import com.tikal.jenkins.plugins.multijob.views.TableProperty;
import hudson.EnvVars;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.User;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Utils {

    private Utils() {
    }

    public static Map<Object, Object> getBindings(String bindings) throws IOException {
        Map<Object, Object> binding = new HashMap<Object, Object>();
        binding.putAll(parseProperties(bindings));
        return binding;
    }

    public static @Nonnull Properties parseProperties(final String properties) throws IOException {
        Properties props = new Properties();

        if (null != properties) {
            try {
                props.load(new StringReader(properties));
            } catch (NoSuchMethodError e) {
                props.load(new ByteArrayInputStream(properties.getBytes()));
            }
        }
        return props;
    }

    public static TableProperty getTableProperty() throws IOException {
        User user = Jenkins.getInstance().getUser(Jenkins.getAuthentication().getName());
        TableProperty property = user.getProperty(TableProperty.class);
        if (null == property) {
            property = TableProperty.DESCRIPTOR.newInstance(user);
            user.addProperty(property);
        }
        return property;
    }

    public static Map<String, String> getEnvVars(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        Map<String, String> ret = new HashMap<String, String>();
        EnvVars envVars = build.getEnvironment(listener);
        envVars.overrideAll(build.getBuildVariables());
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    public static boolean rebuildPluginAvailable() {
        try {
            Class.forName("com.sonyericsson.rebuild.Rebuilder");
            PluginManager pluginManager = Jenkins.getInstance().getPluginManager();
            PluginWrapper rebuildPluginWrapper = pluginManager.getPlugin("rebuild");
            return rebuildPluginWrapper != null && rebuildPluginWrapper.isActive();
        } catch (ClassNotFoundException ignore) {
            return false;
        }
    }

    public static List<Action> copyBuildCauses(Run<?, ?> run) {
        List<Action> actions = new ArrayList<Action>();
        boolean hasUserIdCause = false;
        for (Object cause : run.getCauses()) {
            if (cause instanceof Cause.UserIdCause) {
                hasUserIdCause = true;
                actions.add(new CauseAction(new Cause.UserIdCause()));
            } else {
                actions.add(new CauseAction((Cause) cause));
            }
        }
        if (!hasUserIdCause) {
            actions.add(new CauseAction(new Cause.UserIdCause()));
        }
        actions.addAll(run.getActions(ParametersAction.class));
        return actions;
    }
}
