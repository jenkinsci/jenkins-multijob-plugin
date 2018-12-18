package com.tikal.jenkins.plugins.multijob.views;

import hudson.model.BallColor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("rawtypes")
public class PhaseWrapper extends AbstractWrapper {

    final String phaseName;

    final boolean isConditional;

    public PhaseWrapper(Job project, int nestLevel, String phaseName, boolean isConditional) {
        super(project, nestLevel);
        this.phaseName = phaseName;
        this.isConditional = isConditional;
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends Job> getAllJobs() {
        return Collections.EMPTY_LIST;
    }

    public String getName() {
        return phaseName;
    }

    public String getFullName() {
        return phaseName;
    }

    public String getDisplayName() {
        return phaseName;
    }

    public String getFullDisplayName() {
        return phaseName;
    }

    public boolean isConditional() {
        return isConditional;
    }

    public BallColor getIconColor() {
        Run worseBuild = null;
        for (BuildState buildState : childrenBuildState) {
            Job project = (Job) Jenkins.getInstance()
                        .getItemByFullName(buildState.getJobName());
            if (project == null)
                continue;

            Run build = (Run) project
                    .getBuildByNumber(buildState.getLastBuildNumber());
            if (build == null)
                continue;

            if (worseBuild == null) {
                worseBuild = build;
            } else {
                if (build.getResult().isWorseThan(worseBuild.getResult())) {
                    worseBuild = build;
                }
            }
        }
        if (worseBuild != null) {
            return worseBuild.getIconColor();
        }

        return BallColor.NOTBUILT;
    }

    public String getCss() {
        StringBuilder builder = new StringBuilder();
        builder.append("padding-left:");
        builder.append(String.valueOf((nestLevel + 1) * 20));
        builder.append("px;");
        builder.append("font-style:italic;font-size:smaller;font-weight:bold;");
        return builder.toString();
    }

    public String getPhaseName() {
        return phaseName;
    }

    public boolean isPhase() {
        return true;
    }

    List<BuildState> childrenBuildState = new ArrayList<BuildState>();

    public void addChildBuildState(BuildState jobBuildState) {
        childrenBuildState.add(jobBuildState);
    }

    public String getRelativeNameFrom(ItemGroup g) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRelativeNameFrom(Item item) {
        // TODO Auto-generated method stub
        return null;
    }
}
