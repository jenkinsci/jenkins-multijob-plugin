package com.tikal.jenkins.plugins.multijob.views;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

public class TableProperty extends UserProperty {

    private Map<String, Boolean> columns;
    private long timestamp;

    @Extension
    public static final TablePropertyDescriptor DESCRIPTOR = new TablePropertyDescriptor();

    @DataBoundConstructor
    public TableProperty(Map<String, Boolean> columns) {
        this.columns = columns;
    }

    public Map<String, Boolean> getColumnProps() {
        return columns;
    }

    public void setColumnProps(Map<String, Boolean> columns) {
        this.columns = columns;
    }

    public boolean isShowColumn(String key) {
        Boolean res = columns.get(key);
        if (null == res) {
            res = false;
        }
        return res;
    }

    public void setColumnVisible(String key, boolean isVisible) {
        columns.put(key, isVisible);
        timestamp = System.nanoTime();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public UserPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final class TablePropertyDescriptor extends UserPropertyDescriptor {

        @Override
        public TableProperty newInstance(User user) {
            Map<String, Boolean> ret = new HashMap<String, Boolean>();
            ret.put("job", true);
            ret.put("status", true);
            ret.put("weather", true);
            ret.put("build", true);
            ret.put("last-success", true);
            ret.put("last-failure", true);
            ret.put("last-duration", true);
            ret.put("console", true);
            ret.put("run", true);
            return new TableProperty(ret);
        }

        @Override
        public String getDisplayName() {
            return "MultiJob table properties";
        }
    }
}
