/*
 * The MIT License
 *
 * Copyright (c) 2013, Chris Johnson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.tikal.jenkins.plugins.multijob.test;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobParametersAction;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause.UserIdCause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Johnson
 */
@WithJenkins
class PhaseJobsConfigTest {

    private static final Map<String, String> DEFAULT_KEY_VALUES = new HashMap<>() {{
        put("key1", "value1");
        put("key2", "value2");
        put("key3", "value3");
    }};
    private static final Map<String, String> CURRENT_KEY_VALUES = new HashMap<>() {{
        put("key4", "value4");
        put("key5", "value5");
        put("key6", "value6");
    }};
    private static final Map<String, String> OVERRIDES_KEY_VALUES = new HashMap<>() {{
        put("key2", "value4");
        put("key3", "value5");
    }};
    private static final Map<String, String> CONFIG_OVERRIDES_KEY_VALUES = new HashMap<>() {{
        put("key3", "value9");
    }};

    @Test
    void testNoParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, null);
        MultiJobBuild mjb = createTriggeringBuild(null);

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "", true, false);

        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);
        // check single ParametersAction created
        assertEquals(0, actions.size());
    }

    @Test
    void testDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(null);

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null,
				KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);
        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

        // check single ParametersAction created
        assertEquals(1, actions.size());

        MultiJobParametersAction pa = getParametersAction(actions);
        checkParameterMatch(DEFAULT_KEY_VALUES, pa);
    }

    /**
     * Test that both the default and current build parameters are combined
     */
    @Test
    void testCurrentDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(createParametersAction(CURRENT_KEY_VALUES));

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null,
				KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);

        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

        // check single ParametersAction created
        assertEquals(1, actions.size());
        MultiJobParametersAction pa = getParametersAction(actions);

        HashMap<String, String> combined = new HashMap<>(DEFAULT_KEY_VALUES);
        combined.putAll(CURRENT_KEY_VALUES);

        checkParameterMatch(combined, pa);
    }

    /**
     * Test that the current build parameters override default ones and are combined
     */
    @Test
    void testCurrentOverridesDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null,
				KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);

        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

        // check single ParametersAction created
        assertEquals(1, actions.size());
        MultiJobParametersAction pa = getParametersAction(actions);

        HashMap<String, String> combined = new HashMap<>(DEFAULT_KEY_VALUES);
        combined.putAll(OVERRIDES_KEY_VALUES);

        checkParameterMatch(combined, pa);
    }

    /**
     * Test that the current build parameters are ignored and use just the default ones
     */
    @Test
    void testCurrentIgnoredDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null,
				KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);
        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, false);

        // check single ParametersAction created
        assertEquals(1, actions.size());
        MultiJobParametersAction pa = getParametersAction(actions);

        checkParameterMatch(DEFAULT_KEY_VALUES, pa);
    }

    /**
     * Test that the current build parameters are ignored and use just the default ones
     */
    @Test
    void testConfigsDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(null);

        List<AbstractBuildParameters> configs = new ArrayList<>();
        configs.add(new TestCauseConfig());
        configs.add(new TestParametersConfig());
        configs.add(new TestParametersConfig(OVERRIDES_KEY_VALUES));

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true,
				configs, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);


        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

        // check 2 actions created
        assertEquals(2, actions.size());
        MultiJobParametersAction pa = getParametersAction(actions);

        //check that expected parameter is listed
        HashMap<String, String> combined = new HashMap<>(DEFAULT_KEY_VALUES);
        combined.putAll(OVERRIDES_KEY_VALUES);

        checkParameterMatch(combined, pa);
    }

    /**
     * Test that the config overrides current overrides default values
     */
    @Test
    void testCurrentConfigsDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

        List<AbstractBuildParameters> configs = new ArrayList<>();
        configs.add(new TestCauseConfig());
        configs.add(new TestParametersConfig());
        configs.add(new TestParametersConfig(CONFIG_OVERRIDES_KEY_VALUES));

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, configs,
				KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);

        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

        // check 2 actions created
        assertEquals(2, actions.size());
        MultiJobParametersAction pa = getParametersAction(actions);

        HashMap<String, String> combined = new HashMap<>(DEFAULT_KEY_VALUES);
        combined.putAll(OVERRIDES_KEY_VALUES);
        combined.putAll(CONFIG_OVERRIDES_KEY_VALUES);

        checkParameterMatch(combined, pa);
    }

    /**
     * Test that the config overrides default values ignoring current values
     */
    @Test
    void testNotCurrentConfigsDefaultParameters(JenkinsRule j) throws Exception {
        AbstractProject projectB = createTriggeredProject(j, DEFAULT_KEY_VALUES);
        MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

        List<AbstractBuildParameters> configs = new ArrayList<>();
        configs.add(new TestCauseConfig());
        configs.add(new TestParametersConfig());
        configs.add(new TestParametersConfig(CONFIG_OVERRIDES_KEY_VALUES));

        PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, configs,
				KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
				false, false, "", false, false);


        List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, false);

        // check 2 actions created
        assertEquals(2, actions.size());
        MultiJobParametersAction pa = getParametersAction(actions);
        HashMap<String, String> combined = new HashMap<>(DEFAULT_KEY_VALUES);
        combined.putAll(CONFIG_OVERRIDES_KEY_VALUES);

        checkParameterMatch(combined, pa);
    }

    private static MultiJobBuild createTriggeringBuild(MultiJobParametersAction parametersAction) throws Exception {
        // set up the triggering build
        MultiJobProject projectA = new MultiJobProject((Hudson) Jenkins.get(), "ssss");
        MultiJobBuild mjb = new MultiJobBuild(projectA);
        // add build ParametersAction
        if (parametersAction != null) {
            mjb.getActions().add(parametersAction);
        }
        return mjb;
    }

    private static AbstractProject createTriggeredProject(JenkinsRule j, Map<String, String> defaultParameters) throws Exception {
        // set up the project to be triggered
        FreeStyleProject projectB = j.createFreeStyleProject();
        if (defaultParameters != null) {
            List<ParameterDefinition> pds = new ArrayList<>();

            for (String name : defaultParameters.keySet()) {
                pds.add(new StringParameterDefinition(name, defaultParameters.get(name)));
            }

            ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(pds);
            projectB.addProperty(pdp);
        }
        return projectB;
    }

    private static MultiJobParametersAction createParametersAction(Map<String, String> items) {
        List<ParameterValue> params = new ArrayList<>();
        if (items != null) {
            for (String name : items.keySet()) {
                params.add(new StringParameterValue(name, items.get(name)));
            }
        }
        return new MultiJobParametersAction(params);
    }

    private static void checkParameterMatch(Map<String, String> combined, MultiJobParametersAction pa) {
        assertNotNull(pa);
        assertEquals(combined.size(), pa.getParameters().size());
        for (String key : combined.keySet()) {
            assertEquals(((StringParameterValue) pa.getParameter(key)).value, combined.get(key));
        }
    }

    private static MultiJobParametersAction getParametersAction(List<Action> actions) {
        MultiJobParametersAction pa = null;
        for (Action a : actions) {
            if (a instanceof MultiJobParametersAction) {
                pa = (MultiJobParametersAction) a;
            }
        }
        return pa;
    }

    /**
     * Config item returning a cause action
     */
    static class TestCauseConfig extends AbstractBuildParameters {

        @Override
        public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) {
            return new CauseAction(new UserIdCause());
        }
    }

    /**
     * Config item returning a ParametersAction
     */
    static class TestParametersConfig extends AbstractBuildParameters {
        private final Map<String, String> items;

        public TestParametersConfig() {
            this.items = null;
        }

        public TestParametersConfig(Map<String, String> items) {
            this.items = items;
        }

        @Override
        public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) {
            return createParametersAction(items);
        }
    }
}
