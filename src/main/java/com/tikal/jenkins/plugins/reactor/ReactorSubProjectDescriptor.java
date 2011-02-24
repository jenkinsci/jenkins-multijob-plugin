package com.tikal.jenkins.plugins.reactor;

import hudson.model.Descriptor;


public abstract class ReactorSubProjectDescriptor extends Descriptor<ReactorSubProjectConfig> {
    @Override
    public String getDisplayName() {
        return clazz.getSimpleName();
    }
}
