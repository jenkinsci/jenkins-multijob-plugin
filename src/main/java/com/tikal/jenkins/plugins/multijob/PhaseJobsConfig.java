package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BooleanParameterDefinition;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.FileParameterValue;
import hudson.model.Hudson;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;
import hudson.plugins.parameterizedtrigger.FileBuildParameters;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import com.tikal.jenkins.plugins.multijob.scm.MultiJobScm;
public class PhaseJobsConfig implements Describable<PhaseJobsConfig> {

	private String jobName;
	private String jobProperties;
	private boolean currParams;
	private boolean exposedSCM;
	private boolean disableJob;
	private String parsingRulesPath;
	private int maxRetries;
	private boolean enableRetryStrategy;
	private boolean enableCondition;
	private boolean abortAllJob;
	private String condition;
	private List<AbstractBuildParameters> configs;
	private KillPhaseOnJobResultCondition killPhaseOnJobResultCondition = KillPhaseOnJobResultCondition.NEVER;
	private boolean buildOnlyIfSCMChanges = false;
	private boolean enableJobScript;
	private boolean isUseScriptFile;
	private String jobScript;
	private String scriptPath;
	private ResumeCondition resumeCondition = ResumeCondition.SKIP;
	private String resumeBindings;
	private String jobBindings;
	private boolean isUseResumeScriptFile;
	private String resumeScriptPath;
	private String resumeScriptText;
	private JSONObject resumeConditions;
	private boolean isJobScriptOnSlaveNode;
	private boolean isResumeScriptOnSlaveNode;
	private boolean isRunJobScriptOnSlave;
	private boolean isRunResumeScriptOnSlave;

	public boolean isBuildOnlyIfSCMChanges() {
		return this.buildOnlyIfSCMChanges;
	}

	public void setBuildOnlyIfSCMChanges(boolean triggerOnlyIfSCMChanges) {
		this.buildOnlyIfSCMChanges = triggerOnlyIfSCMChanges;
	}

	public void setParsingRulesPath(String parsingRulesPath) {
		this.parsingRulesPath = parsingRulesPath;
	}

	public String getParsingRulesPath() {
		return parsingRulesPath;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setEnableRetryStrategy(boolean enableRetryStrategy) {
		this.enableRetryStrategy = enableRetryStrategy;
	}

	public boolean getEnableRetryStrategy() {
		return enableRetryStrategy;
	}

	public void setEnableCondition(boolean enableCondition) {
		this.enableCondition = enableCondition;
	}

	public boolean getEnableCondition() {
		return enableCondition;
	}

	public void setAbortAllJob(boolean abortAllJob) {
		this.abortAllJob = abortAllJob;
	}

	public boolean getAbortAllJob() {
		return abortAllJob;
	}

	public boolean isDisableJob() {
		return disableJob;
	}

	public void setDisableJob(boolean disableJob) {
		this.disableJob = disableJob;
	}

	public KillPhaseOnJobResultCondition getKillPhaseOnJobResultCondition() {
		return killPhaseOnJobResultCondition;
	}

	public void setKillPhaseOnJobResultCondition(
			KillPhaseOnJobResultCondition killPhaseOnJobResultCondition) {
		this.killPhaseOnJobResultCondition = killPhaseOnJobResultCondition;
	}

	public boolean isExposedSCM() {
		return exposedSCM;
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

	public boolean getEnableJobScript() {
		return enableJobScript;
	}

	public void setEnableJobScript(boolean enableJobScript) {
		this.enableJobScript = enableJobScript;
	}

	public String getJobScript() {
		return jobScript;
	}

	public void setJobScript(String jobScript) {
		this.jobScript = jobScript;
	}

	public boolean isUseScriptFile() {
		return isUseScriptFile;
	}

	public void setUseScriptFile(boolean isUseScriptFile) {
		this.isUseScriptFile = isUseScriptFile;
	}

	public String getScriptPath() {
		return scriptPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public ResumeCondition getResumeCondition() {
		return resumeCondition;
	}

	public void setResumeCondition(ResumeCondition resumeCondition) {
		this.resumeCondition = resumeCondition;
	}

	public String getResumeBindings() {
		return resumeBindings;
	}

	public void setResumeBindings(String resumeBindings) {
		this.resumeBindings = resumeBindings;
	}

	public String getJobBindings() {
		return jobBindings;
	}

	public void setJobBindings(String jobBindings) {
		this.jobBindings = jobBindings;
	}

	public boolean isUseResumeScriptFile() {
		return isUseResumeScriptFile;
	}

	public void setUseResumeScriptFile(boolean isUseResumeScriptFile) {
		this.isUseResumeScriptFile = isUseResumeScriptFile;
	}

	public String getResumeScriptPath() {
		return resumeScriptPath;
	}

	public void setResumeScriptPath(String resumeScriptPath) {
		this.resumeScriptPath = resumeScriptPath;
	}

	public String getResumeScriptText() {
		return resumeScriptText;
	}

	public void setResumeScriptText(String resumeScriptText) {
		this.resumeScriptText = resumeScriptText;
	}

	public JSONObject getResumeConditions() {
		return resumeConditions;
	}

	public void setResumeConditions(JSONObject resumeConditions) {
		this.resumeConditions = resumeConditions;
	}

	public boolean isJobScriptOnSlaveNode() {
		return isJobScriptOnSlaveNode;
	}

	public void setJobScriptOnSlaveNode(boolean isJobScriptOnSlaveNode) {
		this.isJobScriptOnSlaveNode = isJobScriptOnSlaveNode;
	}

	public boolean isResumeScriptOnSlaveNode() {
		return isResumeScriptOnSlaveNode;
	}

	public void setResumeScriptOnSlaveNode(boolean isResumeScriptOnSlaveNode) {
		this.isResumeScriptOnSlaveNode = isResumeScriptOnSlaveNode;
	}

	public boolean isRunJobScriptOnSlave() {
		return isRunJobScriptOnSlave;
	}

	public void setRunJobScriptOnSlave(boolean isRunJobScriptOnSlave) {
		this.isRunJobScriptOnSlave = isRunJobScriptOnSlave;
	}

	public boolean isRunResumeScriptOnSlave() {
		return isRunResumeScriptOnSlave;
	}

	public void setRunResumeScriptOnSlave(boolean isRunResumeScriptOnSlave) {
		this.isRunResumeScriptOnSlave = isRunResumeScriptOnSlave;
	}

	public Descriptor<PhaseJobsConfig> getDescriptor() {
		return Hudson.getInstance().getDescriptorOrDie(getClass());
	}

	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@DataBoundConstructor
	public PhaseJobsConfig(String jobName, String jobProperties,
			boolean currParams, List<AbstractBuildParameters> configs,
			KillPhaseOnJobResultCondition killPhaseOnJobResultCondition,
			boolean disableJob, boolean enableRetryStrategy,
			String parsingRulesPath, int maxRetries, boolean enableCondition,
			boolean abortAllJob, String condition, boolean buildOnlyIfSCMChanges,
			boolean enableJobScript, ScriptLocation scriptLocation,
			String jobBindings, String resumeBindings,
			JSONObject resumeConditions,
			ResumeCondition resumeCondition, String resumeScriptPath, String resumeScriptText,
			boolean isUseResumeScriptFile,
			boolean isJobScriptOnSlaveNode, boolean isResumeScriptOnSlaveNode,
			boolean isRunResumeScriptOnSlave, boolean isRunJobScriptOnSlave) {
		this.jobName = jobName;
		this.jobProperties = jobProperties;
		this.currParams = currParams;
		this.killPhaseOnJobResultCondition = killPhaseOnJobResultCondition;
		this.disableJob = disableJob;
		this.configs = Util.fixNull(configs);
		this.enableRetryStrategy = enableRetryStrategy;
		this.maxRetries = maxRetries;
		if (this.maxRetries < 0) {
			this.maxRetries = 0;
		}
		this.parsingRulesPath = Util.fixNull(parsingRulesPath);
		this.enableCondition = enableCondition;
		this.abortAllJob = abortAllJob;
		this.condition = Util.fixNull(condition);
		this.buildOnlyIfSCMChanges = buildOnlyIfSCMChanges;
		this.enableJobScript = enableJobScript;
		if (enableJobScript && null != scriptLocation) {
			this.jobScript = scriptLocation.getScriptText();
			this.scriptPath = scriptLocation.getScriptPath();
			this.isUseScriptFile = scriptLocation.isUseFile();
			this.isJobScriptOnSlaveNode = scriptLocation.isScriptOnSlave();
		} else {
			this.jobScript = "";
			this.scriptPath = "";
			this.isUseScriptFile = false;
			this.isJobScriptOnSlaveNode = false;
		}
		this.jobBindings = Util.fixNull(jobBindings);

		if (null != resumeConditions) {
			this.resumeCondition = resumeConditions.has("resumeCondition") ?
					ResumeCondition.valueOf(resumeConditions.getString("resumeCondition")) : ResumeCondition.SKIP;
			if (this.resumeCondition.equals(ResumeCondition.EXPRESSION)) {
				JSONObject resLoc = resumeConditions.getJSONObject("resumeScriptLocation");
				String value = resLoc.getString("value");
				this.isUseResumeScriptFile = null == value || value.trim().isEmpty() ? false : Boolean.parseBoolean(value);
				this.resumeScriptPath = resLoc.has("scriptPath") ? Util.fixNull(resLoc.getString("scriptPath")) : "";
				this.resumeScriptText = resLoc.has("scriptText") ? Util.fixNull(resLoc.getString("scriptText")) : "";
				this.resumeBindings = resumeConditions.has("resumeBindings") ?
						Util.fixNull(resumeConditions.getString("resumeBindings")) : "";
			}
		} else {
			this.resumeCondition = null != resumeCondition ? resumeCondition : ResumeCondition.SKIP;
			this.resumeScriptPath = Util.fixNull(resumeScriptPath);
			this.resumeScriptText = Util.fixNull(resumeScriptText);
			this.isUseResumeScriptFile = isUseResumeScriptFile;
			this.resumeBindings = Util.fixNull(resumeBindings);
		}
		this.isRunResumeScriptOnSlave = isRunResumeScriptOnSlave;
		this.isRunJobScriptOnSlave = isRunJobScriptOnSlave;
	}

	public List<AbstractBuildParameters> getConfigs() {
		return configs;
	}

	@Extension(optional = true)
	public static class DescriptorImpl extends Descriptor<PhaseJobsConfig> {
		private ParserRuleFile[] parsingRulesGlobal = new ParserRuleFile[0];

		public DescriptorImpl() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "Phase Jobs Config";
		}

		public List<Descriptor<AbstractBuildParameters>> getBuilderConfigDescriptors() {
			return Hudson
					.getInstance()
					.<AbstractBuildParameters, Descriptor<AbstractBuildParameters>> getDescriptorList(
							AbstractBuildParameters.class);
		}

		public AutoCompletionCandidates doAutoCompleteJobName(
				@QueryParameter String value) {
			AutoCompletionCandidates c = new AutoCompletionCandidates();
			for (String localJobName : Hudson.getInstance().getJobNames()) {
				if (localJobName.toLowerCase().startsWith(value.toLowerCase()))
					c.add(localJobName);
			}
			return c;
		}

		private void savePhaseJobConfigParameters(String localJobName) {
			AbstractProject project = ((AbstractProject) Jenkins.getInstance()
					.getItemByFullName(localJobName));
			List<ParameterDefinition> parameterDefinitions = getParameterDefinition(project);
			StringBuilder sb = new StringBuilder();
			// ArrayList<ModuleLocation> scmLocation = null;
			for (ParameterDefinition pdef : parameterDefinitions) {
				String paramValue = null;
				if (pdef instanceof StringParameterDefinition) {
					StringParameterDefinition stringParameterDefinition = (StringParameterDefinition) pdef;
					paramValue = stringParameterDefinition
							.getDefaultParameterValue().value;
				} else if (pdef instanceof BooleanParameterDefinition) {
					BooleanParameterDefinition booleanParameterDefinition = (BooleanParameterDefinition) pdef;
					paramValue = String.valueOf(booleanParameterDefinition
							.getDefaultParameterValue().value);
				}
				sb.append(pdef.getName()).append("=").append(paramValue)
						.append("\n");
			}

			AbstractProject item = getCurrentJob();
			if (item instanceof MultiJobProject) {
				MultiJobProject parentProject = (MultiJobProject) item;
				List<Builder> builders = parentProject.getBuilders();
				if (builders != null) {
					for (Builder builder : builders) {
						if (builder instanceof MultiJobBuilder) {
							MultiJobBuilder multiJobBuilder = (MultiJobBuilder) builder;
							List<PhaseJobsConfig> phaseJobs = multiJobBuilder
									.getPhaseJobs();
							for (PhaseJobsConfig phaseJob : phaseJobs) {
								if (phaseJob.getJobName().equals(localJobName)) {
									phaseJob.setJobProperties(sb.toString());
									save();
								}
							}
						}
					}

				}
			}
		}

		private static String getCurrentJobName() {
			String path = Descriptor.getCurrentDescriptorByNameUrl();
			String[] parts = path.split("/");
			StringBuilder builder = new StringBuilder();
			for (int i = 2; i < parts.length; i += 2) {
				if (i > 2)
					builder.append('/');
				builder.append(parts[i]);
			}
			return builder.toString();
		}

		private static AbstractProject getCurrentJob() {
			String jobName = getCurrentJobName();
			return (AbstractProject) Jenkins.getInstance().getItemByFullName(
					jobName);
		}

		public List<ParameterDefinition> getParameterDefinition(
				AbstractProject project) {
			List<ParameterDefinition> list = new ArrayList<ParameterDefinition>();
			Map<JobPropertyDescriptor, JobProperty> map = project
					.getProperties();
			for (Map.Entry<JobPropertyDescriptor, JobProperty> entry : map
					.entrySet()) {
				JobProperty property = entry.getValue();
				if (property instanceof ParametersDefinitionProperty) {
					ParametersDefinitionProperty pdp = (ParametersDefinitionProperty) property;
					for (ParameterDefinition parameterDefinition : pdp
							.getParameterDefinitions()) {
						if (parameterDefinition instanceof StringParameterDefinition
								|| parameterDefinition instanceof BooleanParameterDefinition
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

		public ParserRuleFile[] getParsingRulesGlobal() {
			return parsingRulesGlobal;
		}

		@Override
		public boolean configure(final StaplerRequest req, final JSONObject json)
				throws FormException {
			parsingRulesGlobal = req.bindParametersToList(ParserRuleFile.class,
					"jenkins-multijob-plugin.").toArray(new ParserRuleFile[0]);
			save();
			return true;
		}

	}

	public List<ParameterValue> getJobParameters(AbstractBuild<?, ?> build,
			TaskListener listener) {
		ParametersAction action = build.getAction(ParametersAction.class);
		List<ParameterValue> values = new ArrayList<ParameterValue>(action
				.getParameters().size());
		if (action != null) {
			for (ParameterValue value : action.getParameters())
				// FileParameterValue is currently not reusable, so omit these:
				if (!(value instanceof FileParameterValue))
					values.add(value);
		}

		return values;

	}

	private static ParametersAction mergeParameters(ParametersAction base,
			ParametersAction overlay) {
		LinkedHashMap<String, ParameterValue> params = new LinkedHashMap<String, ParameterValue>();
		for (ParameterValue param : base.getParameters())
			if (param != null)
				params.put(param.getName(), param);
		for (ParameterValue param : overlay.getParameters())
			params.put(param.getName(), param);
		return new ParametersAction(params.values().toArray(
				new ParameterValue[params.size()]));
	}

	/**
	 * Create a list of actions to pass to the triggered build of project.
	 *
	 * This will create a single ParametersAction which will use the defaults
	 * from the project being triggered and override these, With the current
	 * parameters defined in this build. if configured. With any matching items
	 * defined in the different configs, e.g. predefined parameters.
	 *
	 * @param build
	 *            build that is triggering project
	 * @param listener
	 * @param project
	 *            Project that is being triggered
	 * @param isCurrentInclude
	 *            Include parameters from the current build.
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public List<Action> getActions(AbstractBuild build, TaskListener listener,
			AbstractProject project, boolean isCurrentInclude)
			throws IOException, InterruptedException {
		List<Action> actions = new ArrayList<Action>();
		ParametersAction params = null;
		LinkedList<ParameterValue> paramsValuesList = new LinkedList<ParameterValue>();

		List originalActions = project.getActions();

		// Check to see if the triggered project has Parameters defined.
		ParametersDefinitionProperty parameters = null;
		for (Object object : originalActions) {
			if (object instanceof hudson.model.ParametersDefinitionProperty)
				parameters = (ParametersDefinitionProperty) object;
		}
		// Get and add ParametersAction for default parameters values
		// if triggered project is Parameterized.
		// Values will get overridden later as required
		if (parameters != null) {
			for (ParameterDefinition parameterdef : parameters
					.getParameterDefinitions()) {
				if (parameterdef.getDefaultParameterValue() != null)
					paramsValuesList.add(parameterdef
							.getDefaultParameterValue());
			}
			params = new ParametersAction(
					paramsValuesList
							.toArray(new ParameterValue[paramsValuesList.size()]));

		}

		// Merge current parameters with the defaults from the triggered job.
		// Current parameters override the defaluts.
		if (isCurrentInclude) {
			ParametersAction defaultParameters = build
					.getAction(ParametersAction.class);

			if (params != null && defaultParameters != null) {
				params = mergeParameters(params, defaultParameters);
			} else if (params == null) {
				params = defaultParameters;
			}
		}
		// Backward compatibility
		// get actions from configs merge ParametersActions if needed.
		if (configs != null) {
			for (AbstractBuildParameters config : configs) {
				Action a;
				try {
					a = config.getAction(build, listener);
					if (a instanceof ParametersAction) {
						params = params == null ? (ParametersAction) a
								: mergeParameters(params, (ParametersAction) a);
					} else if (a != null) {
						actions.add(a);
					}
				} catch (DontTriggerException e) {
					// don't trigger on this configuration
					listener.getLogger().println(
							"[multiJob] DontTriggerException: " + e);
				}
			}
		}

		if (params != null)
			actions.add(params);

		return actions;
	}

	public boolean hasProperties() {
		return this.jobProperties != null && !this.jobProperties.isEmpty();
	}

	// compatibility with earlier plugins
	public Object readResolve() {
		if (hasProperties()) {
			AbstractBuildParameters buildParameters = new PredefinedBuildParameters(
					jobProperties);
			if (configs == null)
				configs = new ArrayList<AbstractBuildParameters>();
			configs.add(buildParameters);
		}

		List<AbstractBuildParameters> oldParams = new ArrayList<AbstractBuildParameters>();
		if (configs != null && configs.size() > 0) {
			Iterator parametersIterator = configs.iterator();
			while (parametersIterator.hasNext()) {
				Object param = parametersIterator.next();
				if (param instanceof com.tikal.jenkins.plugins.multijob.PredefinedBuildParameters) {
					com.tikal.jenkins.plugins.multijob.PredefinedBuildParameters previosStringParam = (com.tikal.jenkins.plugins.multijob.PredefinedBuildParameters) param;
					parametersIterator.remove();
					oldParams.add(new PredefinedBuildParameters(
							previosStringParam.getJobProperties()));
				} else if (param instanceof com.tikal.jenkins.plugins.multijob.FileBuildParameters) {
					com.tikal.jenkins.plugins.multijob.FileBuildParameters previosFileParam = (com.tikal.jenkins.plugins.multijob.FileBuildParameters) param;
					parametersIterator.remove();
					oldParams.add(new FileBuildParameters(previosFileParam
							.getPropertiesFile()));
				}
			}
			configs.addAll(oldParams);
		}
		return this;
	}

	public static enum KillPhaseOnJobResultCondition {
		FAILURE("Failure (stop the phase execution if the job is failed)") {
			@Override
			public boolean isKillPhase(Result result) {
				return result.isWorseOrEqualTo(Result.FAILURE);
			}
		},
		NEVER("Never (ignore the job result and continue the phase execution)") {
			@Override
			public boolean isKillPhase(Result result) {
				return result.equals(Result.ABORTED) ? true : false;
			}
		},
		UNSTABLE("Unstable (stop the phase execution if the job is unstable)") {
			@Override
			public boolean isKillPhase(Result result) {
				return result.isWorseOrEqualTo(Result.UNSTABLE);
			}
		};

		abstract public boolean isKillPhase(Result result);

		private KillPhaseOnJobResultCondition(String label) {
			this.label = label;
		}

		final private String label;

		public String getLabel() {
			return label;
		}
	}

	public enum ResumeCondition {

		SKIP("Skip the job is previous run was successful", "SKIP") {
			@Override
			public boolean isStart() {
				return false;
			}

			@Override
			public boolean isEvaluate() {
				return false;
			}
		},
		ALWAYS("Always run this job during resume", "ALWAYS") {
			@Override
			public boolean isStart() {
				return true;
			}

			@Override
			public boolean isEvaluate() {
				return false;
			}
		},
		EXPRESSION("Skip phase expression", "EXPRESSION") {
			@Override
			public boolean isStart() {
				return false;
			}

			@Override
			public boolean isEvaluate() {
				return true;
			}
		};

		abstract public boolean isStart();

		abstract public boolean isEvaluate();

		final private String label;
		final private String value;

		public String getLabel() {
			return label;
		}

		public String getValue() {
			return value;
		}

		ResumeCondition(String label, String value) {
			this.label = label;
			this.value = value;
		}



		public List<ResumeCondition> all() {
			List<ResumeCondition> list = new ArrayList<ResumeCondition>();
			Collections.addAll(list, getDeclaringClass().getEnumConstants());
			return list;
		}
	}
}
