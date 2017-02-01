package com.tikal.jenkins.plugins.multijob.test;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder.ContinuationCondition;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import com.tikal.jenkins.plugins.multijob.views.PhaseWrapper;
import com.tikal.jenkins.plugins.multijob.views.ProjectWrapper;
import hudson.model.*;
import hudson.model.Cause.UserCause;
import hudson.tasks.BuildStep;
import hudson.tasks.Shell;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static java.lang.System.out;

/**
 * @author Victor Ott (t0r0X)
 */
public class AllBuildsInvokedTest
{
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testAllJobsHaveBeenInvoked() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final int CHILDREN_COUNT = 5;

        // executor count: 1 parent + 2 per child
        j.jenkins.setNumExecutors(1 + CHILDREN_COUNT * 2);
        
        // P
        //  |_ C1
        //  |   |_ worker
        //  |_ C2
        //  |   |_ worker
        //  ...
        //  |_ CN
        //  |   |_ worker

        // worker freestyle job 'worker'
        final FreeStyleProject workerJob = j.jenkins.createProject(FreeStyleProject.class, "worker");
        workerJob.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("PAR1", "defaultVAL1", ""),
                new StringParameterDefinition("PAR2", "defaultVAL2", "")));
        workerJob.getBuildersList().add(new Shell("echo \"PAR1=$PAR1, PAR2=$PAR2\""));

        // children multijobs 'C1' .. 'Cn'
        MultiJobProject[] childrenJobs = new MultiJobProject[CHILDREN_COUNT];

        List<PhaseJobsConfig> childrenJobConfigList = new ArrayList<PhaseJobsConfig>(CHILDREN_COUNT);

        // create 'parentPhase' containing children jobs
        for (int i = 0; i < CHILDREN_COUNT; ++i) {
            MultiJobProject childJob = j.jenkins.createProject(MultiJobProject.class, "C" + i);
            childJob.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("PAR1", "defaultChildVAL1", ""),
                    new StringParameterDefinition("PAR2", "defaultChildVAL2", "")));

            // create multijob-phase containing 'worker' job
            PhaseJobsConfig phaseJobConfig = new PhaseJobsConfig("worker", null, true, null,
                    KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "",false, false);
            // TODO job parameters

            List<PhaseJobsConfig> phaseJobsConfigs = Collections.singletonList(phaseJobConfig);

            MultiJobBuilder workerPhaseBuilder = new MultiJobBuilder("Phase child runs worker",
                    phaseJobsConfigs, ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL);
            childJob.getBuildersList().add(workerPhaseBuilder);

            childJob.getBuildersList().add(new Shell("echo \"worker finished for $JOB_NAME\""));

            // TODO job parameters
            PhaseJobsConfig childPhaseJobConfig = new PhaseJobsConfig(childJob.getName(), null, false, null,
                    KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "", false, false);
            childrenJobConfigList.add(childPhaseJobConfig);

            childrenJobs[i] = childJob;
        }

        // parent multijob 'P'
        final MultiJobProject parentJob = j.jenkins.createProject(MultiJobProject.class, "P");
        workerJob.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("PAR1", "parentVAL1", "")));

        MultiJobBuilder childrenPhaseBuilder = new MultiJobBuilder("Phase 'parent runs children'", childrenJobConfigList,
                ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL);
        parentJob.getBuildersList().add(childrenPhaseBuilder);
        parentJob.getBuildersList().add(new Shell("echo 'all children finished'"));

        // run first child job and its worker => the build number must be 1 for each
        j.assertBuildStatus(Result.SUCCESS, childrenJobs[0].scheduleBuild2(0, new Cause.UserIdCause()).get());

        out.println("=============================================");
        out.printf("parent job '%s' last build ID: %s\n", parentJob.getName(), parentJob.getLastBuild());
        for (MultiJobProject job : childrenJobs) {
            out.printf("child job '%s' last build ID: %s\n", job.getName(), job.getLastBuild());
        }
        out.printf("worker job '%s' last build ID: %s\n", workerJob.getName(), workerJob.getLastBuild());

        assertEquals("Child job 0 should have built exactly 1 time", 1, childrenJobs[0].getLastBuild().getNumber());
        assertEquals("Worker job should have built exactly 1 time", 1, workerJob.getLastBuild().getNumber());

        // run parent job, its children and their worker => build numbers must have certain values
        j.assertBuildStatus(Result.SUCCESS, parentJob.scheduleBuild2(0, new Cause.UserIdCause()).get());

        out.println("=============================================");
        out.printf("parent job '%s' last build ID: %s\n", parentJob.getName(), parentJob.getLastBuild());
        for (MultiJobProject job : childrenJobs) {
            out.printf("child job '%s' last build ID: %s\n", job.getName(), job.getLastBuild());
        }
        out.printf("worker job '%s' last build ID: %s\n", workerJob.getName(), workerJob.getLastBuild());

        out.println("=============================================");
        FreeStyleBuild build = workerJob.getFirstBuild();
        while (build != null) {
            out.printf("  worker job build ID: %s / %d  %s\n", build.getId(), build.getNumber(), build.getUpstreamBuilds().toString());
            build = build.getNextBuild();
        }

        assertEquals("Child job 0 should have been built exactly 2 times", 2, childrenJobs[0].getLastBuild().getNumber());
        assertEquals("Worker job should have been built exactly N + 1 times", CHILDREN_COUNT + 1, workerJob.getLastBuild().getNumber());

//        out.println("=============================================");
//        out.println(parentJob.getLastBuild().getLog(1000));
//        out.println("=============================================");
    }

}
