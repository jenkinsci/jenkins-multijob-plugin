package com.tikal.jenkins.plugins.reactor.views;

import hudson.Extension;
import hudson.Indenter;
import hudson.Util;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.ListView;
import hudson.model.Project;
import hudson.model.Run;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.RunList;
import hudson.views.ListViewColumn;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.tikal.jenkins.plugins.reactor.ReactorSubProjectConfig;
import com.tikal.jenkins.plugins.reactor.ReactorBuilder;
import com.tikal.jenkins.plugins.reactor.TikalReactorProject;

public class ReactorView extends ListView {

	private DescribableList<ListViewColumn, Descriptor<ListViewColumn>> columns = new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(this,
			ReactorListViewColumn.createDefaultInitialColumnList());

	@DataBoundConstructor
	public ReactorView(String name) {
		super(name);
	}

	public ReactorView(String name, ViewGroup owner) {
		super(name, owner);
	}

	@Override
	public Iterable<ListViewColumn> getColumns() {
		return columns;
	}

	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		public String getDisplayName() {
			return "Reactor View";
		}

		/**
		 * Checks if the include regular expression is valid.
		 */
		public FormValidation doCheckIncludeRegex(@QueryParameter String value) throws IOException, ServletException, InterruptedException {
			String v = Util.fixEmpty(value);
			if (v != null) {
				try {
					Pattern.compile(v);
				} catch (PatternSyntaxException pse) {
					return FormValidation.error(pse.getMessage());
				}
			}
			return FormValidation.ok();
		}
	}

	@Override
	public List<TopLevelItem> getItems() {
		Collection<TopLevelItem> items = Hudson.getInstance().getItems();
		List<TopLevelItem> out = new ArrayList<TopLevelItem>();
		for (TopLevelItem item : items) {
			if (item instanceof TikalReactorProject) {
				TikalReactorProject project = (TikalReactorProject) item;
				if (project.getUpstreamProjects().size() == 0) {
					addSubprojects(null, project, 0, null, out);
				}
			}
		}
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addSubprojects(TikalReactorProject reactorProject, AbstractProject project, int nestLevel, String phaseName, List<TopLevelItem> out) {
		out.add((TopLevelItem) new ProjectWrapper(reactorProject, project, nestLevel, phaseName));
		// out.add((TopLevelItem) project);
		List<Builder> builders = null;
		if (project instanceof TikalReactorProject) {
			builders = ((Project) project).getBuilders();
			reactorProject = (TikalReactorProject) project;
		}
		if (builders == null) {
			return;
		}
		for (Builder builder : builders) {
			if (builder instanceof ReactorBuilder) {
				ReactorBuilder reactorBuilder = (ReactorBuilder) builder;
				List<ReactorSubProjectConfig> subProjects = reactorBuilder.getSubProjects();
				String currentPhaseName = reactorBuilder.getReactorName();
				for (ReactorSubProjectConfig projectConfig : subProjects) {
					TopLevelItem it = Hudson.getInstance().getItem(projectConfig.getJobName());
					if (it instanceof TikalReactorProject) {
						// reactorProject = (TikalReactorProject) it;
					}
					if (it instanceof AbstractProject) {
						AbstractProject p = (AbstractProject) it;
						RunList<Run> list = p.getBuilds();
						for (Run run : list) {
							UpstreamCause cause = (UpstreamCause) run.getCause(UpstreamCause.class);
							if (cause != null) {
								if (cause.getUpstreamProject().equals(project.getName())) {
									// out.add((TopLevelItem) p);
									addSubprojects(reactorProject, p, nestLevel + 1, currentPhaseName, out);
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected void submit(StaplerRequest req) throws ServletException, FormException, IOException {
	}

	protected void initColumns() {
		try {
			Field field = ListView.class.getDeclaredField("columns");
			field.setAccessible(true);
			field.set(this, new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(this, ReactorListViewColumn.createDefaultInitialColumnList()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Indenter<Job> createIndenter() {
		System.out.println("ProjectWrapper.createIndenter()");
		return new Indenter<Job>() {

			protected int getNestLevel(Job job) {
				if ((TopLevelItem) job instanceof ProjectWrapper) {
					ProjectWrapper projectWrapper = (ProjectWrapper) (TopLevelItem) job;
					return projectWrapper.getNestLevel();
				}
				System.out.println("ProjectWrapper.createIndenter().new Indenter<Job>() {...}.getNestLevel()");
				return 0;
			}
		};
	}
}
