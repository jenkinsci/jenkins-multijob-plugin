package com.tikal.jenkins.plugins.multijob;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.Run;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;

@ExportedBean(defaultVisibility = 999)
public class MultiJobBuild extends Build<MultiJobProject, MultiJobBuild> {

    private List<SubBuild> subBuilds;
    private MultiJobChangeLogSet changeSets = new MultiJobChangeLogSet(this);
    private Map<String, SubBuild> subBuildsMap = new HashMap<String, SubBuild>();
    private MultiJobTestResults multiJobTestResults;
    

    public MultiJobBuild(MultiJobProject project) throws IOException {
        super(project);
    }

    @Override
    public ChangeLogSet<? extends Entry> getChangeSet() {
        return super.getChangeSet();
    }

    public void addChangeLogSet(ChangeLogSet<? extends Entry> changeLogSet) {
        if (changeLogSet != null) {
            this.changeSets.addChangeLogSet(changeLogSet);
        }
    }

    public MultiJobBuild(MultiJobProject project, File buildDir)
            throws IOException {
        super(project, buildDir);
    }

    @Override
    public synchronized void doStop(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        super.doStop(req, rsp);
    }

    @Override
    public void addAction(Action a) {
        super.addAction(a);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        execute(new MultiJobRunnerImpl());
    }

    public List<SubBuild> getBuilders() {
        MultiJobBuild multiJobBuild = getParent().getNearestBuild(getNumber());
        return multiJobBuild.getSubBuilds();
    }

    public String getBuildParams(SubBuild subBuild) {
        try {
            AbstractProject project = (AbstractProject) Jenkins.getInstance()
            		.getItem(subBuild.getJobName(), this.getParent(), AbstractProject.class);
            Run build = project.getBuildByNumber(subBuild.getBuildNumber());
            ParametersAction action = build.getAction(ParametersAction.class);
            List<ParameterValue> parameters = action.getParameters();
            StringBuffer buffer = new StringBuffer();
            for (ParameterValue parameterValue : parameters) {
                StringParameterValue stringParameter;
                try {
                    stringParameter = ((StringParameterValue) parameterValue);
                } catch (Exception e) {
                    continue;
                }
                String value = (String) stringParameter.getValue();
                String name = stringParameter.getName();
                buffer.append("<input type='text' size='15' value='")
                        .append(name)
                        .append("' readonly/>")
                        .append("&nbsp;")
                        .append("<input type='text' size='35' value='")
                        .append(value)
                        .append("'/ readonly>")
                        .append("</br>");
            }
            return buffer.toString();
        } catch (Exception e) {
            return "Failed to retrieve build parameters.";
        }
    }

    public void addSubBuild(SubBuild subBuild) {
        String key = subBuild.getPhaseName().concat(subBuild.getJobName())
                .concat(String.valueOf(subBuild.getBuildNumber()));
        if (subBuildsMap.containsKey(key)) {
            SubBuild e = subBuildsMap.get(key);
            Collections.replaceAll(getSubBuilds(), e, subBuild);
        } else {
            getSubBuilds().add(subBuild);
        }
        subBuildsMap.put(key, subBuild);
    }

    @Exported
    public List<SubBuild> getSubBuilds() {
        if (subBuilds == null)
            subBuilds = new CopyOnWriteArrayList<SubBuild>();
        return subBuilds;
    }
    
    public MultiJobTestResults getMultiJobTestResults() {
        return multiJobTestResults;
    }
    
    public void addTestsResult() {
        multiJobTestResults = new MultiJobTestResults();
        this.addAction(multiJobTestResults);
    }

    protected class MultiJobRunnerImpl extends
            Build<MultiJobProject, MultiJobBuild>.BuildExecution {
        @Override
        public Result run(BuildListener listener) throws Exception {
            Result result = super.run(listener);
            if (isAborted()) {
                result = Result.ABORTED;
            } else if (isNotBuilt()) {
                result = Result.NOT_BUILT;
            } else if (isFailure()) {
                result = Result.FAILURE;
            } else if (isUnstable()) {
                result = Result.UNSTABLE;
            }

            if (!Result.SUCCESS.equals(result)) {
                MultiJobResumeBuild action = new MultiJobResumeBuild(super.getBuild());
                super.getBuild().addAction(action);
            }

            return result;
        }

        private boolean isAborted() {
            return evaluateResult(Result.NOT_BUILT);
        }

        private boolean isNotBuilt() {
            return evaluateResult(Result.FAILURE);
        }

        private boolean isFailure() {
            return evaluateResult(Result.UNSTABLE);
        }

        private boolean isUnstable() {
            return evaluateResult(Result.SUCCESS);
        }

        private boolean evaluateResult(Result result) {
            List<SubBuild> builders = getBuilders();
            for (SubBuild subBuild : builders) {
                if (!subBuild.isRetry() && !subBuild.isAbort()) {
                    Result buildResult = subBuild.getResult();
                    if (buildResult != null && buildResult.isWorseThan(result)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @ExportedBean(defaultVisibility = 999)
    public static class SubBuild {

        private final String parentJobName;
        private final int parentBuildNumber;
        private final String jobName;
        private final String jobAlias;
        private final int buildNumber;
        private final String phaseName;
        private final Result result;
        private final String icon;
        private final String duration;
        private final String url;
        private final boolean retry;
        private final boolean aborted;
        private String buildID;

        public SubBuild(String parentJobName, int parentBuildNumber,
                String jobName, String jobAlias, int buildNumber, String phaseName,
                Result result, String icon, String duration, String url,
                Run<?, ?> build) {
            this.parentJobName = parentJobName;
            this.parentBuildNumber = parentBuildNumber;
            this.jobName = jobName;
            this.jobAlias = jobAlias;
            this.buildNumber = buildNumber;
            this.phaseName = phaseName;
            this.result = result;
            this.icon = icon;
            this.duration = duration;
            this.url = url;
            this.retry = false;
            this.aborted = false;
            buildID = build.getExternalizableId();
        }

        public SubBuild(String parentJobName, int parentBuildNumber,
                String jobName, String jobAlias, int buildNumber, String phaseName,
                Result result, String icon, String duration, String url,
                boolean retry, boolean aborted, Run<?, ?> build) {
            this.parentJobName = parentJobName;
            this.parentBuildNumber = parentBuildNumber;
            this.jobName = jobName;
            this.jobAlias = jobAlias;
            this.buildNumber = buildNumber;
            this.phaseName = phaseName;
            this.result = result;
            this.icon = icon;
            this.duration = duration;
            this.url = url;
            this.retry = retry;
            this.aborted = aborted;
            buildID = build.getExternalizableId();
        }

        @Exported
        public String getDuration() {
            return duration;
        }

        @Exported
        public boolean isRetry() {
            return retry;
        }


        @Exported
        public boolean isAbort() {
            return aborted;
        }

        @Exported
        public String getIcon() {
            return icon;
        }

        @Exported
        public String getUrl() {
            return url;
        }

        @Exported
        public String getPhaseName() {
            return phaseName;
        }

        @Exported
        public String getParentJobName() {
            return parentJobName;
        }

        @Exported
        public int getParentBuildNumber() {
            return parentBuildNumber;
        }

        @Exported
        public String getJobName() {
            return jobName;
        }

        @Exported
        public String getJobAlias() {
            if (jobAlias == null) {
                return "";
            }

            return jobAlias;
        }

        @Exported
        public int getBuildNumber() {
            return buildNumber;
        }

        @Exported
        public Result getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "SubBuild [parentJobName=" + parentJobName
                    + ", parentBuildNumber=" + parentBuildNumber + ", jobName="
                    + jobName + ", jobAlias=" + jobAlias
                    + ", buildNumber=" + buildNumber + "]";
        }

		@Exported
        @CheckForNull
		public Run<?,?> getBuild() {
            if (buildID != null) {
                Run<?, ?> build = Run.fromExternalizableId(buildID);
                if (build instanceof Run) {
                    return (Run) build;
                }
            } // else null if loaded from historical data prior to JENKINS-49328
			return null;
		}

		@Exported
		public boolean isMultiJobBuild() {
            if (buildID != null) {
                Run<?, ?> build = Run.fromExternalizableId(buildID);
                if (build instanceof MultiJobBuild) {
                    return true;
                }
            }
            return false;
        }
    }
}
