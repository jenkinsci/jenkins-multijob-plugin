package com.tikal.jenkins.plugins.multijob.ui;

import hudson.model.Descriptor;

public class UIObjectDescriptor extends Descriptor<UIObject> {

    @Override
    public String getDisplayName() {
        return clazz.getSimpleName();
    }
}
