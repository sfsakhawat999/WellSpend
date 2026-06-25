package com.h2.wellspend.utils

object MathParser {
    // Check if any basic math operator (+, -, *, /) or parenthesis is present in the expression
    fun hasMathOperator(expr: String): Boolean {
        return expr.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '(' || it == ')' }
    }

    // Check if parentheses are matching and in correct order
    fun areParenthesesCorrect(expr: String): Boolean {
        var balance = 0
        for (char in expr) {
            if (char == '(') {
                balance++
            } else if (char == ')') {
                balance--
                if (balance < 0) return false
            }
        }
        return balance == 0
    }

    // Evaluates the expression. Returns the Double result, or null if it's invalid.
    fun evaluate(expr: String): Double? {
        val cleanExpr = expr.replace(" ", "")
        if (cleanExpr.isEmpty()) return null
        
        // Before evaluation, verify if parentheses are balanced. If not, it's invalid.
        if (!areParenthesesCorrect(cleanExpr)) return null
        
        return try {
            val parser = Parser(cleanExpr)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val input: String) {
        private var pos = 0

        private fun peek(): Char? {
            return if (pos < input.length) input[pos] else null
        }

        private fun next(): Char? {
            return if (pos < input.length) input[pos++] else null
        }

        fun parse(): Double {
            val result = expression()
            if (pos < input.length) {
                throw IllegalArgumentException("Unexpected character: ${input[pos]}")
            }
            return result
        }

        // expression = term { ('+' | '-') term }
        private fun expression(): Double {
            var result = term()
            while (true) {
                val nextChar = peek()
                if (nextChar == '+' || nextChar == '-') {
                    next() // consume operator
                    val right = term()
                    if (nextChar == '+') {
                        result += right
                    } else {
                        result -= right
                    }
                } else {
                    break
                }
            }
            return result
        }

        // term = factor { ('*' | '/') factor }
        private fun term(): Double {
            var result = factor()
            while (true) {
                val nextChar = peek()
                if (nextChar == '*' || nextChar == '/') {
                    next() // consume operator
                    val right = factor()
                    if (nextChar == '*') {
                        result *= right
                    } else {
                        if (right == 0.0) throw ArithmeticException("Division by zero")
                        result /= right
                    }
                } else {
                    break
                }
            }
            return result
        }

        // factor = number | '(' expression ')' | '-' factor | '+' factor
        private fun factor(): Double {
            val nextChar = peek() ?: throw IllegalArgumentException("Unexpected end of expression")
            if (nextChar == '+') {
                next()
                return factor()
            }
            if (nextChar == '-') {
                next()
                return -factor()
            }
            if (nextChar == '(') {
                next() // consume '('
                val result = expression()
                if (next() != ')') {
                    throw IllegalArgumentException("Expected ')'")
                }
                return result
            }
            
            // Parse number
            val start = pos
            if (peek() == '.') {
                next()
            }
            while (peek()?.isDigit() == true || peek() == '.') {
                next()
            }
            if (pos == start) {
                throw IllegalArgumentException("Expected number at position $pos")
            }
            val numStr = input.substring(start, pos)
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
        }
    }
}
