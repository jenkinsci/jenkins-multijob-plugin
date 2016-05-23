package com.tikal.jenkins.plugins.multijob;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.CauseAction;
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

    private static final String REGEX = "com.tikal.jenkins.plugins.multijob.resume\\d+.xml";

    @Override
    public void onLoaded() {
        List<MultiJobProject> items = Jenkins.getActiveInstance().getItems(MultiJobProject.class);

        items.stream().filter(MultiJobProject::isSurviveRestart).forEach(item -> {
            File[] configs = item.getRootDir().listFiles((dir, name) -> {
                return name.matches(REGEX);
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

                        final MultiJobResumeControl control = new MultiJobResumeControl(build);
                        List<Action> actions = Utils.copyBuildCauses(build);
                        actions.add(control);
                        actions.add(new CauseAction(new ResumeCause(build)));
                        Jenkins.getActiveInstance().getQueue().schedule2(item, item.getQuietPeriod(), actions);
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
        });
    }
}
