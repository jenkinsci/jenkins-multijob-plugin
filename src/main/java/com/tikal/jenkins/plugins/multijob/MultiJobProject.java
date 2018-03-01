package com.tikal.jenkins.plugins.multijob;

import java.util.List;
import java.io.IOException;
import javax.servlet.ServletException;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import jenkins.model.Jenkins;
import hudson.Extension;
import hudson.model.DependencyGraph;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.util.AlternativeUiTextProvider;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.*;

import com.tikal.jenkins.plugins.multijob.views.MultiJobView;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class MultiJobProject extends Project<MultiJobProject, MultiJobBuild>
		implements TopLevelItem {

        private volatile boolean pollSubjobs = false;
        private volatile String resumeEnvVars = null;

	@SuppressWarnings("rawtypes")
	private MultiJobProject(ItemGroup parent, String name) {
		super(parent, name);
	}

	public MultiJobProject(Hudson parent, String name) {
		super(parent, name);
	}

	@Override
	protected Class<MultiJobBuild> getBuildClass() {
		return MultiJobBuild.class;
	}

	@Override
	public String getPronoun() {
		return AlternativeUiTextProvider.get(PRONOUN, this, getDescriptor().getDisplayName());
	}

	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension(ordinal = 1000)
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends AbstractProjectDescriptor {
		public String getDisplayName() {
			return "MultiJob Project";
		}

		@SuppressWarnings("rawtypes")
		public MultiJobProject newInstance(ItemGroup itemGroup, String name) {
			return new MultiJobProject(itemGroup, name);
		}
	}

	@Override
	protected void buildDependencyGraph(DependencyGraph graph) {
		super.buildDependencyGraph(graph);
	}

	public boolean isTopMost() {
		return getUpstreamProjects().size() == 0;
	}

	public MultiJobView getView() {
		return new MultiJobView("");
	}

	public String getRootUrl() {
		return Jenkins.getInstance().getRootUrl();
	}

        @Override
        public PollingResult poll(TaskListener listener) {
            //Preserve default behavior unless specified otherwise
            if (!getPollSubjobs()) {
                return super.poll(listener);
            }

            PollingResult result = super.poll(listener);
            //If multijob has changes, save the effort of checking children
            if (result.hasChanges()) {
                return result;
            }
            List<AbstractProject> downProjs = getDownstreamProjects();
            PollingResult tmpResult = new PollingResult(PollingResult.Change.NONE);
            //return when we get changes to save resources
            //If we don't get changes, return the most significant result
            for (AbstractProject downProj : downProjs) {
                tmpResult = downProj.poll(listener);
                if (result.change.ordinal() < tmpResult.change.ordinal()) {
                    result = tmpResult;
                    if (result.hasChanges()) {
                        return result;
                    }
                }
            }
            return result;
        }

        public boolean getPollSubjobs() {
            return pollSubjobs;
        }

        public void setPollSubjobs(boolean poll) {
            pollSubjobs = poll;
        }

        public String getResumeEnvVars() {
			return resumeEnvVars;
		}

        public void setResumeEnvVars(String resumeEnvVars) {
			this.resumeEnvVars = resumeEnvVars;
		}

        public boolean getCheckResumeEnvVars() {
        	return !StringUtils.isBlank(resumeEnvVars);
        }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req, rsp);
        JSONObject json = req.getSubmittedForm();
        String k = "multijob";
        if (json.has(k)) {
            json = json.getJSONObject(k);
            k = "pollSubjobs";
            if (json.has(k)) {
                setPollSubjobs(json.optBoolean(k));
            }
            String resumeEnvVars = null;
            k = "resumeEnvVars";
            if (json.has(k)) {
            	json = json.getJSONObject(k);
                if (json.has(k)) {
                	resumeEnvVars = json.getString(k);
                }
            }
            setResumeEnvVars(resumeEnvVars);
        }
    }

    // TODO as in https://github.com/jenkinsci/maven-plugin/pull/109:
    @Override
    public boolean scheduleBuild(Cause c) {
        return super.scheduleBuild(c);
    }
    @Override
    public boolean scheduleBuild(int quietPeriod, Cause c) {
        return super.scheduleBuild(quietPeriod, c);
    }
    @Override
    public boolean scheduleBuild(int quietPeriod) {
        return super.scheduleBuild(quietPeriod);
    }
    @Override
    public boolean scheduleBuild() {
        return super.scheduleBuild();
    }

}
