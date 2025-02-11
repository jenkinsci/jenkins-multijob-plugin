/*
 * The MIT License
 *
 * Copyright (c) 2017 CloudBees, Inc.
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
package com.tikal.jenkins.plugins.multijob.test;

import com.tikal.jenkins.plugins.multijob.MultiJobParametersAction;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MultiJobParametersAction}
 *
 * @author Oleg Nenashev
 */
class MultiJobParametersActionTest {

    @Test
    void shouldMergeEmptyParameters() {
        MultiJobParametersAction params = new MultiJobParametersAction();
        assertShouldSchedule(params, true);
    }

    @Test
    void shouldMergeEmptyParametersOfDifferentTypes() {
        MultiJobParametersAction params = new MultiJobParametersAction();
        assertShouldSchedule(params, false);
    }

    @Test
    @Issue("JENKINS-38850")
    void shouldMergeSameParameters() {
        MultiJobParametersAction params = new MultiJobParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        assertShouldSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
    }

    @Test
    @Issue("JENKINS-38850")
    void shouldNotMergeDifferentParameters() {
        MultiJobParametersAction params = new MultiJobParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));

        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"));
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue2"));
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("C", "bValue"));
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"), new StringParameterValue("B", "cValue"));
    }

    @Test
    @Issue("JENKINS-38850")
    void shouldNotMergeEmptyParameters() {
        MultiJobParametersAction params1 = new MultiJobParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        MultiJobParametersAction params2 = new MultiJobParametersAction();
        ParametersAction params3 = new ParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        ParametersAction params4 = new ParametersAction();

        // Empty MultiJob
        assertTrue(params1.shouldSchedule(Collections.singletonList(params2)), "MultiJob Parameters set should not allow merge with empty MultiJob parameters");
        assertTrue(params2.shouldSchedule(Collections.singletonList(params1)), "Empty parameters MultiJob should not allow merge with non-empty MultiJob parameter set");

        // Empty ParametersAction
        assertTrue(params1.shouldSchedule(Collections.singletonList(params4)), "Parameters set should not allow merge with empty parameters");
        assertTrue(params4.shouldSchedule(Collections.singletonList(params1)), "Empty parameters should not allow merge with non-empty MultiJob parameter set");

        // ParametersAction with empty MultiJob
        assertTrue(params3.shouldSchedule(Collections.singletonList(params2)), "Parameters set should not allow merge with empty MultiJob parameters");
        assertTrue(params2.shouldSchedule(Collections.singletonList(params3)), "Empty parameters MultiJob should not allow merge with non-empty parameter set");

    }

    private static void assertShouldNotSchedule(MultiJobParametersAction current, ParameterValue... scheduled)
            throws AssertionError {
        assertShouldNotSchedule(current, false, scheduled);
    }

    private static void assertShouldNotSchedule(MultiJobParametersAction current, boolean multiJob, ParameterValue... scheduled)
            throws AssertionError {
        ParametersAction toSchedule = multiJob ? new MultiJobParametersAction(scheduled) : new ParametersAction(scheduled);
        assertTrue(current.shouldSchedule(Collections.singletonList(toSchedule)), "Different parameter sets should not be merged");
    }

    private static void assertShouldSchedule(MultiJobParametersAction current, ParameterValue... scheduled)
            throws AssertionError {
        assertShouldSchedule(current, false, scheduled);
    }

    private static void assertShouldSchedule(MultiJobParametersAction current, boolean multiJob, ParameterValue... scheduled)
            throws AssertionError {
        ParametersAction toSchedule = multiJob ? new MultiJobParametersAction(scheduled) : new ParametersAction(scheduled);
        assertFalse(current.shouldSchedule(Collections.singletonList(toSchedule)), "Same parameter sets should be merged");
    }
}
