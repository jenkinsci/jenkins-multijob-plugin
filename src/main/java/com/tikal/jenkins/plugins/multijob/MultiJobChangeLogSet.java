package com.tikal.jenkins.plugins.multijob;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import java.util.Iterator;

public class MultiJobChangeLogSet extends ChangeLogSet<ChangeLogSet.Entry> {

    protected MultiJobChangeLogSet(AbstractBuild build) {
        super(build);
        // TODO Auto-generated constructor stub
    }

    public Iterator<hudson.scm.ChangeLogSet.Entry> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEmptySet() {
        // TODO Auto-generated method stub
        return false;
    }

    public void addChangeLogSet(ChangeLogSet<? extends Entry> changeLogSet) {
        // TODO Auto-generated method stub

    }
}
