package com.tikal.jenkins.plugins.multijob.test;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.model.Cause;
import hudson.model.queue.QueueTaskFuture;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@WithJenkins
class MultiJobViewTest {

    @Test
    void simpleView(JenkinsRule j) throws Exception {
        j.createFreeStyleProject("job1").setQuietPeriod(0);
        j.createFreeStyleProject("job2").setQuietPeriod(0);
        MultiJobProject mj = j.createProject(MultiJobProject.class, "root");
        // create 'FirstPhase' containing job 'free'
        List<PhaseJobsConfig> jobs = new ArrayList<>();
        jobs.add(new PhaseJobsConfig("job1", "job1Alias", null, true, null,
                PhaseJobsConfig.KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
                false, false, "", false, false));
        jobs.add(new PhaseJobsConfig("job2", "job2Alias", null, true, null,
                PhaseJobsConfig.KillPhaseOnJobResultCondition.NEVER, false, false, "", 0,
                false, false, "", false, false));
        MultiJobBuilder phase1Builder = new MultiJobBuilder("FirstPhase", jobs, MultiJobBuilder.ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL,
                null);
        mj.getBuildersList().add(phase1Builder);

        JenkinsRule.WebClient client = j.createWebClient();

        client.getPage(mj); // MultiJob project page, assert it opens with no errors

        QueueTaskFuture<MultiJobBuild> future = mj.scheduleBuild2(0, new Cause.UserIdCause());
        MultiJobBuild multiJobBuild = future.get(10, TimeUnit.SECONDS);

        client.getPage(multiJobBuild); // MultiJob project page, assert it opens with no errors

        client.getPage(mj); // Check job page again after build
    }
}
