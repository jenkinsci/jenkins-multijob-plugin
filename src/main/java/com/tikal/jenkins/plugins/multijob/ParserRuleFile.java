package com.tikal.jenkins.plugins.multijob;

public class ParserRuleFile {

    private String name = null;
    private String path = null;

    public ParserRuleFile() {
        // Empty constructor
    }

    public ParserRuleFile(final String name, final String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPath(final String path) {
        this.path = path;
    }

}
