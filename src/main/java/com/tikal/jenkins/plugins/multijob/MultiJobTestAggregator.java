/*
Similar to build-flow-test-aggregator (https://github.com/zeroturnaround/build-flow-test-aggregator)
*/
package com.tikal.jenkins.plugins.multijob;

import java.util.concurrent.ExecutionException;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.Run;
import hudson.model.BuildListener;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.Child;


public class MultiJobTestAggregator {
    public static void aggregateResultsFromBuild(Run subbuild, MultiJobTestResults testResults, BuildListener listener) throws ExecutionException, InterruptedException {
        if (subbuild == null) {
            return;
        }
        listener.getLogger().println("Starting to gather test results!");

        if (subbuild instanceof MatrixBuild) {
            aggregateResultsFromMatrixJob((MatrixBuild) subbuild, testResults, listener);
        } else if (subbuild instanceof MavenModuleSetBuild) {
            aggregateResultsFromMavenMultiModuleJob(subbuild, testResults, listener);
        } else {
            addTestResultFromBuild(subbuild, testResults, listener);
        }
    }

    private static void aggregateResultsFromMatrixJob(MatrixBuild run, MultiJobTestResults testResults, BuildListener listener) {
        listener.getLogger().println("Going to gather results from matrix job " + run);
        for (MatrixRun matrixRun : run.getRuns()) {
            addTestResultFromBuild(matrixRun, testResults, listener);
        }
    }

    private static void aggregateResultsFromMavenMultiModuleJob(Run<?, ?> subbuild, MultiJobTestResults testResults, BuildListener listener) {
        listener.getLogger().println("Going to gather results from Maven multi module job " + subbuild);
        SurefireAggregatedReport aggregatedTestReport = subbuild.getAction(hudson.maven.reporters.SurefireAggregatedReport.class);
        if (aggregatedTestReport != null) {
            listener.getLogger().println("Adding test result for job " + subbuild);
            for (Child child : aggregatedTestReport.children) {
                TestResultAction testResult = aggregatedTestReport.getChildReport(child);
                testResults.add(testResult);
            }
        }
    }

    private static void addTestResultFromBuild(Run subbuild, MultiJobTestResults testResults, BuildListener listener) {
        TestResultAction testResult = subbuild.getAction(hudson.tasks.junit.TestResultAction.class);
        if (testResult != null) {
            listener.getLogger().println("Adding test result for job " + subbuild);
            testResults.add(testResult);
        }
    }
}
