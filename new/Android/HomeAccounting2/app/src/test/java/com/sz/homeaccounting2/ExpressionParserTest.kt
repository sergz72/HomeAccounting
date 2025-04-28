package com.sz.homeaccounting2

import org.junit.Test

import org.junit.Assert.*

data class TestData(val input: String, val result: Double, val errorMessage: String?)
class ExpressionParserTest {
    companion object {
        private val tests: Array<TestData>  = arrayOf(
            TestData("1", 1.0, null),
            TestData("2.34567890", 2.34567890, null),
            TestData("-2.34567890", -2.34567890, null),
            TestData("+2.34567890", 2.34567890, null),
            TestData("2*(3+4)", 14.0, null),
            TestData("2*(3-4)", -2.0, null),
            TestData("-2*(3-4)", 2.0, null),
            TestData("-2*(-3-4)", 14.0, null),
            TestData(".", 0.0, "empty statement"),
            TestData(".1", 0.1, null),
            TestData(".1.", 0.0, "unexpected comma"),
            TestData("..1", 0.0, "unexpected comma"),
            TestData("1+2*3-4", 3.0, null),
            TestData("1-2*3/6", 0.0, null),
            TestData("1-2*3/0", 0.0, "division by zero"),
            TestData("2+(3+4)*(4-8)", -26.0, null),
            TestData("2+(3+4)*(4-8)+8", -18.0, null),
            TestData("2+(3+4)*(4-8", 0.0, ") is missing"),
            TestData("2+(3+4)*4-8)", 0.0, "( is missing"),
            TestData("2+(3+x)*4-8)", 0.0, "unexpected character: x")
        )
    }

    @Test
    fun testExpressionParser() {
        for (test in tests) {
            val p = ExpressionParser()
            try {
                val result = p.eval(test.input)
                assertEquals(test.result, result, 0.00000000000001)
            } catch (e: Exception) {
                assertEquals(test.errorMessage, e.message)
            }
        }
    }
}