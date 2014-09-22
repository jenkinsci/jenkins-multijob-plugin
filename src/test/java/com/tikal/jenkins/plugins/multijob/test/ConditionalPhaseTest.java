package com.tikal.jenkins.plugins.multijob.test;

import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStep;
import hudson.tasks.Shell;

import java.util.ArrayList;
import java.util.List;

import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder.ContinuationCondition;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import com.tikal.jenkins.plugins.multijob.views.PhaseWrapper;
import com.tikal.jenkins.plugins.multijob.views.ProjectWrapper;

/**
 * @author Bartholdi Dominik (imod)
 */
public class ConditionalPhaseTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testConditionalPhase() throws Exception {
        j.jenkins.getInjector().injectMembers(this);
        
        // MultiTop
        //  |_ FirstPhase
        //      |_ free
        //  |_ [?] SecondPhase 
        //          |_ free2

        final FreeStyleProject free = j.jenkins.createProject(FreeStyleProject.class, "Free");
        free.getBuildersList().add(new Shell("echo hello"));
        final FreeStyleProject free2 = j.jenkins.createProject(FreeStyleProject.class, "Free2");
        free2.getBuildersList().add(new Shell("echo hello2"));
        
        final MultiJobProject multi = j.jenkins.createProject(MultiJobProject.class, "MultiTop");

        // create 'FirstPhase' containing job 'free'
        PhaseJobsConfig firstPhase = new PhaseJobsConfig("free", null, true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "");
        List<PhaseJobsConfig> configTopList = new ArrayList<PhaseJobsConfig>();
        configTopList.add(firstPhase);
        MultiJobBuilder firstPhaseBuilder = new MultiJobBuilder("FirstPhase", configTopList, ContinuationCondition.SUCCESSFUL);
        
        
        // create 'SecondPhase' containing job 'free2'
        PhaseJobsConfig secondPhase = new PhaseJobsConfig("free2", null, true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "");
        List<PhaseJobsConfig> configTopList2 = new ArrayList<PhaseJobsConfig>();
        configTopList.add(secondPhase);
        MultiJobBuilder secondPhaseBuilder = new MultiJobBuilder("SecondPhase", configTopList2, ContinuationCondition.SUCCESSFUL);
        
        multi.getBuildersList().add(new Shell("echo dude"));
        multi.getBuildersList().add(firstPhaseBuilder);

        // wrap second phase in condition
        List<BuildStep> blist = new ArrayList<BuildStep>();
        blist.add(secondPhaseBuilder);
        multi.getBuildersList().add(new ConditionalBuilder(new AlwaysRun(), new BuildStepRunner.Run(), blist));

        j.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
        Assert.assertTrue("shell task writes 'hello' to log", free.getLastBuild().getLog(10).contains("hello"));
        Assert.assertTrue("shell task writes 'dude' to log", multi.getLastBuild().getLog(10).contains("dude"));
        
        // check for correct number of items to be displayed
        int numberOfPhases = 0;
        int numberOfConditionalPhases = 0;
        int numberOfProjects = 0;
        for (TopLevelItem item : multi.getView().getRootItem(multi)) {
            if(item instanceof ProjectWrapper) {
                ++numberOfProjects;
            }else if (item instanceof PhaseWrapper) {
                ++numberOfPhases;
                if(((PhaseWrapper)item).isConditional()) {
                    ++numberOfConditionalPhases;
                }
            }
        }
        Assert.assertEquals("there should be two phases and three projects", 5, multi.getView().getRootItem(multi).size());
        Assert.assertEquals("there should be two phases", 2, numberOfPhases);
        Assert.assertEquals("there should be three projects", 3, numberOfProjects);
        Assert.assertEquals("there should be 1 conditional phase", 1, numberOfConditionalPhases);
        
    }

}
