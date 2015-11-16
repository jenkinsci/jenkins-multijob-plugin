package com.tikal.jenkins.plugins.multijob.views;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.acegisecurity.AccessDeniedException;

import hudson.model.HealthReport;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.security.ACL;
import hudson.security.Permission;

abstract public class AbstractWrapper implements TopLevelItem {
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
        return null;
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

    public ACL getACL() {
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
