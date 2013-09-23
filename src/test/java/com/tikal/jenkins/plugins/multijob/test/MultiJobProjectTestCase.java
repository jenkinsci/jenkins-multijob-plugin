package com.tikal.jenkins.plugins.multijob.test;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.scm.MultiSCM;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import org.junit.Test;
import org.junit.Assert;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.List;

public class MultiJobProjectTestCase extends BaseJenkinsTestCase {


    //@Test
    //@LocalData
    public void testAddSimpleScmPage() throws Exception {
        AbstractProject simpleScmProject = (AbstractProject) j.jenkins.getItem("simple_git_project");

        addPhaseProject(simpleScmProject);
        MultiJobProject multijobProject = (MultiJobProject) j.jenkins.getItem("multijob_project");

        List<Builder> builders = multijobProject.getBuilders();
        Assert.assertTrue(builders.size()==1);
        MultiJobBuilder multiJobBuilder = (MultiJobBuilder)builders.get(0);
        Assert.assertTrue((multiJobBuilder.getPhaseName().equals(simpleScmProject.getName())));
        
        MultiSCM multiSCM = (MultiSCM)multijobProject.getScm();
        Assert.assertTrue(multiSCM.getConfiguredSCMs().size()==1);
    }
//    TODO: fix this test, see how the delete button can be properly invoked
    //@Test
    //@LocalData
    public void testRemoveScmProject() throws Exception {

        AbstractProject simpleScmProject = (AbstractProject) j.jenkins.getItem("simple_scm_project");

        addPhaseProject(simpleScmProject);
        JenkinsRule.WebClient webClient = getLoggedInWebClient();
        HtmlPage configPage = webClient.goTo("job/multijob_project/configure");
        HtmlForm configForm = configPage.getFormByName("config");

        HtmlButton deleteButton = (HtmlButton)configPage.getElementById("yui-gen18-button");
        HtmlPage after = deleteButton.click();
        webClient.waitForBackgroundJavaScript(1000);

        HtmlButton submitButton = configForm.getButtonByCaption("Save");
        configForm.submit(submitButton);
        MultiJobProject multijobProject = (MultiJobProject) j.jenkins.getItem("multijob_project");
        MultiSCM multiSCM = (MultiSCM) multijobProject.getScm();
        Assert.assertTrue(multiSCM.getConfiguredSCMs().size()==0);
    }

    private void addPhaseProject(AbstractProject project) throws Exception {

        JenkinsRule.WebClient webClient = getLoggedInWebClient();
        HtmlPage configPage = webClient.goTo("job/multijob_project/configure");
        HtmlForm configForm = configPage.getFormByName("config");

        HtmlTextInput phaseNameInput = (HtmlTextInput)configForm.getInputsByName("phaseName").get(0);
        HtmlTextInput jobNameInput = (HtmlTextInput)configForm.getInputsByName("_.jobName").get(0);

        phaseNameInput.setValueAttribute(project.getName());
        jobNameInput.setValueAttribute(project.getName());

        HtmlButton submitButton = configForm.getButtonByCaption("Save");
        configForm.submit(submitButton);
    }
}
