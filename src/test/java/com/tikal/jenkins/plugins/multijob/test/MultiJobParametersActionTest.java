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
import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Tests for {@link MultiJobParametersAction}
 * @author Oleg Nenashev
 */
public class MultiJobParametersActionTest {
    
    @Test
    public void shouldMergeEmptyParameters() {
        MultiJobParametersAction params = new MultiJobParametersAction();
        assertShouldSchedule(params, true);
    }
    
    @Test
    public void shouldMergeEmptyParametersOfDifferentTypes() {
        MultiJobParametersAction params = new MultiJobParametersAction();
        assertShouldSchedule(params, false);
    }
    
    @Test
    @Issue("JENKINS-38850")
    public void shouldMergeSameParameters() {
        MultiJobParametersAction params = new MultiJobParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        assertShouldSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
    }
    
    @Test
    @Issue("JENKINS-38850")
    public void shouldNotMergeDifferentParameters() {
        MultiJobParametersAction params = new MultiJobParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"));
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue2"));
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("C", "bValue"));
        assertShouldNotSchedule(params, new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"), new StringParameterValue("B", "cValue"));
    }
    
    @Test
    @Issue("JENKINS-38850")
    public void shouldNotMergeEmptyParameters() {
        MultiJobParametersAction params1 = new MultiJobParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        MultiJobParametersAction params2 = new MultiJobParametersAction();
        ParametersAction params3 = new ParametersAction(new StringParameterValue("A", "aValue"), new StringParameterValue("B", "bValue"));
        ParametersAction params4 = new ParametersAction();
        
        // Empty MultiJob
        Assert.assertTrue("MultiJob Parameters set should not allow merge with empty MultiJob parameters", params1.shouldSchedule(Collections.<Action>singletonList(params2)));
        Assert.assertTrue("Empty parameters MultiJob should not allow merge with non-empty MultiJob parameter set", params2.shouldSchedule(Collections.<Action>singletonList(params1)));
        
        // Empty ParametersAction
        Assert.assertTrue("Parameters set should not allow merge with empty parameters", params1.shouldSchedule(Collections.<Action>singletonList(params4)));
        Assert.assertTrue("Empty parameters should not allow merge with non-empty MultiJob parameter set", params4.shouldSchedule(Collections.<Action>singletonList(params1)));  
    
        // ParametersAction with empty MultiJob
        Assert.assertTrue("Parameters set should not allow merge with empty MultiJob parameters", params3.shouldSchedule(Collections.<Action>singletonList(params2)));
        Assert.assertTrue("Empty parameters MultiJob should not allow merge with non-empty parameter set", params2.shouldSchedule(Collections.<Action>singletonList(params3)));
        
    }
    
    private static void assertShouldNotSchedule(MultiJobParametersAction current, ParameterValue ... scheduled) 
            throws AssertionError {
        assertShouldNotSchedule(current, false, scheduled);
    }
    
    private static void assertShouldNotSchedule(MultiJobParametersAction current, boolean multiJob, ParameterValue ... scheduled) 
            throws AssertionError {
        ParametersAction toSchedule = multiJob ? new MultiJobParametersAction(scheduled) : new ParametersAction(scheduled);
        Assert.assertTrue("Different parameter sets should not be merged", current.shouldSchedule(Collections.<Action>singletonList(toSchedule))); 
    }
    
    private static void assertShouldSchedule(MultiJobParametersAction current, ParameterValue ... scheduled) 
            throws AssertionError {
        assertShouldSchedule(current, false, scheduled);
    }
    
    private static void assertShouldSchedule(MultiJobParametersAction current, boolean multiJob, ParameterValue ... scheduled) 
            throws AssertionError {
        ParametersAction toSchedule = multiJob ? new MultiJobParametersAction(scheduled) : new ParametersAction(scheduled);
        Assert.assertFalse("Same parameter sets should be merged", current.shouldSchedule(Collections.<Action>singletonList(toSchedule))); 
    }
}
