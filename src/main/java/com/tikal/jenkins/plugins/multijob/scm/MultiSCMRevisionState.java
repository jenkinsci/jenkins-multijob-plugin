package com.tikal.jenkins.plugins.multijob.scm;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;

import java.util.HashMap;
import java.util.Map;

public class MultiSCMRevisionState extends SCMRevisionState {
    private final Map<String, SCMRevisionState> revisionStates;

    public MultiSCMRevisionState() {
        revisionStates = new HashMap<String, SCMRevisionState>();
    }

    public void add(@NonNull SCM scm, @NonNull FilePath ws, @Nullable AbstractBuild<?,?> build, SCMRevisionState scmState) {
        revisionStates.put(keyFor(scm, ws, build), scmState);
    }

    public SCMRevisionState get(@NonNull SCM scm, @NonNull FilePath ws, @Nullable AbstractBuild<?,?> build) {
        SCMRevisionState state = revisionStates.get(keyFor(scm, ws, build));
        // for backward compatibility with version 0.1, try to get the state using the class name as well
        if (state == null)
            state = revisionStates.get(scm.getClass().getName());
        return state;
    }

    private static String keyFor(@NonNull SCM scm, @NonNull FilePath ws, @Nullable AbstractBuild<?,?> build) { // JENKINS-12298
        StringBuilder b = new StringBuilder(scm.getType());
        for (FilePath root : scm.getModuleRoots(ws, build)) {
            b.append(root.getRemote().substring(ws.getRemote().length()));
        }
        return b.toString();
    }

}