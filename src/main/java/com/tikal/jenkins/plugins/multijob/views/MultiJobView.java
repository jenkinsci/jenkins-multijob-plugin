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
        Collection<TopLevelItem> items = Hudson.getInstance().getItems();
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
        addMultiProject(null, project, createBuildState(project), 0, null, out);
    }

    private void addMultiProject(MultiJobProject parent,
            MultiJobProject project, BuildState buildState, int nestLevel,
            String phaseName, List<TopLevelItem> out) {
        //TODO: Nested MultiJob
        out.add(new ProjectWrapper(parent, project, buildState, nestLevel, project.getLastBuild()));
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


    @SuppressWarnings("rawtypes")
    private void addProjectFromBuilder(MultiJobProject project,
            BuildState buildState, List<TopLevelItem> out, BuildStep builder,
            int phaseNestLevel, boolean isConditional) {
        MultiJobBuilder reactorBuilder = (MultiJobBuilder) builder;
        List<PhaseJobsConfig> subProjects = reactorBuilder.getPhaseJobs();
        String currentPhaseName = reactorBuilder.getPhaseName();
        PhaseWrapper phaseWrapper = new PhaseWrapper(project, phaseNestLevel,
                currentPhaseName, isConditional);
        out.add(phaseWrapper);
        for (PhaseJobsConfig projectConfig : subProjects) {
            Item tli = Jenkins.getInstance().getItem(projectConfig.getJobName(), project.getParent(), AbstractProject.class);
            if (tli instanceof MultiJobProject) {
                MultiJobProject subProject = (MultiJobProject) tli;
                BuildState jobBuildState = createBuildState(buildState,
                        project, subProject, projectConfig.getJobAlias());
                phaseWrapper.addChildBuildState(jobBuildState);
                addMultiProject(project, subProject, jobBuildState,
                        phaseNestLevel + 1, currentPhaseName, out);
            } else {
                Job subProject = (Job) tli;
                if (subProject == null)
                    continue;
                BuildState jobBuildState = createBuildState(buildState,
                        project, subProject, projectConfig.getJobAlias());
                phaseWrapper.addChildBuildState(jobBuildState);

                Run tmp_build, latestAliasBuild = null;
                int lastSuccess = 0, lastFailure = 0;
                if( project.getLastBuild() != null ) {
                    for ( MultiJobBuild mjb : project.getBuilds() ) {
                    	for ( SubBuild sb : mjb.getSubBuilds() ) {
							if (sb.getJobAlias() != null) {
								if (sb.getJobAlias().equals(projectConfig.getJobAlias())) {
									tmp_build = subProject.getBuildByNumber(sb.getBuildNumber());
									if (tmp_build != null) {
										if (latestAliasBuild == null) {
											latestAliasBuild = tmp_build;
										}

										if (lastFailure == 0 && tmp_build.getIconColor() == BallColor.RED) {
											lastFailure = tmp_build.getNumber();
										} else if (lastSuccess == 0 && tmp_build.getIconColor() == BallColor.BLUE) {
											lastSuccess = tmp_build.getNumber();
										}

										if (lastFailure != 0 && lastSuccess != 0) {
											break;
										}
									}
								}
							}
						}

						if (lastFailure != 0 && lastSuccess != 0) {
							break;
						}
					}

                    jobBuildState = new BuildState(jobBuildState.jobName, jobBuildState.previousBuildNumber,
							jobBuildState.lastBuildNumber, lastSuccess, lastFailure);
                }
                addSimpleProject(project, subProject, jobBuildState,
                        phaseNestLevel + 1, out, latestAliasBuild);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void addSimpleProject(MultiJobProject parent, Job project,
            BuildState buildState, int nestLevel, List<TopLevelItem> out,
            Run build) {
        out.add(new ProjectWrapper(parent, project, buildState, nestLevel, build));
    }

    @SuppressWarnings({ "rawtypes" })
    private BuildState createBuildState(BuildState parentBuildState,
            MultiJobProject multiJobProject, Job project, String alias) {
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
        if (previousParentBuild != null) {
            for (SubBuild subBuild : previousParentBuild.getSubBuilds()) {
                if (subBuild.getJobName().equals(project.getName())) {
                    previousBuildNumber = subBuild.getBuildNumber();
                }
            }
        }
        if (lastParentBuild != null) {
            for (SubBuild subBuild : lastParentBuild.getSubBuilds()) {
                if (subBuild.getJobName().equals(project.getName())) {
                    lastBuildNumber = subBuild.getBuildNumber();
                }
            }
        }
        if (lastParentSuccessBuild != null) {
            for (SubBuild subBuild : lastParentSuccessBuild.getSubBuilds()) {
                if (subBuild.getJobName().equals(project.getName())) {
                    AbstractBuild build = (AbstractBuild) project
                            .getBuildByNumber(subBuild.getBuildNumber());
                    if (build != null
                            && Result.SUCCESS.equals(build.getResult())) {
                        lastSuccessBuildNumber = subBuild.getBuildNumber();
                        break;
                    } else {
                        lastParentSuccessBuild = multiJobProject
                                .getBuildByNumber(parentBuildState
                                        .getPreviousBuildNumber());
                    }
                }
            }
        }
        if (lastParentFailureBuild != null) {
            for (SubBuild subBuild : lastParentFailureBuild.getSubBuilds()) {
                if (subBuild.getJobName().equals(project.getName())) {
                    AbstractBuild build = (AbstractBuild) project
                            .getBuildByNumber(subBuild.getBuildNumber());
                    if (build != null
                            && Result.FAILURE.equals(((AbstractBuild) build)
                                    .getResult())) {
                        lastFailureBuildNumber = subBuild.getBuildNumber();
                        break;
                    } else {
                        lastParentFailureBuild = multiJobProject
                                .getBuildByNumber(parentBuildState
                                        .getPreviousBuildNumber());
                    }
                }
            }
        }
        String displayName = project.getDisplayName();
        if( alias != null )
		{
			if( !alias.equals("") ) {
				displayName += " (" + alias + ")";
			}
		}
        return new BuildState(displayName, previousBuildNumber,
                lastBuildNumber, lastSuccessBuildNumber, lastFailureBuildNumber);
    }

    private BuildState createBuildState(MultiJobProject project) {

        MultiJobBuild lastBuild = project.getLastBuild();
        MultiJobBuild previousBuild = lastBuild == null ? null : lastBuild
                .getPreviousBuild();
        MultiJobBuild lastSuccessfulBuild = project.getLastSuccessfulBuild();
        MultiJobBuild lastFailedBuild = project.getLastFailedBuild();
        return new BuildState(project.getName(), previousBuild == null ? 0
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
