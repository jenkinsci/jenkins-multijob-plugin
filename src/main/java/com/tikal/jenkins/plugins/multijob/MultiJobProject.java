package com.tikal.jenkins.plugins.multijob;

import java.util.List;
import java.io.IOException;
import javax.servlet.ServletException;

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
import hudson.tasks.test.AbstractTestResultAction;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class MultiJobProject extends Project<MultiJobProject, MultiJobBuild>
		implements TopLevelItem {

    private volatile boolean pollSubjobs = false;
    private volatile boolean disableResumeBuild = false;

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

    public boolean getDisableResumeBuild() {
        return this.disableResumeBuild;
    }

    public void setDisableResumeBuild(boolean disableResumeBuild) {
        this.disableResumeBuild = disableResumeBuild;
    }

    public AbstractTestResultAction<?> getTestResultAction() {
        MultiJobBuild b = getLastCompletedBuild();
        return b != null ? b.getAction(AbstractTestResultAction.class) : null;
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
                setPollSubjobs(json.getBoolean(k));
            }
            
            k = "disableResumeBuild";
            if (json.has(k)) {
                setDisableResumeBuild(json.getBoolean(k));
            }
        }
    }
}