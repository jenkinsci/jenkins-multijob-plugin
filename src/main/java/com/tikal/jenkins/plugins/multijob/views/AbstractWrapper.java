package com.tikal.jenkins.plugins.multijob.views;

import java.io.File;
import java.io.IOException;
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

    public AbstractWrapper(Job project, int nestLevel) {
        this.project = project;
        this.nestLevel = nestLevel;
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

}
