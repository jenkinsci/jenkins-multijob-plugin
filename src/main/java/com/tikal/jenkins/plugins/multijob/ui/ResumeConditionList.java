package com.tikal.jenkins.plugins.multijob.ui;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Extension
public class ResumeConditionList extends UIObject {
    @Override
    public String getDescription() {
        return "Resume condition choices";
    }

    public ResumeCondition getCondition() {
        return null;
    }

    public DescriptorExtensionList<ResumeCondition, Descriptor<ResumeCondition>> getResumeDescriptors() {
        return Jenkins.getInstance().<ResumeCondition, Descriptor<ResumeCondition>>getDescriptorList(ResumeCondition.class);
    }

    public void doSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        ResumeCondition condition = req.bindJSON(ResumeCondition.class, (JSONObject) req.getSubmittedForm().get("resumeCondition"));
        rsp.setContentType("text/plain");
        new XStream2().toXML(condition, rsp.getWriter());
    }

    @Override
    public List<SourceFile> getSourceFiles() {
        List<SourceFile> list = new ArrayList<SourceFile>(super.getSourceFiles());
        list.add(new SourceFile("SkipResume/config.jelly"));
        list.add(new SourceFile("AlwaysResume/config.jelly"));
        list.add(new SourceFile("ExpressionResume/config.jelly"));
        return list;
    }

    @Extension
    public static final class DescriptorImpl extends UIObjectDescriptor {
    }

    public static abstract class ResumeCondition implements ExtensionPoint, Describable<ResumeCondition> {
        protected String description;
        protected boolean answer;

        protected ResumeCondition(String description, boolean answer) {
            this.description = description;
            this.answer = answer;
        }

        public Descriptor<ResumeCondition> getDescriptor() {
            return Jenkins.getInstance().getDescriptor(getClass());
        }
    }

    public static class ResumeConditionDescriptor extends Descriptor<ResumeCondition> {

        public ResumeConditionDescriptor(Class<? extends ResumeCondition> clazz) {
            super(clazz);
        }

        @Override
        public String getDisplayName() {
            return clazz.getSimpleName();
        }
    }

    public static class SkipResume extends ResumeCondition {

        @DataBoundConstructor
        public SkipResume() {
            super("Skip the phase if previous run was successful", true);
        }

        @Extension
        public static final ResumeConditionDescriptor DESCRIPTOR = new ResumeConditionDescriptor(SkipResume.class);
    }

    public static class AlwaysResume extends ResumeCondition {

        @DataBoundConstructor
        public AlwaysResume() {
            super("always run this phase during resume, rest of the phase conditions still applies", false);
        }

        @Extension
        public static final ResumeConditionDescriptor DESCRIPTOR = new ResumeConditionDescriptor(AlwaysResume.class);
    }

    public static class ExpressionResume extends ResumeCondition {

        private String expression;

        @DataBoundConstructor
        public ExpressionResume(String expression) {
            super("Skip phase expression", false);
            this.expression = expression;
        }

        @Extension
        public static final ResumeConditionDescriptor DESCRIPTOR = new ResumeConditionDescriptor(ExpressionResume.class);
    }
}
