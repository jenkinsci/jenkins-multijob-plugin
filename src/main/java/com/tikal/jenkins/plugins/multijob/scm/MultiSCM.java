package com.tikal.jenkins.plugins.multijob.scm;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.*;
import hudson.scm.PollingResult.Change;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class MultiSCM extends SCM implements Saveable {

//    private DescribableList<SCM,Descriptor<SCM>> scms =
//            new DescribableList<SCM,Descriptor<SCM>>(this);

    private Map<String,SCM> scms = new HashMap<String, SCM>();

    private SCM parentProjectSCM;

    @DataBoundConstructor
    public MultiSCM(SCM parentProjectSCM) throws IOException {
        if(parentProjectSCM != null) {}
        //    scms.put(parentProjectSCM);
    }

    /*@DataBoundConstructor
    public MultiSCM(List<SCM> scmList) throws IOException {
        if(scmList != null)
            scms.addAll(scmList);
    } */

    @Exported
    public SCM getParentProjectSCM() {
        return parentProjectSCM;
    }

    @Exported
    public List<SCM> getConfiguredSCMs() {
        return new ArrayList<SCM>(scms.values());
    }

    public void addScm(String projectName, SCM scm) throws IOException{
        scms.put(projectName, scm);
    }

    public void removeScm(SCM scm) throws IOException{
        scms.values().remove(scm);//scms.remove(scm);
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
                                                   Launcher launcher, TaskListener listener) throws IOException,
            InterruptedException {

        MultiSCMRevisionState revisionStates = new MultiSCMRevisionState();

        for(SCM scm : scms.values()) {
            SCMRevisionState scmState = scm.calcRevisionsFromBuild(build, launcher, listener);
            revisionStates.add(scm, build.getWorkspace(), build, scmState);
        }

        return revisionStates;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?,?> build, Map<String, String> env) {
        for(SCM scm : scms.values()) {
            try {
                scm.buildEnvVars(build, env);
            }
            catch(NullPointerException npe)
            {}
        }
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(
            AbstractProject<?, ?> project, Launcher launcher,
            FilePath workspace, TaskListener listener, SCMRevisionState baseline)
            throws IOException, InterruptedException {

        MultiSCMRevisionState baselineStates = baseline instanceof MultiSCMRevisionState ? (MultiSCMRevisionState) baseline : null;
        MultiSCMRevisionState currentStates = new MultiSCMRevisionState();

        Change overallChange = Change.NONE;

        for(SCM scm : scms.values()) {
            SCMRevisionState scmBaseline = baselineStates != null ? baselineStates.get(scm, workspace, null) : null;
            PollingResult scmResult = scm.poll(project, launcher, workspace, listener, scmBaseline != null ? scmBaseline : SCMRevisionState.NONE);
            currentStates.add(scm, workspace, null, scmResult.remote);
            if(scmResult.change.compareTo(overallChange) > 0)
                overallChange = scmResult.change;
        }
        return new PollingResult(baselineStates, currentStates, overallChange);
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher,
                            FilePath workspace, BuildListener listener, File changelogFile)
            throws IOException, InterruptedException {

        MultiSCMRevisionState revisionState = new MultiSCMRevisionState();
        build.addAction(revisionState);

        HashSet<Object> scmActions = new HashSet<Object>();

        FileOutputStream logStream = new FileOutputStream(changelogFile);
        OutputStreamWriter logWriter = new OutputStreamWriter(logStream);
        logWriter.write(String.format("<%s>\n", MultiSCMChangeLogParser.ROOT_XML_TAG));

        boolean checkoutOK = true;
        for(SCM scm : scms.values()) {
            String changeLogPath = changelogFile.getPath() + ".temp";
            File subChangeLog = new File(changeLogPath);
            checkoutOK = scm.checkout(build, launcher, workspace, listener, subChangeLog) && checkoutOK;

            List<Action> actions = build.getActions();
            for(Action a : actions) {
                if(!scmActions.contains(a) && a instanceof SCMRevisionState) {
                    scmActions.add(a);
                    revisionState.add(scm, workspace, build, (SCMRevisionState) a);
                }
            }
            if (subChangeLog.exists()) {
                String subLogText = FileUtils.readFileToString(subChangeLog);
                logWriter.write(String.format("<%s scm=\"%s\">\n<![CDATA[%s]]>\n</%s>\n",
                        MultiSCMChangeLogParser.SUB_LOG_TAG,
                        scm.getType(),
                        subLogText,
                        MultiSCMChangeLogParser.SUB_LOG_TAG));

                subChangeLog.delete();
            }
        }
        logWriter.write(String.format("</%s>\n", MultiSCMChangeLogParser.ROOT_XML_TAG));
        logWriter.close();

        return checkoutOK;
    }

    @Override
    public FilePath[] getModuleRoots(FilePath workspace, AbstractBuild build) {
        ArrayList<FilePath> paths = new ArrayList<FilePath>();
        for(SCM scm : scms.values()) {
            FilePath[] p = scm.getModuleRoots(workspace, build);
            for(FilePath p2 : p)
                paths.add(p2);
        }
        return paths.toArray(new FilePath[paths.size()]);
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new MultiSCMChangeLogParser(new ArrayList(scms.values()));
    }

    public void save() throws IOException {
        // TODO Auto-generated method stub

    }

    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends SCMDescriptor<MultiSCM> {

        public DescriptorImpl() {
            super(MultiSCMRepositoryBrowser.class);
            // TODO Auto-generated constructor stub
        }

        public List<SCMDescriptor<?>> getApplicableSCMs(AbstractProject<?, ?> project) {
            List<SCMDescriptor<?>> scms = new ArrayList<SCMDescriptor<?>>();

            for(SCMDescriptor<?> scm : SCM._for(project)) {
                // Filter MultiSCM itself from the list of choices.
                // Theoretically it might work, but I see no practical reason to allow
                // nested MultiSCM configurations.
                if(!(scm instanceof DescriptorImpl) && !(scm instanceof NullSCM.DescriptorImpl))
                    scms.add(scm);
            }

            return scms;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Multiple SCMs";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return super.newInstance(req, formData);
        }
    }
}

