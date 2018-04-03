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

import com.tikal.jenkins.plugins.multijob.MultiJobParametersAction;
import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UserIdCause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
/**
 *
 * @author Chris Johnson
 */
public class PhaseJobsConfigTest extends HudsonTestCase{

	private static final Map<String, String> DEFAULT_KEY_VALUES = new HashMap<String, String>() {{
		put("key1", "value1");
		put("key2", "value2");
		put("key3", "value3");
	}};
	private static final Map<String, String> CURRENT_KEY_VALUES = new HashMap<String, String>() {{
		put("key4", "value4");
		put("key5", "value5");
		put("key6", "value6");
	}};
	private static final Map<String, String> OVERRIDES_KEY_VALUES = new HashMap<String, String>() {{
		put("key2", "value4");
		put("key3", "value5");
	}};
	private static final Map<String, String> CONFIG_OVERRIDES_KEY_VALUES = new HashMap<String, String>() {{
		put("key3", "value9");
	}};

	@Test
	public void testNoParameters() throws Exception {
		AbstractProject projectB = createTriggeredProject(null);
		MultiJobBuild mjb =createTriggeringBuild(null);

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy","dummyAlias", "", true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "" , true, false);

		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);
		// check single ParametersAction created
		assertEquals(0, actions.size());
	}

	@Test
	public void testDefaultParameters() throws Exception {
		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(null);

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy","dummyAlias", "", true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);
		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

		// check single ParametersAction created
		assertEquals(1, actions.size());

		MultiJobParametersAction pa = getParametersAction(actions);
		checkParameterMatch(DEFAULT_KEY_VALUES, pa);

	}

	@Test
	/**
	 * Test that both the default and current build parameters are combined
	 */
	public void testCurrentDefaultParameters() throws Exception {

		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(createParametersAction(CURRENT_KEY_VALUES));

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias","", true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "" , false, false);

		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

		// check single ParametersAction created
		assertEquals(1, actions.size());
		MultiJobParametersAction pa = getParametersAction(actions);

		HashMap<String,String> combinedlist = new HashMap<String,String>(DEFAULT_KEY_VALUES);
		combinedlist.putAll(CURRENT_KEY_VALUES);

		checkParameterMatch(combinedlist, pa);
	}

	@Test
	/**
	 * Test that the current build parameters override default ones and are combined
	 */
	public void testCurrentOverridesDefaultParameters() throws Exception {

		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);

		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

		// check single ParametersAction created
		assertEquals(1, actions.size());
		MultiJobParametersAction pa = getParametersAction(actions);

		HashMap<String,String> combinedlist = new HashMap<String,String>(DEFAULT_KEY_VALUES);
		combinedlist.putAll(OVERRIDES_KEY_VALUES);

		checkParameterMatch(combinedlist, pa);
	}
	@Test
	/**
	 * Test that the current build parameters are ignored and use just the default ones
	 */
	public void testCurrentIgnoredDefaultParameters() throws Exception {

		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);
		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, false);

		// check single ParametersAction created
		assertEquals(1, actions.size());
		MultiJobParametersAction pa = getParametersAction(actions);

		checkParameterMatch(DEFAULT_KEY_VALUES, pa);
	}
	@Test
	/**
	 * Test that the current build parameters are ignored and use just the default ones
	 */
	public void testConfigsDefaultParameters() throws Exception {

		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(null);

		List<AbstractBuildParameters> configs = new ArrayList<AbstractBuildParameters>();
		configs.add(new TestCauseConfig());
		configs.add(new TestParametersConfig());
		configs.add(new TestParametersConfig(OVERRIDES_KEY_VALUES));

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias","", true, configs, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);


		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

		// check 2 actions created
		assertEquals(2, actions.size());
		MultiJobParametersAction pa = getParametersAction(actions);

		//check that expected parameter is listed
		HashMap<String,String> combinedlist = new HashMap<String,String>(DEFAULT_KEY_VALUES);
		combinedlist.putAll(OVERRIDES_KEY_VALUES);

		checkParameterMatch(combinedlist, pa);
	}
	@Test
	/**
	 * Test that the config overrides current overrides default values
	 */
	public void testCurrentConfigsDefaultParameters() throws Exception {

		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

		List<AbstractBuildParameters> configs = new ArrayList<AbstractBuildParameters>();
		configs.add(new TestCauseConfig());
		configs.add(new TestParametersConfig());
		configs.add(new TestParametersConfig(CONFIG_OVERRIDES_KEY_VALUES));

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, configs, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);

		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, true);

		// check 2 actions created
		assertEquals(2, actions.size());
		MultiJobParametersAction pa = getParametersAction(actions);

		HashMap<String,String> combinedlist = new HashMap<String,String>(DEFAULT_KEY_VALUES);
		combinedlist.putAll(OVERRIDES_KEY_VALUES);
		combinedlist.putAll(CONFIG_OVERRIDES_KEY_VALUES);

		checkParameterMatch(combinedlist, pa);
	}

		@Test
	/**
	 * Test that the config overrides default values ignoring current values
	 */
	public void testNotCurrentConfigsDefaultParameters() throws Exception {

		AbstractProject projectB = createTriggeredProject(DEFAULT_KEY_VALUES);
		MultiJobBuild mjb = createTriggeringBuild(createParametersAction(OVERRIDES_KEY_VALUES));

		List<AbstractBuildParameters> configs = new ArrayList<AbstractBuildParameters>();
		configs.add(new TestCauseConfig());
		configs.add(new TestParametersConfig());
		configs.add(new TestParametersConfig(CONFIG_OVERRIDES_KEY_VALUES));

		PhaseJobsConfig pjc = new PhaseJobsConfig("dummy", "dummyAlias", "", true, configs, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);


		List<Action> actions = pjc.getActions(mjb, TaskListener.NULL, projectB, false);

		// check 2 actions created
		assertEquals(2, actions.size());
		MultiJobParametersAction pa = getParametersAction(actions);
		HashMap<String,String> combinedlist = new HashMap<String,String>(DEFAULT_KEY_VALUES);
		combinedlist.putAll(CONFIG_OVERRIDES_KEY_VALUES);

		checkParameterMatch(combinedlist, pa);
	}

	private MultiJobBuild createTriggeringBuild(MultiJobParametersAction parametersAction) throws Exception {
				// set up the triggering build
		MultiJobProject projectA = new MultiJobProject(Hudson.getInstance(), "ssss");
		MultiJobBuild mjb = new MultiJobBuild(projectA);
		// add build ParametersAction
		if(parametersAction != null) {
			mjb.getActions().add(parametersAction);
		}
		return mjb;
	}

	private AbstractProject createTriggeredProject(Map<String,String> defaultParameters) throws Exception {
		// set up the project to be triggered
		FreeStyleProject projectB = createFreeStyleProject();
		if(defaultParameters != null) {
			List<ParameterDefinition> pds = new ArrayList<ParameterDefinition>();

			for(String name: defaultParameters.keySet()) {
				pds.add(new StringParameterDefinition(name, defaultParameters.get(name)));
			}

			ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(pds);
			projectB.addProperty(pdp);
		}
		return projectB;
	}

	private MultiJobParametersAction createParametersAction(Map<String,String> items) {
		List<ParameterValue> params = new ArrayList<ParameterValue>();
		if(items != null) {
			for(String name: items.keySet()) {
				params.add(new StringParameterValue(name, items.get(name)));
			}
		}
		return new MultiJobParametersAction(params);
	}

	private void checkParameterMatch(Map<String, String> combinedlist, MultiJobParametersAction pa) {
		assertTrue(pa != null);
		assertEquals(combinedlist.size(), pa.getParameters().size());
		for(String key : combinedlist.keySet()) {
			assertEquals(((StringParameterValue)pa.getParameter(key)).value, combinedlist.get(key));
		}
	}

	private MultiJobParametersAction getParametersAction(List<Action> actions) {
		MultiJobParametersAction pa =null;
		for (Action a :actions) {
			if(a instanceof MultiJobParametersAction) {
				pa = (MultiJobParametersAction)a;
			}
		}
		return pa;
	}
	/**
	 * Config item returning a cause action
	 */
	class TestCauseConfig extends AbstractBuildParameters {

		@Override
		public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
			return new CauseAction(new UserIdCause());
		}
	}
	/**
	 * Config item returning a ParametersAction
	 */
	class TestParametersConfig extends AbstractBuildParameters {
		private Map<String,String> items;

		public TestParametersConfig() {
			this.items = null;
		}
		public TestParametersConfig(Map<String,String> items) {
			this.items = items;
		}
		@Override
		public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
			return createParametersAction(items);
		}
	}
}
