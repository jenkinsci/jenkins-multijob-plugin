/*
Similar to build-flow-test-aggregator (https://github.com/zeroturnaround/build-flow-test-aggregator)
*/
package com.tikal.jenkins.plugins.multijob;

import jenkins.model.Jenkins;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;

public class MultiJobTestResults extends AggregatedTestResultAction {

    @Override
    protected String getChildName(AbstractTestResultAction tr) {
        return tr.owner.getProject().getFullName();
    }

    @Override
    public AbstractBuild<?, ?> resolveChild(Child child) {
        AbstractProject<?, ?> project = Jenkins.getInstance().getItemByFullName(child.name, AbstractProject.class);
        if (project != null) {
            return project.getBuildByNumber(child.build);
        }
        return null;
    }

    @Override
    protected void add(AbstractTestResultAction child) {
        super.add(child);
    }
}
