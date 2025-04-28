package com.sz.homeaccounting2

data class Op(val unary: Boolean, val value: Char)
data class OutputItem(val op: Op?, val value: Double)

class ExpressionParser {
    companion object {
        private val priorities: Map<Op, Int> = mapOf(
            Op(false, '+') to 1,
            Op(false, '-') to 1,
            Op(false, '*') to 2,
            Op(false, '/') to 2,
            Op(true, '+') to 3,
            Op(true, '-') to 3
        )
    }

    private val output: ArrayDeque<OutputItem> = ArrayDeque()
    private val opStack: ArrayDeque<Op> = ArrayDeque()
    private val dataStack: ArrayDeque<Double> = ArrayDeque()
    private var prevOp = true
    private var koef = 1.0
    private var op: Double? = null

    private fun parseNumber(multiplier: Double) {
        if (op == null) { op = 0.0 }
        if (koef < 1.0) {
            op = op!!.plus(koef * multiplier)
            koef /= 10
        } else {
            op = op!!.times(10)
            op = op!!.plus(multiplier)
        }
        prevOp = false
    }

    private fun storeNumber() {
        if (op != null) {
            output.addLast(OutputItem(null, op!!))
            koef = 1.0
            op = null
        }
    }

    private fun moveToOutput(priority: Int) {
        while (opStack.isNotEmpty()) {
            val v = opStack.last()
            if (v.value == '(') {
                return
            }
            val opPriority = priorities.getValue(v)
            if (opPriority < priority) {
                return
            }
            output.addLast(OutputItem(v, 0.0))
            opStack.removeLast()
        }
    }

    private fun operation(op: Op) {
        storeNumber()
        moveToOutput(priorities.getValue(op))
        opStack.addLast(op)
        prevOp = true
    }

    private fun parse(c: Char) {
        when (c) {
            '+', '-' -> {
                if (prevOp) {
                    operation(Op(true, c))
                    return
                }
                operation(Op(false, c))
            }
            '*', '/' -> {
                if (prevOp) {
                    throw IllegalArgumentException("invalid statement")
                }
                operation(Op(false, c))
            }
            ' ' -> storeNumber()
            '(' -> {
                storeNumber()
                opStack.addLast(Op(false, c))
            }
            ')' -> {
                if (prevOp) {
                    throw IllegalArgumentException("invalid statement")
                }
                storeNumber()
                moveToOutput(0)
                if (opStack.isEmpty()) {
                    throw IllegalArgumentException("( is missing")
                }
                opStack.removeLast()
            }
            '0' -> parseNumber(0.0)
            '1' -> parseNumber(1.0)
            '2' -> parseNumber(2.0)
            '3' -> parseNumber(3.0)
            '4' -> parseNumber(4.0)
            '5' -> parseNumber(5.0)
            '6' -> parseNumber(6.0)
            '7' -> parseNumber(7.0)
            '8' -> parseNumber(8.0)
            '9' -> parseNumber(9.0)
            '.' -> {
                if (koef < 1.0) {
                    throw IllegalArgumentException("unexpected comma")
                }
                koef = 0.1
                prevOp = false
            }
            else -> throw IllegalArgumentException(String.format("unexpected character: %c", c))
        }
    }

    private fun getOperand(): Double {
        if (dataStack.size < 2) {
            throw IllegalArgumentException("invalid statement")
        }
        return dataStack.removeLast()
    }

    private fun finish(): Double {
        storeNumber()
        moveToOutput(0)
        if (opStack.isNotEmpty()) {
            throw IllegalArgumentException(") is missing")
        }
        if (output.isEmpty()) {
            throw IllegalArgumentException("empty statement")
        }
        for (data in output) {
            when (data.op) {
                null -> dataStack.addLast(data.value)
                Op(true, '+') -> {
                    if (dataStack.isEmpty()) {
                        throw IllegalArgumentException("invalid statement")
                    }
                }
                Op(true, '-') -> {
                    if (dataStack.isEmpty()) {
                        throw IllegalArgumentException("invalid statement")
                    }
                    dataStack.addLast(-dataStack.removeLast())
                }
                Op(false, '+') -> {
                    val v = getOperand()
                    dataStack.addLast(dataStack.removeLast() + v)
                }
                Op(false, '-') -> {
                    val v = getOperand()
                    dataStack.addLast(dataStack.removeLast() - v)
                }
                Op(false, '*') -> {
                    val v = getOperand()
                    dataStack.addLast(dataStack.removeLast() * v)
                }
                Op(false, '/') -> {
                    val v = getOperand()
                    if (v == 0.0) {
                        throw IllegalArgumentException("division by zero")
                    }
                    dataStack.addLast(dataStack.removeLast() / v)
                }
            }
        }

        if (dataStack.size != 1) {
            throw IllegalArgumentException("invalid statement")
        }

        return dataStack.removeLast()
    }

    fun eval(value: String): Double {
        for (c in value) {
            parse(c)
        }

        return finish()
    }
}
