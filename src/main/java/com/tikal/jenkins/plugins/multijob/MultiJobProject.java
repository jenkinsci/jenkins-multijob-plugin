package com.tikal.jenkins.plugins.multijob;

import java.io.IOException;
import java.util.*;

import com.tikal.jenkins.plugins.multijob.scm.MultiSCM;
import hudson.Extension;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.util.AlternativeUiTextProvider;

import com.tikal.jenkins.plugins.multijob.views.MultiJobView;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;

public class MultiJobProject extends Project<MultiJobProject, MultiJobBuild>
		implements TopLevelItem {


	@SuppressWarnings("rawtypes")
	private MultiJobProject(ItemGroup parent, String name){
		super(parent, name);
	}

	public MultiJobProject(Hudson parent, String name){
		super(parent, name);
	}

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {

        Map<AbstractProject,SCM> project2scm = new HashMap<AbstractProject,SCM>();

        MultiSCM multiSCM = new MultiSCM(null);
        setScm(multiSCM);

        //extract the submitted phases
        JSONObject json = req.getSubmittedForm();
        if(json.has("builder")) {

            JSON ret = (JSON)json.get("builder");

            JSONArray jsonArray = Utils.getCreateJSONArray(ret);

            //add SCMS
            for (int i = 0; i < jsonArray.size(); i++) {
                if(jsonArray.getJSONObject(i).has("phaseJobs")) {

                    JSON phaseJobsEntry = (JSON)jsonArray.getJSONObject(i).get("phaseJobs");

                    JSONArray phases = Utils.getCreateJSONArray(phaseJobsEntry);
                    for(Object phase : phases){
                        JSONObject pahseJson = (JSONObject)phase;

                        if(pahseJson.getBoolean("exposedSCM")){
                            String pahseJobName = pahseJson.getString("jobName");
                            AbstractProject project = ((AbstractProject) Hudson.getInstance().getItem(pahseJobName));
                            multiSCM.addScm(project.getName(),project.getScm());
                        }
                    }
                }
            }
        }
        super.submit(req, rsp);
    }

    @Override
    public void setScm(SCM scm) throws IOException {
        if(scm instanceof MultiSCM)
            super.setScm(scm);
    }

    @Override
	protected Class<MultiJobBuild> getBuildClass() {
		return MultiJobBuild.class;
	}

	@Override
	public Hudson getParent() {
		return Hudson.getInstance();
	}
	
        @Override
        public String getPronoun() {
            return AlternativeUiTextProvider.get(PRONOUN, this, getDescriptor().getDisplayName());
        }

	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension(ordinal = 1000)
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends AbstractProjectDescriptor {
		public String getDisplayName() {
			return "MultiJob Project";
		}

		@SuppressWarnings("rawtypes")
		public MultiJobProject newInstance(ItemGroup itemGroup, String name) {
            return new MultiJobProject(itemGroup, name);
		}
	}

	@Override
	protected void buildDependencyGraph(DependencyGraph graph) {
		super.buildDependencyGraph(graph);
	}

	public boolean isTopMost() {
		return getUpstreamProjects().size() == 0;
	}

	public MultiJobView getView() {
		MultiJobView view = new MultiJobView("");
		return view;
	}

	public String getRootUrl() {
		return Hudson.getInstance().getRootUrl();
	}
}
