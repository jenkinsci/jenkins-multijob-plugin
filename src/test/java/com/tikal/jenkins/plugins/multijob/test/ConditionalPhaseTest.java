package com.tikal.jenkins.plugins.multijob.test;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder.ContinuationCondition;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import com.tikal.jenkins.plugins.multijob.views.PhaseWrapper;
import com.tikal.jenkins.plugins.multijob.views.ProjectWrapper;
import hudson.Functions;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStep;
import hudson.tasks.Shell;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Bartholdi Dominik (imod)
 */
@WithJenkins
class ConditionalPhaseTest {

    @Test
    void testConditionalPhase(JenkinsRule j) throws Throwable {
        // MultiTop
        //  |_ FirstPhase
        //      |_ free
        //  |_ [?] SecondPhase
        //          |_ free2

        FreeStyleProject free = j.jenkins.createProject(FreeStyleProject.class, "Free");
        FreeStyleProject free2 = j.jenkins.createProject(FreeStyleProject.class, "Free2");

        if (Functions.isWindows()) {
            free.getBuildersList().add(new BatchFile("echo hello"));
            free2.getBuildersList().add(new BatchFile("echo hello2"));
        } else {
            free.getBuildersList().add(new Shell("echo hello"));
            free2.getBuildersList().add(new Shell("echo hello"));
        }

        MultiJobProject multi = j.jenkins.createProject(MultiJobProject.class, "MultiTop");

        // create 'FirstPhase' containing job 'free'
        PhaseJobsConfig firstPhase = new PhaseJobsConfig("free", "freeAlias", null, true, null,
                KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
                false, false, "", false, false);
        List<PhaseJobsConfig> configTopList = new ArrayList<>();
        configTopList.add(firstPhase);
        MultiJobBuilder firstPhaseBuilder = new MultiJobBuilder("FirstPhase", configTopList, ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL, null);


        // create 'SecondPhase' containing job 'free2'
        PhaseJobsConfig secondPhase = new PhaseJobsConfig("free2", "free2Alias", null, true, null,
                KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
                false, false, "", false, false);
        List<PhaseJobsConfig> configTopList2 = new ArrayList<>();
        configTopList.add(secondPhase);
        MultiJobBuilder secondPhaseBuilder = new MultiJobBuilder("SecondPhase", configTopList2, ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL, null);

        multi.getBuildersList().add(firstPhaseBuilder);
        if (Functions.isWindows()) {
            multi.getBuildersList().add(new BatchFile("echo dude"));
        } else {
            multi.getBuildersList().add(new Shell("echo dude"));
        }

        // wrap second phase in condition
        List<BuildStep> blist = new ArrayList<>();
        blist.add(secondPhaseBuilder);
        multi.getBuildersList().add(new ConditionalBuilder(new AlwaysRun(), new BuildStepRunner.Run(), blist));

        MultiJobBuild b = j.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
        assertTrue(free.getLastBuild().getLog(10).contains("hello"), "shell task writes 'hello' to log");
        assertTrue(multi.getLastBuild().getLog(10).contains("dude"), "shell task writes 'dude' to log");
        // check for correct number of items to be displayed
        int numberOfPhases = 0;
        int numberOfConditionalPhases = 0;
        int numberOfProjects = 0;
        for (TopLevelItem item : multi.getView().getRootItem(multi)) {
            if (item instanceof ProjectWrapper) {
                ++numberOfProjects;
            } else if (item instanceof PhaseWrapper phaseWrapper) {
                ++numberOfPhases;
                if (phaseWrapper.isConditional()) {
                    ++numberOfConditionalPhases;
                }
            }
        }

        assertEquals(5, multi.getView().getRootItem(multi).size(), "there should be two phases and three projects");
        assertEquals(2, numberOfPhases, "there should be two phases");
        assertEquals(3, numberOfProjects, "there should be three projects");
        assertEquals(1, numberOfConditionalPhases, "there should be 1 conditional phase");
        assertSubBuilds(b, "Free#1", "Free2#1");

        // restart the instance
        j.restart();

        multi = j.jenkins.getItemByFullName("MultiTop", MultiJobProject.class);
        b = multi.getBuildByNumber(1);
        // JENKINS-49328: ensure that SubBuild.getBuild() works after a restart:
        assertSubBuilds(b, "Free#1", "Free2#1");
    }

    private static void assertSubBuilds(MultiJobBuild b, String... externalIDs) {
        List<String> ids = b.getSubBuilds().stream()
                .map(sub -> sub.getBuild().getExternalizableId()).sorted().toList();
        assertIterableEquals(ids, Arrays.stream(externalIDs).sorted().toList());
    }

}
