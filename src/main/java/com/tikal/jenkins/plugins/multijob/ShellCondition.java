/*
 * The MIT License
 *
 * Copyright (C) 2012 by Chris Johnson
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
package com.tikal.jenkins.plugins.multijob;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.tasks.Shell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Executes a series of commands by using a shell.
 *
 * @author Chris Johnson (modifed by Brandon Turner)
 */
public class ShellCondition {

    /**
     * Command to execute.
     */
    protected final String command;

    public ShellCondition(String command) {
        this.command = fixCrLf(command);
    }

    public final String getCommand() {
        return command;
    }

    public boolean runPerform(AbstractBuild<?, ?> build, BuildListener listener) throws InterruptedException {
        FilePath ws = build.getWorkspace();
        FilePath script = null;

        try {
            Launcher launcher = null;

            try {
                launcher = ws.createLauncher(listener);
            }
            catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace(listener.fatalError("Unable to create Launcher in " + ws));
                return false;
            }

            try {
                script = createScriptFile(ws);
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace(listener.fatalError("Unable to produce a script file"));
                return false;
            }

            int r;
            try {
                EnvVars envVars = build.getEnvironment(listener);
                // on Windows environment variables are converted to all upper case,
                // but no such conversions are done on Unix, so to make this cross-platform,
                // convert variables to all upper cases.
                for(Map.Entry<String,String> e : build.getBuildVariables().entrySet())
                    envVars.put(e.getKey(),e.getValue());

                r = launcher.launch().cmds(buildCommandLine(script)).envs(envVars).stdout(listener).pwd(ws).join();

            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace(listener.fatalError("command execution failed"));
                r = -1;
            }
            return r==0;
        } finally {
            try {
                if(script!=null)
                script.delete();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("Unable to delete script file " + script) );
            }
        }
    }


    /**
     * Creates a script file in a temporary name in the specified directory.
     */
    public FilePath createScriptFile(FilePath dir) throws IOException, InterruptedException {
        return dir.createTextTempFile("ShellCondition", getFileExtension(), getContents(), false);
    }

    /**
     * Fix CR/LF and always make it Unix style.
     */
    private static String fixCrLf(String s) {
        // eliminate CR
        int idx;
        while((idx=s.indexOf("\r\n"))!=-1)
            s = s.substring(0,idx)+s.substring(idx+1);
        return s;
    }

    /**
     * Older versions of bash have a bug where non-ASCII on the first line
     * makes the shell think the file is a binary file and not a script. Adding
     * a leading line feed works around this problem.
     */
    private static String addCrForNonASCII(String s) {
        if(!s.startsWith("#!")) {
            if (s.indexOf('\n')!=0) {
                return "\n" + s;
            }
        }

        return s;
    }

    public String[] buildCommandLine(FilePath script) {
        if(command.startsWith("#!")) {
            // interpreter override
            int end = command.indexOf('\n');
            if(end<0)   end=command.length();
            List<String> args = new ArrayList<String>();
            args.addAll(Arrays.asList(Util.tokenize(command.substring(0,end).trim())));
            args.add(script.getRemote());
            args.set(0,args.get(0).substring(2));   // trim off "#!"
            return args.toArray(new String[args.size()]);
        } else {
            hudson.tasks.Shell.DescriptorImpl shellDesc = Hudson.getInstance().getDescriptorByType(Shell.DescriptorImpl.class);

            return new String[] { shellDesc.getShellOrDefault(script.getChannel()), "-xe", script.getRemote()};
        }
    }

    protected String getContents() {
        return addCrForNonASCII(fixCrLf(command));
    }

    protected String getFileExtension() {
        return ".sh";
    }
}
