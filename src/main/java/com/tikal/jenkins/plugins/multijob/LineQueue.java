package com.tikal.jenkins.plugins.multijob;

public final class LineQueue {
    private boolean errorFound;

    public LineQueue(boolean errorFound) {
        this.errorFound = errorFound;
    }

    public boolean hasError() {
        return errorFound;
    }
}