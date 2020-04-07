package com.tikal.jenkins.plugins.multijob.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.tikal.jenkins.plugins.multijob.QuietPeriodCalculator;

public class QuietPeriodCalculatorTest {
	private static final String CALC = "index < 5 ? 0 : 2 * 60";
	private final QuietPeriodCalculator calculator = new QuietPeriodCalculator();

	@Test
	public void testCalculate_StaticScript() {
		assertEquals(1, calculator.calculate("1", 1));
		assertEquals(2, calculator.calculate("2", 1));
	}

	@Test
	public void testCalculate_CalculatingScript() {
		assertEquals(0, calculator.calculate(CALC, 1));
		assertEquals(120, calculator.calculate(CALC, 5));
	}

	@Test
	public void testCalculate_ScriptErrorReturnsResult() {
		assertEquals(0, calculator.calculate("invalid script", 1));
	}

	@Test
	public void testCalculateOrThrow_ScriptErrorThrows() {
		try {
			calculator.calculateOrThrow("invalid script", 1);
			fail("Exception expected");
		} catch (RuntimeException re) {
			// expected
		}
	}

	@Test
	public void testCalculate_NegativeIndexThrows() {
		try {
			calculator.calculate(CALC, -1);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException iae) {
			// expected
		}
	}
	@Test
	public void testCalculateOrThrow_NegativeIndexThrows() {
		try {
			calculator.calculateOrThrow(CALC, -1);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException iae) {
			// expected
		}
	}
}
