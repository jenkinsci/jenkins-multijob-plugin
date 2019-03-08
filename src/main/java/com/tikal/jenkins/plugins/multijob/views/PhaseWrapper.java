package com.tikal.jenkins.plugins.multijob.views;

import hudson.model.BallColor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.apache.commons.lang.ObjectUtils;

import java.util.*;

@SuppressWarnings("rawtypes")
public class PhaseWrapper extends AbstractWrapper {

    final String phaseName;

    final boolean isConditional;

    public PhaseWrapper(Job project, int nestLevel, int index, String phaseName, boolean isConditional) {
        super(project, nestLevel, index);
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
        BallColor iconColor01 = getIconColor01(childWrappers, null);
        return iconColor01;
    }

    public BallColor getIconColor01(List<AbstractWrapper> subWrapper, BallColor worseBallColor) {

        for (AbstractWrapper childWrapper : subWrapper) {

            if (childWrapper instanceof ProjectWrapper) {
                /**
                 * ProjectWrapper时，获取Job的状态
                 */
                ProjectWrapper projectWrapper = (ProjectWrapper) childWrapper;
                BallColor wrapperIconColor = projectWrapper.getIconColor();
                worseBallColor = getWorseIconColor(worseBallColor, wrapperIconColor);
            } else {
                /**
                 * PhaseWrapper时，获取下级wrapper的最差构建状态。
                 */
                worseBallColor = getIconColor01(childWrapper.childWrappers, worseBallColor);
            }
        }

        return worseBallColor;
    }

    public BallColor getWorseIconColor(BallColor oldBallColor, BallColor newBallColor) {
        if (oldBallColor == null) {
            return newBallColor;
        }

        if (newBallColor == null) {
            return oldBallColor;
        }

        EnumMap<BallColor, Integer> ballMap = new EnumMap<>(BallColor.class);
        ballMap.put(BallColor.RED_ANIME, 0);
        ballMap.put(BallColor.YELLOW_ANIME, 1);
        ballMap.put(BallColor.BLUE_ANIME, 2);
        ballMap.put(BallColor.GREY_ANIME, 3);
        ballMap.put(BallColor.DISABLED_ANIME, 4);
        ballMap.put(BallColor.ABORTED_ANIME, 5);
        ballMap.put(BallColor.NOTBUILT_ANIME, 6);
        ballMap.put(BallColor.RED, 7);
        ballMap.put(BallColor.YELLOW, 8);
        ballMap.put(BallColor.BLUE, 9);
        ballMap.put(BallColor.GREY, 10);
        ballMap.put(BallColor.DISABLED, 11);
        ballMap.put(BallColor.ABORTED, 12);
        ballMap.put(BallColor.NOTBUILT, 13);

        Integer oldOrder = ballMap.get(oldBallColor);
        if (oldOrder == null) {
            return newBallColor;
        }

        Integer newOrder = ballMap.get(newBallColor);
        if (newOrder == null) {
            return oldBallColor;
        } else {
            if (oldOrder - newOrder > 0) {
                return newBallColor;
            }
        }

        return oldBallColor;
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
