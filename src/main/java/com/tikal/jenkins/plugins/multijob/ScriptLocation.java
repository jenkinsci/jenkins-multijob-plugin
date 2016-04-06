package com.tikal.jenkins.plugins.multijob;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

public class ScriptLocation {

    private boolean isUseFile;
    private String scriptText;
    private String scriptPath;
    private boolean isScriptOnSlave;

    @DataBoundConstructor
    public ScriptLocation(String value, String scriptText, String scriptPath, Boolean isScriptOnSlave) {
        this.isUseFile = null == value ? false : Boolean.parseBoolean(value);
        this.scriptText = Util.fixNull(scriptText);
        this.scriptPath = Util.fixNull(scriptPath);
        this.isScriptOnSlave = null == isScriptOnSlave ? false : isScriptOnSlave;
    }

    public boolean isUseFile() {
        return isUseFile;
    }

    public void setUseFile(String value) {
        this.isUseFile = null == value ? false : Boolean.parseBoolean(value);
    }

    public void setUseFile(boolean isUseFile) {
        this.isUseFile = isUseFile;
    }

    public String getScriptText() {
        return scriptText;
    }

    public void setScriptText(String scriptText) {
        this.scriptText = scriptText;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public boolean isScriptOnSlave() {
        return isScriptOnSlave;
    }

    public void setScriptOnSlave(boolean isScriptOnSlave) {
        this.isScriptOnSlave = isScriptOnSlave;
    }

}
