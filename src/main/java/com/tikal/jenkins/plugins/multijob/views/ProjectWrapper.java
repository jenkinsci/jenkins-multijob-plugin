package com.tikal.jenkins.plugins.multijob.views;

import hudson.model.BallColor;
import hudson.model.HealthReport;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.search.SearchIndex;
import hudson.search.Search;
import hudson.security.ACL;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.acegisecurity.AccessDeniedException;

import com.tikal.jenkins.plugins.multijob.MultiJobProject;

@SuppressWarnings("rawtypes")
public class ProjectWrapper implements TopLevelItem {

	final MultiJobProject multijob;

	final BuildState buildState;

	final AbstractProject project;

	final int nestLevel;

	final String phaseName;

	public ProjectWrapper(MultiJobProject multijob, AbstractProject project, BuildState buildState, int nestLevel, String phaseName) {
		this.project = project;
		this.multijob = multijob;
		this.nestLevel = nestLevel;
		this.phaseName = phaseName;
		this.buildState = buildState;
	}

	@SuppressWarnings("unchecked")
	public Collection<? extends Job> getAllJobs() {
		return project.getAllJobs();
	}

	public String getName() {
		return project.getName();
	}

	public String getFullName() {
		return project.getFullName();
	}

	public String getDisplayName() {
		return project.getDisplayName();
	}

	public String getFullDisplayName() {
		return project.getFullDisplayName();
	}

	public String getUrl() {
		return project.getUrl();
	}

	public String getShortUrl() {
		return project.getShortUrl();
	}

	@Deprecated
	public String getAbsoluteUrl() {
		return project.getAbsoluteUrl();
	}

	@SuppressWarnings("unchecked")
	public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
		project.onLoad(parent, name);
	}

	public void onCopiedFrom(Item src) {
		project.onCopiedFrom(src);
	}

	public void onCreatedFromScratch() {
		project.onCreatedFromScratch();
	}

	public void save() throws IOException {
		project.save();
	}

	public void delete() throws IOException, InterruptedException {
		project.delete();
	}

	public File getRootDir() {
		return project.getRootDir();
	}

	public Search getSearch() {
		return project.getSearch();
	}

	public String getSearchName() {
		return project.getSearchName();
	}

	public String getSearchUrl() {
		return project.getSearchUrl();
	}

	public SearchIndex getSearchIndex() {
		return project.getSearchIndex();
	}

	public ACL getACL() {
		return project.getACL();
	}

	public void checkPermission(Permission permission) throws AccessDeniedException {
		project.checkPermission(permission);
	}

	public boolean hasPermission(Permission permission) {
		return project.hasPermission(permission);
	}

	public Hudson getParent() {
		return Hudson.getInstance();
	}

	public int getNestLevel() {
		return nestLevel;
	}

	public TopLevelItemDescriptor getDescriptor() {
		return (TopLevelItemDescriptor) project.getDescriptorByName(project.getClass().getName());
	}

	Run findLastBuildForResult(Result result) {
		if (Result.SUCCESS.equals(result)) {
			return project.getBuildByNumber(buildState.getLastSuccessBuildNumber());
		}
		if (Result.FAILURE.equals(result)) {
			return project.getBuildByNumber(buildState.getLastFailureBuildNumber());
		}
		return project.getBuildByNumber(buildState.getLastBuildNumber());
	}

	public Run getLastFailedBuild() {
		return findLastBuildForResult(Result.FAILURE);
	}

	public Run getLastSuccessfulBuild() {
		return findLastBuildForResult(Result.SUCCESS);
	}

	public Run getLastBuild() {
		return findLastBuildForResult(null);
	}

	public AbstractProject getProject() {
		return project;
	}

	public BallColor getIconColor() {
		if (project.isDisabled())
			return BallColor.DISABLED;
		Run lastBuild = getLastBuild();
		while (lastBuild != null && lastBuild.hasntStartedYet())
			lastBuild = lastBuild.getPreviousBuild();

		if (lastBuild != null)
			return lastBuild.getIconColor();
		else
			return BallColor.GREY;
	}

	public String getCss() {
		return "padding-left: " + (getNestLevel() + 1) * 2 + "em";
	}

	public String getRelativeShift() {
		int i = getNestLevel();
		if (i == 0)
			return null;
		return "position:relative; left: " + i * 2 + "em";
	}

	public HealthReport getBuildHealth() {
		return getProject().getBuildHealth();
	}

	@SuppressWarnings("unchecked")
	public List<HealthReport> getBuildHealthReports() {
		return getProject().getBuildHealthReports();
	}

	public boolean isBuildable() {
		return multijob == null && getProject().isBuildable();
	}

	public String getPhaseName() {
		return phaseName;
	}

}
