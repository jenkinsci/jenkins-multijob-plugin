package com.tikal.jenkins.plugins.multijob.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.QuietPeriodCalculator;
import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import jenkins.model.Jenkins;
import jenkins.util.BuildListenerAdapter;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@Issue("SECURITY-3823")
@WithJenkins
class GroovyScriptSecurityTest {
    private static final String QUIET_PERIOD_GROOVY_FIELD = "quietPeriodGroovy";

    @Test
    void conditionScriptHandlesSimpleBooleanExpression(JenkinsRule j) throws Exception {
        MultiJobBuilder builder = builder();
        FreeStyleBuild build = j.buildAndAssertSuccess(j.createFreeStyleProject());

        assertTrue(builder.evalCondition("return true", build, BuildListenerAdapter.wrap(TaskListener.NULL)));
        assertFalse(builder.evalCondition("return false", build, BuildListenerAdapter.wrap(TaskListener.NULL)));
    }

    @Test
    void conditionScriptRejectsUnsafeScript(JenkinsRule j) throws Exception {
        MultiJobBuilder builder = builder();
        FreeStyleBuild build = j.buildAndAssertSuccess(j.createFreeStyleProject());

        assertThrows(
                RejectedAccessException.class,
                () -> builder.evalCondition(
                        "System.err.println('pwned'); return true",
                        build,
                        BuildListenerAdapter.wrap(TaskListener.NULL)));
    }

    @Test
    void quietPeriodScriptHandlesSimpleExpression(JenkinsRule j) {
        assertEquals(120, new QuietPeriodCalculator().calculateOrThrow("index < 5 ? 0 : 2 * 60", 5));
    }

    @Test
    void quietPeriodScriptRejectsUnsafeScript(JenkinsRule j) {
        assertThrows(
                RejectedAccessException.class,
                () -> new QuietPeriodCalculator().calculateOrThrow("System.err.println('quiet'); return 5", 1));
    }

    @Test
    void quietPeriodFormValidationAcceptsPostAndRejectsGet(JenkinsRule j) throws Exception {
        JenkinsRule.WebClient client = j.createWebClient();
        String path = quietPeriodValidationPath(client, "index < 5 ? 0 : 2 * 60");

        WebRequest post = new WebRequest(new URL(j.getURL(), path), HttpMethod.POST);
        Page response = client.getPage(client.addCrumb(post));

        assertEquals(HttpServletResponse.SC_OK, response.getWebResponse().getStatusCode());
        String content = response.getWebResponse().getContentAsString();
        assertTrue(content.contains("Calculated quiet period:"));
        assertTrue(content.contains("Index 5: quiet period=120"));

        client.withThrowExceptionOnFailingStatusCode(false);
        WebRequest get = new WebRequest(new URL(j.getURL(), path), HttpMethod.GET);
        response = client.getPage(get);

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getWebResponse().getStatusCode());
        assertFalse(response.getWebResponse().getContentAsString().contains("Calculated quiet period:"));
    }

    @Test
    void quietPeriodFormValidationDoesNotEvaluateScriptsForUsersWithoutAdminister(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Jenkins.READ).everywhere().to("alice"));

        JenkinsRule.WebClient client = j.createWebClient().login("alice");
        String path = quietPeriodValidationPath(client, "throw new RuntimeException('should not run')");
        WebRequest post = new WebRequest(new URL(j.getURL(), path), HttpMethod.POST);

        Page response = client.getPage(client.addCrumb(post));

        assertEquals(HttpServletResponse.SC_OK, response.getWebResponse().getStatusCode());
        String content = response.getWebResponse().getContentAsString();
        assertFalse(content.contains("Script error:"));
        assertFalse(content.contains("should not run"));
        assertFalse(content.contains("Calculated quiet period:"));
    }

    private static MultiJobBuilder builder() {
        return new MultiJobBuilder(
                "phase",
                Collections.emptyList(),
                MultiJobBuilder.ContinuationCondition.SUCCESSFUL,
                MultiJobBuilder.ExecutionType.PARALLEL,
                "0");
    }

    private static String quietPeriodValidationPath(JenkinsRule.WebClient client, String value) throws Exception {
        String checkUrl = client.executeOnServer(() -> Jenkins.get()
                .getDescriptorByType(MultiJobBuilder.DescriptorImpl.class)
                .getCheckMethod(QUIET_PERIOD_GROOVY_FIELD)
                .toStemUrl());
        return checkUrl + (checkUrl.endsWith("/") ? "?" : "/?") + "value="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
