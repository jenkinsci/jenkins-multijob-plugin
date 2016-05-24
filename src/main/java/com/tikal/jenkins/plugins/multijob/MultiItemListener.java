package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

/**
 * Looking for aborted multi job builds and resume them after restart
 */
@Extension
public class MultiItemListener extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(MultiItemListener.class.getName());

    private static final String CONFIG_REGEX = "com.tikal.jenkins.plugins.multijob.resume\\d+.xml";

    private static final String RESTART_REGEX = "com.tikal.jenkins.plugins.multijob.restart\\d+.xml";

    @Override
    public void onLoaded() {
        List<MultiJobProject> items = Jenkins.getActiveInstance().getItems(MultiJobProject.class);

        items.stream().forEach(item -> {

            //Last success/failure init
            MultiJobBuild lastBuild = item.getLastBuild();
            if (null != lastBuild) {
                for (MultiJobBuild.SubBuild subBuild : lastBuild.getSubBuilds()) {
                    Item it = Jenkins.getActiveInstance().getItemByFullName(subBuild.getJobName());
                    if (null != it) {
                        AbstractProject<?, ?> job = (AbstractProject<?, ?>) it;
                        AbstractBuild<?, ?> sub = job.getBuildByNumber(subBuild.getBuildNumber());
                        if (null == subBuild.getFailureTimestamp() || null == subBuild.getSuccessTimestamp()) {
                            Cause.UpstreamCause cause = sub.getCause(Cause.UpstreamCause.class);
                            if (null != cause) {
                                int success = 0;
                                int failure = 0;
                                if (null != sub.getResult()) {
                                    boolean s = false;
                                    boolean f = false;
                                    if (Result.SUCCESS.equals(sub.getResult())) {
                                        success = sub.getNumber();
                                        s = true;
                                    }
                                    if (Result.FAILURE.equals(sub.getResult())) {
                                        failure = sub.getNumber();
                                        f = true;
                                    }
                                    String prjStr = cause.getUpstreamProject();
                                    for (Run<?, ?> run : sub.getProject().getBuilds()) {
                                        Cause.UpstreamCause c = run.getCause(Cause.UpstreamCause.class);
                                        if (null != c && c.getUpstreamProject().equals(prjStr)) {
                                            if (run.getResult().equals(Result.SUCCESS) && run.getNumber() > success) {
                                                success = run.getNumber();
                                                s = true;
                                            }
                                            if (run.getResult().equals(Result.FAILURE) && run.getNumber() > failure) {
                                                failure = run.getNumber();
                                                f = true;
                                            }
                                        }
                                        if (s && f) {
                                            break;
                                        }
                                    }
                                    if (s) {
                                        Long successTimestamp = 0 == success ? null : job.getBuildByNumber(success)
                                                .getTimeInMillis();
                                        subBuild.setSuccessTimestamp(successTimestamp);
                                    }
                                    if (f) {
                                        Long failureTimestamp = 0 == failure ? null : job.getBuildByNumber(failure)
                                                .getTimeInMillis();
                                        subBuild.setFailureTimestamp(failureTimestamp);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (item.isSurviveRestart()) {
                File[] restartFlags = item.getRootDir().listFiles((dir, name) -> {
                    return name.matches(RESTART_REGEX);
                });

                for (File restartFlag : restartFlags) {
                    int buildNumber = Integer.valueOf(restartFlag.getName().replaceAll("\\D+", ""));
                    MultiJobBuild build = item.getBuildByNumber(buildNumber);
                    if (null != build) {
                        scheduleResumeBuild(item, build);
                        try {
                            Files.delete(restartFlag.toPath());
                        } catch (IOException e) {
                            LOGGER.warning("Failed to remove restart flag for " + item.getDisplayName() + " #" + buildNumber);
                        }
                    }
                }


                File[] configs = item.getRootDir().listFiles((dir, name) -> {
                    return name.matches(CONFIG_REGEX);
                });

                for (File config : configs) {
                    int buildNumber = Integer.valueOf(config.getName().replaceAll("\\D+", ""));

                    if (null == item.getBuildByNumber(buildNumber)) {
                        Path buildDir = Paths.get(item.getRootDir().getAbsolutePath(), "builds", String.valueOf
                                (buildNumber));
                        Path buildXmlPath = Paths.get(buildDir.toString(), "build.xml");
                        try {
                            Files.copy(config.toPath(), buildXmlPath);
                            MultiJobBuild build = new MultiJobBuild(item, buildDir.toFile());
                            scheduleResumeBuild(item, build);
                        } catch (IOException e) {
                            LOGGER.warning("Failed to copy build config for " + item.getDisplayName() + " #" + buildNumber);
                        }
                    }

                    try {
                        Files.delete(config.toPath());
                    } catch (IOException e) {
                        LOGGER.warning("Failed to remove resume config for " + item.getDisplayName() + " #" + buildNumber);
                    }
                }
            }
        });
    }

    private void scheduleResumeBuild(MultiJobProject project, MultiJobBuild build) {
        List<Action> actions = Utils.copyBuildCauses(build);
        actions.add(new MultiJobResumeControl(build));
        actions.add(new CauseAction(new ResumeCause(build, true)));
        Jenkins.getActiveInstance().getQueue().schedule2(project, project.getQuietPeriod(), actions);
    }
}
