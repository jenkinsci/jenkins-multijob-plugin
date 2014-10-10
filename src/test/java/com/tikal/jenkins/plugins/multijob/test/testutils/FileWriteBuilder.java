/*
 * The MIT License
 * 
 * Copyright (c) 2014 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.tikal.jenkins.plugins.multijob.test.testutils;

import java.io.IOException;
import java.nio.charset.Charset;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

/**
 * Builder to write a file.
 */
public class FileWriteBuilder extends Builder {
    private final String filename;
    private final String content;
    private final String encoding;

    /**
     * @param filename variables will be expanded
     * @param content variables will be expanded
     * @param encoding
     */
    public FileWriteBuilder(String filename, String content, String encoding) {
        this.filename = filename;
        this.content = content;
        this.encoding = encoding;
    }

    /**
     * @param filename variables will be expanded
     * @param content variables will be expanded
     */
    public FileWriteBuilder(String filename, String content) {
        this(filename, content, Charset.defaultCharset().name());
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        EnvVars envVars = build.getEnvironment(listener);
        String expandedFilename = envVars.expand(filename);
        String expandedContent = envVars.expand(content);

        FilePath file = build.getWorkspace().child(expandedFilename);
        file.write(expandedContent, encoding);
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "File Write Builder";
        }
    }
}
