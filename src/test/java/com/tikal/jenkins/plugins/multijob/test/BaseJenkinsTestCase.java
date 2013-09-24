package com.tikal.jenkins.plugins.multijob.test;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import hudson.model.Hudson;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: liorb
 * Date: 9/18/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class BaseJenkinsTestCase {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * configure jenkins for access
     */
    @Before
    public void setup() throws IOException {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Hudson.ADMINISTER, "admin");
        j.jenkins.setAuthorizationStrategy(authorizationStrategy);
    }

    protected JenkinsRule.WebClient getLoggedInWebClient() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        //webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.login("admin");
        return webClient;
    }
}
