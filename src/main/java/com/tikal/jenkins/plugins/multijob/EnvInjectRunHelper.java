package com.tikal.jenkins.plugins.multijob;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Util;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;
import org.jenkinsci.plugins.envinject.service.BuildCauseRetriever;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// This class is a copy of RunHelper from envinject plugin
//TODO: Ideally it should be offered by the core

/**
 * This method contains abstraction layers for methods, which are available only in {@link AbstractBuild}.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class EnvInjectRunHelper {
    
    private static final Logger LOGGER = Logger.getLogger(EnvInjectRunHelper.class.getName());
    
    /**
     * Gets build variables.
     * For {@link AbstractBuild} it invokes the standard method, 
     * for other types it relies on {@link ParametersAction} only.
     * @param run Run
     * @param result Target collection, where the variables will be added
     */
    public static void getBuildVariables(@NonNull Run<?, ?> run, EnvVars result) {
        if (run instanceof AbstractBuild) {
            Map buildVariables = ((AbstractBuild)run).getBuildVariables();
            result.putAll(buildVariables);
        }
           
        ParametersAction parameters = run.getAction(ParametersAction.class);
        if (parameters!=null) {
            // TODO: not sure if buildEnvironment is safe in this context (e.g. FileParameter)
            for (ParameterValue p : parameters) {
                p.buildEnvironment(run, result);
            }
        }
    }
    
    /**
     * Gets JDK variables.
     * For {@link AbstractBuild} it invokes operation on the node to retrieve the data; 
     * for other types it does nothing.
     * @param run Run
     * @param logger Logger
     * @param result Target collection, where the variables will be added
     * @throws IOException Operation failure
     * @throws InterruptedException Operation has been interrupted
     */
    public static void getJDKVariables(@NonNull Run<?, ?> run, TaskListener logger, EnvVars result) 
            throws IOException, InterruptedException {
        if (run instanceof AbstractBuild) {
            AbstractBuild b = (AbstractBuild) run;
            JDK jdk = b.getProject().getJDK();
            if (jdk != null) {
                Node node = b.getBuiltOn();
                if (node != null) {
                    jdk = jdk.forNode(node, logger);
                }
                jdk.buildEnvVars(result);
            }
        }
    }
    
    // Moved from EnvInjectVariableGetter
    
    public static Map<String, String> getBuildVariables(@NonNull Run<?, ?> run, @NonNull EnvInjectLogger logger) throws EnvInjectException {
        EnvVars result = new EnvVars();

        //Add build process variables
        result.putAll(run.getCharacteristicEnvVars());

        try {
            EnvVars envVars = new EnvVars();
            for (EnvironmentContributor ec : EnvironmentContributor.all()) {
                ec.buildEnvironmentFor(run, envVars, new LogTaskListener(LOGGER, Level.ALL));
                result.putAll(envVars);
            }
            
            // Handle JDK
            EnvInjectRunHelper.getJDKVariables(run, logger.getListener(), result);
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }

        Executor e = run.getExecutor();
        if (e != null) {
            result.put("EXECUTOR_NUMBER", String.valueOf(e.getNumber()));
        }

        String rootUrl = Jenkins.getActiveInstance().getRootUrl();
        if (rootUrl != null) {
            result.put("BUILD_URL", rootUrl + run.getUrl());
            result.put("JOB_URL", rootUrl + run.getParent().getUrl());
        }

        //Add build variables such as parameters, plugins contributions, ...
        EnvInjectRunHelper.getBuildVariables(run, result);

        //Retrieve triggered cause
        Map<String, String> triggerVariable = new BuildCauseRetriever().getTriggeredCause(run);
        result.putAll(triggerVariable);

        return result;
    }

    @NonNull
    public static Map<String, String> getEnvVarsPreviousSteps(
            @NonNull Run<?, ?> build, @NonNull EnvInjectLogger logger) 
            throws IOException, InterruptedException, EnvInjectException {
        Map<String, String> result = new HashMap<String, String>();

        // Env vars contributed by build wrappers; no replacement in Pipeline
        if (build instanceof AbstractBuild) {
            List<Environment> environmentList = ((AbstractBuild)build).getEnvironments();
            if (environmentList != null) {
                for (Environment e : environmentList) {
                    if (e != null) {
                        e.buildEnvVars(result);
                    }
                }
            }
        }

        EnvInjectPluginAction envInjectAction = build.getAction(EnvInjectPluginAction.class);
        if (envInjectAction != null) {
            result.putAll(getCurrentInjectedEnvVars(envInjectAction));
            //Add build variables with axis for a MatrixRun
            if (build instanceof MatrixRun) {
                result.putAll(((MatrixRun)build).getBuildVariables());
            }
        } else {
            result.putAll(getJenkinsSystemEnvVars(false));
            result.putAll(getBuildVariables(build, logger));
        }
        return result;
    }

    @NonNull
    private static Map<String, String> getCurrentInjectedEnvVars(@NonNull EnvInjectPluginAction envInjectPluginAction) {
        Map<String, String> envVars = envInjectPluginAction.getEnvMap();
        return (envVars) == null ? new HashMap<String, String>() : envVars;
    }

    @NonNull
    public static Map<String, String> getJenkinsSystemEnvVars(boolean forceOnMaster) throws IOException, InterruptedException {
        Map<String, String> result = new TreeMap<String, String>();

        final Computer computer;
        final Jenkins jenkins = Jenkins.get();
        if (forceOnMaster) {

            computer = jenkins.toComputer();
        } else {
            computer = Computer.currentComputer();
        }

        //test if there is at least one executor
        if (computer != null) {
            result = computer.getEnvironment().overrideAll(result);
            if(computer instanceof Jenkins.MasterComputer) {
                result.put("NODE_NAME", Jenkins.get().getSelfLabel().getName());
            } else {
                result.put("NODE_NAME", computer.getName());
            }

            Node n = computer.getNode();
            if (n != null) {
                result.put("NODE_LABELS", Util.join(n.getAssignedLabels(), " "));
            }
        }

        String rootUrl = jenkins.getRootUrl();
        if (rootUrl != null) {
            result.put("JENKINS_URL", rootUrl);
            result.put("HUDSON_URL", rootUrl); // Legacy compatibility
        }
        result.put("JENKINS_HOME", jenkins.getRootDir().getPath());
        result.put("HUDSON_HOME", jenkins.getRootDir().getPath());   // legacy compatibility

        return result;
    }
}
