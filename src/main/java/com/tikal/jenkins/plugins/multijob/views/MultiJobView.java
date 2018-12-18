package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.Indenter;
import hudson.Util;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.RunList;
import hudson.views.ListViewColumn;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuild.SubBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;

public class MultiJobView extends ListView {

    private DescribableList<ListViewColumn, Descriptor<ListViewColumn>> columns = new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(
            this, MultiJobListViewColumn.createDefaultInitialColumnList());

    @DataBoundConstructor
    public MultiJobView(String name) {
        super(name);
    }

    public MultiJobView(String name, ViewGroup owner) {
        super(name, owner);
    }

    @Override
    public DescribableList<ListViewColumn, Descriptor<ListViewColumn>> getColumns() {
        return columns;
    }

    @Extension
    public static final class DescriptorImpl extends ViewDescriptor {
        public String getDisplayName() {
            return "MultiJob View";
        }

        /**
         * Checks if the include regular expression is valid.
         */
        public FormValidation doCheckIncludeRegex(@QueryParameter String value)
                throws IOException, ServletException, InterruptedException {
            String v = Util.fixEmpty(value);
            if (v != null) {
                try {
                    Pattern.compile(v);
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error(pse.getMessage());
                }
            }
            return FormValidation.ok();
        }
    }

    @Override
    public List<TopLevelItem> getItems() {
        Collection<TopLevelItem> items = Jenkins.getInstance().getItems();
        List<TopLevelItem> out = new ArrayList<TopLevelItem>();
        for (TopLevelItem item : items) {
            if (item instanceof MultiJobProject) {
                MultiJobProject project = (MultiJobProject) item;
                // if (project.isTopMost()) {
                addTopLevelProject(project, out);
                // }
            }
        }
        return out;
    }

    public List<TopLevelItem> getRootItem(MultiJobProject multiJobProject) {
        List<TopLevelItem> out = new ArrayList<TopLevelItem>();
        addTopLevelProject(multiJobProject, out);
        return out;
    }

    private void addTopLevelProject(MultiJobProject project,
            List<TopLevelItem> out) {
        if (project.getBuilds().isEmpty()) {
            addMultiProject(null, project, createBuildState(project), 0, null, out);
        } else {
            addMultiProject(null, project.getLastBuild(), createBuildState(project), 0, out);
        }
    }

    private void addMultiProject(MultiJobProject parent,
            MultiJobProject project, BuildState buildState, int nestLevel,
            String phaseName, List<TopLevelItem> out) {
        out.add(new ProjectWrapper(
                parent,
                project,
                buildState,
                nestLevel,
                nestLevel == 0 ? project.getLastBuild() : null
        ));
        List<Builder> builders = project.getBuilders();
        for (Builder builder : builders) {
            int phaseNestLevel = nestLevel + 1;
            if (builder instanceof MultiJobBuilder) {
                addProjectFromBuilder(project, buildState, out, builder,
                        phaseNestLevel, false);
            }

            else if (builder instanceof ConditionalBuilder) {
                final List<BuildStep> conditionalbuilders = ((ConditionalBuilder) builder)
                        .getConditionalbuilders();
                for (BuildStep buildStep : conditionalbuilders) {
                    if (buildStep instanceof MultiJobBuilder) {
                        addProjectFromBuilder(project, buildState, out,
                                buildStep, phaseNestLevel, true);
                    }
                }
            }

            else if (builder instanceof SingleConditionalBuilder) {
                final BuildStep buildStep = ((SingleConditionalBuilder) builder)
                        .getBuildStep();
                if (buildStep instanceof MultiJobBuilder) {
                    addProjectFromBuilder(project, buildState, out, buildStep,
                            phaseNestLevel, true);
                }
            }
        }
    }

    private void addMultiProject(MultiJobBuild parentBuild,
                                 MultiJobBuild build, BuildState buildState, int nestLevel,
                                 List<TopLevelItem> out) {
        out.add(new ProjectWrapper(
                parentBuild != null ? parentBuild.getProject() : null,
                build != null ? build.getProject() : null,
                buildState,
                nestLevel,
                build)
        );
        List<Builder> builders = build.getProject().getBuilders();
        for (Builder builder : builders) {
            int phaseNestLevel = nestLevel + 1;
            if (builder instanceof MultiJobBuilder) {
                addProjectFromBuilder(build, buildState, out, builder,
                        phaseNestLevel, false);
            }

            else if (builder instanceof ConditionalBuilder) {
                final List<BuildStep> conditionalbuilders = ((ConditionalBuilder) builder)
                        .getConditionalbuilders();
                for (BuildStep buildStep : conditionalbuilders) {
                    if (buildStep instanceof MultiJobBuilder) {
                        addProjectFromBuilder(build, buildState, out,
                                buildStep, phaseNestLevel, true);
                    }
                }
            }

            else if (builder instanceof SingleConditionalBuilder) {
                final BuildStep buildStep = ((SingleConditionalBuilder) builder)
                        .getBuildStep();
                if (buildStep instanceof MultiJobBuilder) {
                    addProjectFromBuilder(build, buildState, out, buildStep,
                            phaseNestLevel, true);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void addProjectFromBuilder(MultiJobProject project,
            BuildState buildState, List<TopLevelItem> out, BuildStep builder,
            int phaseNestLevel, boolean isConditional) {
        MultiJobBuilder reactorBuilder = (MultiJobBuilder) builder;
        List<PhaseJobsConfig> subProjects = reactorBuilder.getPhaseJobs();
        String currentPhaseName = reactorBuilder.getPhaseName();
        PhaseWrapper phaseWrapper = new PhaseWrapper(
                project,
                phaseNestLevel,
                currentPhaseName,
                isConditional
        );
        out.add(phaseWrapper);
        for (PhaseJobsConfig projectConfig : subProjects) {
            Item tli = Jenkins.getInstance().getItem(
                    projectConfig.getJobName(),
                    project.getParent(),
                    AbstractProject.class
            );
            if (tli == null)
                continue;
            BuildState jobBuildState = createBuildState(
                    buildState,
                    project,
                    null,
                    projectConfig
            );
            phaseWrapper.addChildBuildState(jobBuildState);
            if (tli instanceof MultiJobProject) {
                MultiJobProject subProject = (MultiJobProject) tli;
                addMultiProject(
                        project,
                        subProject,
                        jobBuildState,
                        phaseNestLevel + 1,
                        currentPhaseName,
                        out
                );
            } else {
                Job subProject = (Job) tli;
                addSimpleProject(
                        project,
                        subProject,
                        jobBuildState,
                        phaseNestLevel + 1,
                        out,
                        null
                );
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void addProjectFromBuilder(MultiJobBuild build,
            BuildState buildState, List<TopLevelItem> out, BuildStep builder,
            int phaseNestLevel, boolean isConditional) {
        MultiJobBuilder reactorBuilder = (MultiJobBuilder) builder;
        List<PhaseJobsConfig> subProjects = reactorBuilder.getPhaseJobs();
        String currentPhaseName = reactorBuilder.getPhaseName();
        PhaseWrapper phaseWrapper = new PhaseWrapper(
                build.getProject(),
                phaseNestLevel,
                currentPhaseName,
                isConditional
        );
        out.add(phaseWrapper);

        for (PhaseJobsConfig projectConfig : subProjects) {
            Item abstractProject = Jenkins.getInstance().getItem(
                    projectConfig.getJobName(),
                    build.getParent(),
                    Job.class
            );
            if (abstractProject == null)
                continue;
            SubBuild sb = searchBuildnumberFromMultijobbuild(build, projectConfig, null);
            Run subBuild = sb == null ? null : sb.getBuild();
            BuildState jobBuildState = createBuildState(
                    buildState,
                    build.getProject(),
                    subBuild,
                    projectConfig
            );
            phaseWrapper.addChildBuildState(jobBuildState);
            if (abstractProject instanceof MultiJobProject) {
                if (subBuild == null) {
                    addMultiProject(
                            build.getProject(),
                            (MultiJobProject) abstractProject,
                            jobBuildState,
                            phaseNestLevel + 1,
                            currentPhaseName,
                            out
                    );
                } else {
                    addMultiProject(
                            build,
                            (MultiJobBuild)subBuild,
                            jobBuildState,
                            phaseNestLevel + 1,
                            out
                    );
                }
            } else {
                addSimpleProject(
                        build.getProject(),
                        subBuild == null ? ((Job) abstractProject) : subBuild.getParent(),
                        jobBuildState,
                        phaseNestLevel + 1,
                        out,
                        subBuild
                );
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void addSimpleProject(MultiJobProject parent, Job project,
            BuildState buildState, int nestLevel, List<TopLevelItem> out,
            Run build) {
        out.add(new ProjectWrapper(parent, project, buildState, nestLevel, build));
    }

    private SubBuild searchBuildnumberFromMultijobbuild(MultiJobBuild multiJobBuild, PhaseJobsConfig config, Result result) {
        for (SubBuild subBuild : multiJobBuild.getSubBuilds()) {
            if (!(subBuild.getJobName().equals(config.getJobName()) &&
                    subBuild.getJobAlias().equals(config.getJobAlias()))) {
                continue;
            }
            if (result != null) {
                if (result.equals(subBuild.getResult())) {
                    return subBuild;
                }
            } else {
                return subBuild;
            }
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes" })
    private BuildState createBuildState(BuildState parentBuildState,
            MultiJobProject multiJobProject, Run abstractBuild, PhaseJobsConfig config) {
        int previousBuildNumber = 0;
        int lastBuildNumber = 0;
        int lastSuccessBuildNumber = 0;
        int lastFailureBuildNumber = 0;
        MultiJobBuild previousParentBuild = multiJobProject
                .getBuildByNumber(parentBuildState.getPreviousBuildNumber());
        MultiJobBuild lastParentBuild = multiJobProject
                .getBuildByNumber(parentBuildState.getLastBuildNumber());
        MultiJobBuild lastParentSuccessBuild = multiJobProject
                .getBuildByNumber(parentBuildState.getLastSuccessBuildNumber());
        MultiJobBuild lastParentFailureBuild = multiJobProject
                .getBuildByNumber(parentBuildState.getLastFailureBuildNumber());

        if (abstractBuild != null) {
            if (Result.SUCCESS.equals(abstractBuild.getResult())) {
                lastSuccessBuildNumber = abstractBuild.getNumber();

                if (lastParentFailureBuild != null) {
                    SubBuild sb = searchBuildnumberFromMultijobbuild(
                            lastParentFailureBuild, config, Result.FAILURE);
                    lastFailureBuildNumber = sb == null ? 0 : sb.getBuildNumber();
                }
                if (lastFailureBuildNumber ==  0) {
                    // TODO: not quite correct yet
                    // need to go back to TopMultiJobItem recursively....
                    for (MultiJobBuild multiJobBuild : multiJobProject.getBuilds()) {
                        SubBuild sb = searchBuildnumberFromMultijobbuild(
                                multiJobBuild, config, Result.FAILURE);
                        lastFailureBuildNumber = sb == null ? 0 : sb.getBuildNumber();
                        if (lastFailureBuildNumber != 0) {
                            break;
                        }
                    }
                }
            }

            if (Result.FAILURE.equals(abstractBuild.getResult())) {
                lastFailureBuildNumber = abstractBuild.getNumber();

                if (lastParentSuccessBuild != null) {
                    SubBuild sb = searchBuildnumberFromMultijobbuild(
                            lastParentSuccessBuild, config, Result.SUCCESS);
                    lastSuccessBuildNumber = sb == null ? 0 : sb.getBuildNumber();
                }
                if (lastSuccessBuildNumber == 0) {
                    // TODO: not quite correct yet
                    // need to go back to TopMultiJobItem recursively....
                    for (MultiJobBuild multiJobBuild : multiJobProject.getBuilds()) {
                        SubBuild sb = searchBuildnumberFromMultijobbuild(
                                multiJobBuild, config, Result.SUCCESS);
                        lastSuccessBuildNumber = sb == null ? 0 : sb.getBuildNumber();
                        if (lastSuccessBuildNumber != 0) {
                            break;
                        }
                    }
                }
            }
        }

        if (previousParentBuild != null) {
            SubBuild sb = searchBuildnumberFromMultijobbuild(
                    previousParentBuild, config, null);
            previousBuildNumber = sb == null ? 0 : sb.getBuildNumber();
        }
        if (lastParentBuild != null) {
            SubBuild sb = searchBuildnumberFromMultijobbuild(
                    lastParentBuild, config, null);
            lastBuildNumber = sb == null ? 0 : sb.getBuildNumber();
        }

        return new BuildState(config.getJobName(), config.getJobAlias(), previousBuildNumber,
                lastBuildNumber, lastSuccessBuildNumber, lastFailureBuildNumber);
    }

    private BuildState createBuildState(MultiJobProject project) {

        MultiJobBuild lastBuild = project.getLastBuild();
        MultiJobBuild previousBuild = lastBuild == null ? null : lastBuild
                .getPreviousBuild();
        MultiJobBuild lastSuccessfulBuild = project.getLastSuccessfulBuild();
        MultiJobBuild lastFailedBuild = project.getLastFailedBuild();
        return new BuildState(project.getName(), null, previousBuild == null ? 0
                : previousBuild.getNumber(), lastBuild == null ? 0
                : lastBuild.getNumber(), lastSuccessfulBuild == null ? 0
                : lastSuccessfulBuild.getNumber(), lastFailedBuild == null ? 0
                : lastFailedBuild.getNumber());
    }

    @Override
    protected void submit(StaplerRequest req) throws ServletException,
            FormException, IOException {
    }

    protected void initColumns() {
        try {
            Field field = ListView.class.getDeclaredField("columns");
            field.setAccessible(true);
            field.set(
                    this,
                    new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(
                            this, MultiJobListViewColumn
                                    .createDefaultInitialColumnList()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("rawtypes")
    public Indenter<Job> createIndenter() {
        return new Indenter<Job>() {

            protected int getNestLevel(Job job) {
                if ((TopLevelItem) job instanceof ProjectWrapper) {
                    ProjectWrapper projectWrapper = (ProjectWrapper) (TopLevelItem) job;
                    return projectWrapper.getNestLevel();
                }
                return 0;
            }
        };
    }
}
