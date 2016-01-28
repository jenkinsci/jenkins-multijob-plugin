package com.tikal.jenkins.plugins.multijob.ui;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Describable;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.IOUtils.copy;

public abstract class UIObject implements ExtensionPoint, Action, Describable<UIObject> {

    public String getUrlName() {
        return null;
    }

    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    public String getIconFileName() {
        return getClass().getSimpleName();
    }

    public List<SourceFile> getSourceFiles() {
        List<SourceFile> r = new ArrayList<SourceFile>();

        r.add(new SourceFile(getClass().getSimpleName()+".java"));
        for (String name : new String[]{"index.jelly","index.groovy"}) {
            SourceFile s = new SourceFile(name);
            if (s.resolve()!=null)
                r.add(s);
        }
        return r;
    }

    public void doSourceFile(StaplerRequest req, StaplerResponse rsp) throws IOException {
        String name = req.getRestOfPath().substring(1); // Remove leading /
        for (SourceFile sf : getSourceFiles())
            if (sf.name.equals(name)) {
                sf.doIndex(rsp);
                return;
            }
        rsp.sendError(rsp.SC_NOT_FOUND);
    }

    public abstract String getDescription();

    public UIObjectDescriptor getDescriptor() {
        return (UIObjectDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }


    public static ExtensionList<UIObject> all() {
        return Jenkins.getInstance().getExtensionList(UIObject.class);
    }

    public static List<UIObject> getGroovySamples() {
        List<UIObject> r = new ArrayList<UIObject>();
        for (UIObject uiSample : UIObject.all()) {
            for (SourceFile src : uiSample.getSourceFiles()) {
                if (src.name.contains("groovy")) {
                    r.add(uiSample);
                    break;
                }
            }
        }
        return r;
    }

    public static List<UIObject> getOtherSamples() {
        List<UIObject> r = new ArrayList<UIObject>();
        OUTER:
        for (UIObject uiObject : UIObject.all()) {
            for (SourceFile src : uiObject.getSourceFiles()) {
                if (src.name.contains("groovy")) {
                    continue OUTER;
                }
            }
            r.add(uiObject);
        }
        return r;
    }


    public class SourceFile {
        public final String name;

        public SourceFile(String name) {
            this.name = name;
        }

        public URL resolve() {
            return UIObject.this.getClass().getResource((name.endsWith(".jelly") || name.endsWith(".groovy"))
                    ? UIObject.this.getClass().getSimpleName()+"/"+name : name);
        }

        public void doIndex(StaplerResponse rsp) throws IOException {
            rsp.setContentType("text/plain;charset=UTF-8");
            copy(resolve().openStream(),rsp.getOutputStream());
        }
    }
}
