package com.tikal.jenkins.plugins.multijob.test;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.MultiJobResumeBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobResumeControl;
import hudson.model.Item;
import hudson.model.Result;
import hudson.security.csrf.DefaultCrumbIssuer;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class MultiJobResumeBuildTest {

    private static final String VICTIM = "victim";

    @Test
    @Issue("SECURITY-3781")
    void getResumeWithoutCrumbDoesNotScheduleBuild(JenkinsRule j) throws Exception {
        MultiJobProject multi = j.createProject(MultiJobProject.class, "TargetMultiJob");
        multi.setQuietPeriod(0);
        multi.getBuildersList().add(new FailureBuilder());

        MultiJobBuild failedBuild = j.assertBuildStatus(Result.FAILURE, multi.scheduleBuild2(0).get());
        assertNotNull(failedBuild.getAction(MultiJobResumeBuild.class));

        j.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ).everywhere().to(VICTIM)
                .grant(Item.READ, Item.BUILD).onItems(multi).to(VICTIM));

        JenkinsRule.WebClient client = j.createWebClient();
        client.withBasicCredentials(VICTIM);
        client.withRedirectEnabled(false).withThrowExceptionOnFailingStatusCode(false);

        WebRequest resumeGet = new WebRequest(new URL(j.getURL(), failedBuild.getUrl() + "resume/"));
        WebResponse response = client.loadWebResponse(resumeGet);
        j.waitUntilNoActivity();

        assertAll(
                () -> assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getStatusCode(),
                        "GET /resume/ should be rejected before the resume action is processed"),
                () -> assertEquals(1, multi.getLastBuild().getNumber(),
                        "GET /resume/ without a crumb must not schedule a new build"));
    }

    @Test
    void postResumeWithCrumbSchedulesOneResumedBuild(JenkinsRule j) throws Exception {
        MultiJobProject multi = j.createProject(MultiJobProject.class, "TargetMultiJob");
        multi.setQuietPeriod(0);
        multi.getBuildersList().add(new FailureBuilder());

        MultiJobBuild failedBuild = j.assertBuildStatus(Result.FAILURE, multi.scheduleBuild2(0).get());
        assertNotNull(failedBuild.getAction(MultiJobResumeBuild.class));

        j.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ).everywhere().to(VICTIM)
                .grant(Item.READ, Item.BUILD).onItems(multi).to(VICTIM));

        JenkinsRule.WebClient client = j.createWebClient();
        client.withBasicCredentials(VICTIM);
        client.withRedirectEnabled(false).withThrowExceptionOnFailingStatusCode(false);

        Page crumbPage = client.goTo("crumbIssuer/api/json", "application/json");
        JSONObject crumb = JSONObject.fromObject(crumbPage.getWebResponse().getContentAsString());

        WebRequest resumePost = new WebRequest(new URL(j.getURL(), failedBuild.getUrl() + "resume/"), HttpMethod.POST);
        resumePost.setAdditionalHeader(crumb.getString("crumbRequestField"), crumb.getString("crumb"));
        WebResponse response = client.loadWebResponse(resumePost);
        j.waitUntilNoActivity();

        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, response.getStatusCode(),
                "POST /resume/ with a crumb should redirect after scheduling the resumed build");
        assertEquals(2, multi.getLastBuild().getNumber(),
                "POST /resume/ with a crumb should schedule exactly one resumed build");
        MultiJobBuild resumedBuild = multi.getBuildByNumber(2);
        assertNotNull(resumedBuild, "POST /resume/ with a crumb should create build #2");
        assertNotNull(resumedBuild.getAction(MultiJobResumeControl.class),
                "build #2 should carry the resume control action");
    }
}
