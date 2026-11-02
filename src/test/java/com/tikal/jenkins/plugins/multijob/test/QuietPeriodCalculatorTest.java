package com.tikal.jenkins.plugins.multijob.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tikal.jenkins.plugins.multijob.QuietPeriodCalculator;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class QuietPeriodCalculatorTest {
    private static final String CALC = "index < 5 ? 0 : 2 * 60";
    private final QuietPeriodCalculator calculator = new QuietPeriodCalculator();

    @Test
    void testCalculate_StaticScript(JenkinsRule j) {
        assertEquals(1, calculator.calculate("1", 1));
        assertEquals(2, calculator.calculate("2", 1));
    }

    @Test
    void testCalculate_CalculatingScript(JenkinsRule j) {
        assertEquals(0, calculator.calculate(CALC, 1));
        assertEquals(120, calculator.calculate(CALC, 5));
    }

    @Test
    void testCalculate_ScriptErrorReturnsResult(JenkinsRule j) {
        assertEquals(0, calculator.calculate("invalid script", 1));
    }

    @Test
    void testCalculateOrThrow_ScriptErrorThrows(JenkinsRule j) {
        assertThrows(RuntimeException.class, () -> calculator.calculateOrThrow("invalid script", 1));
    }

    @Test
    void testCalculate_NegativeIndexThrows(JenkinsRule j) {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(CALC, -1));
    }

    @Test
    void testCalculateOrThrow_NegativeIndexThrows(JenkinsRule j) {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateOrThrow(CALC, -1));
    }
}
