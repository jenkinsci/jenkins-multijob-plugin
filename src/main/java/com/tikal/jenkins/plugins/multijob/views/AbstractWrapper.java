package com.tikal.jenkins.plugins.multijob.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hudson.model.*;
import org.acegisecurity.AccessDeniedException;

import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.security.ACL;
import hudson.security.Permission;

import javax.annotation.Nonnull;

abstract public class AbstractWrapper implements TopLevelItem {

    protected final int nestLevel;
    protected final Job project;
    protected final String id;
    protected final List<AbstractWrapper> childWrappers = new ArrayList<>();

    public AbstractWrapper(Job project, int nestLevel, int index) {
        this.project = project;
        this.nestLevel = nestLevel;
        this.id = "@" + index;
    }

    @Nonnull
    @Override
    public ACL getACL() {
        return project.getACL();
    }

    public int getNestLevel() {
        return nestLevel;
    }

    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
    }

    public void onCopiedFrom(Item src) {
    }

    public void onCreatedFromScratch() {
    }

    public void save() throws IOException {
    }

    public void delete() throws IOException, InterruptedException {
    }

    public void checkPermission(Permission permission) throws AccessDeniedException {
    }

    public String getUrl() {
        return null;
    }

    public String getShortUrl() {
        return null;
    }

    @Deprecated
    public String getAbsoluteUrl() {
        return project.getAbsoluteUrl();
    }

    public File getRootDir() {
        return null;
    }

    public Search getSearch() {
        return null;
    }

    public String getSearchName() {
        return null;
    }

    public String getSearchUrl() {
        return null;
    }

    public SearchIndex getSearchIndex() {
        return null;
    }

    public boolean hasPermission(Permission permission) {
        return true;
    }

    public Hudson getParent() {
        return Hudson.getInstance();
    }

    public TopLevelItemDescriptor getDescriptor() {
        return null;
    }

    public HealthReport getBuildHealth() {
        return null;
    }

    public List<HealthReport> getBuildHealthReports() {
        return null;
    }

    public boolean isBuildable() {
        return false;
    }

    public void addChildWrapper(AbstractWrapper childWrapper) {
        childWrappers.add(childWrapper);
    }

    public String getDirectChilds() {
        StringBuilder directChilds = new StringBuilder();
        for (AbstractWrapper childWrapper : childWrappers) {
            if (directChilds.length() != 0) {
                directChilds.append(",");
            }
            directChilds.append(childWrapper.getId());
        }
        return directChilds.toString();
    }

    public String getChildIds() {
        StringBuilder childIds = new StringBuilder();
        getChildIds01(childWrappers, childIds);
        return childIds.toString();
    }

    public void getChildIds01(List<AbstractWrapper> subWrapper, StringBuilder childIds) {
        for (AbstractWrapper childWrapper : subWrapper) {
            if (childIds.length() != 0) {
                childIds.append(",");
            }
            childIds.append(childWrapper.getId());

            if (childWrapper.childWrappers.size() > 0) {
                getChildIds01(childWrapper.childWrappers, childIds);
            }
        }
    }

    public String getId() {
        return id;
    }
}
