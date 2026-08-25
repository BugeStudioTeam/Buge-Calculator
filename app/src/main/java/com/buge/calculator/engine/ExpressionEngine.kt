package com.buge.calculator.engine

import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.EvaluationResult
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/** A dependency-free mathematical parser designed for calculator and plotting use. */
object ExpressionEngine {
    /**
     * Evaluates a calculator expression. Any unmatched opening parenthesis is closed at the
     * end of the expression, so inputs such as `sin(90` and `sqrt(9` evaluate naturally.
     */
    fun evaluate(
        expression: String,
        angleUnit: AngleUnit = AngleUnit.DEG,
        variable: Double = 0.0,
        variableY: Double = 0.0,
        answer: Double = 0.0
    ): EvaluationResult = compile(expression)?.evaluate(angleUnit, variable, variableY, answer)
        ?: EvaluationResult(0.0)

    /** Compiles an expression once for efficient repeated evaluation, such as graph sampling. */
    fun compile(expression: String): CompiledExpression? = try {
        val normalized = completeOpenParentheses(normalize(expression))
        if (normalized.isBlank()) null else CompiledExpression(Lexer(normalized).tokenize())
    } catch (_: Exception) {
        null
    }

    class CompiledExpression internal constructor(private val tokens: List<Token>) {
        fun evaluate(
            angleUnit: AngleUnit = AngleUnit.DEG,
            variable: Double = 0.0,
            variableY: Double = 0.0,
            answer: Double = 0.0
        ): EvaluationResult = try {
            EvaluationResult(Parser(tokens, angleUnit, variable, variableY, answer).parse())
        } catch (exception: CalculationException) {
            EvaluationResult(null, exception.message ?: "Invalid expression")
        } catch (_: Exception) {
            EvaluationResult(null, "Invalid expression")
        }
    }

    private fun normalize(expression: String): String = expression
        .replace("×", "*")
        .replace("÷", "/")
        .replace("−", "-")
        .replace("π", "pi")
        .replace("√", "sqrt")
        .trim()

    private fun completeOpenParentheses(expression: String): String {
        var balance = 0
        expression.forEach { character ->
            if (character == '(') balance++
            if (character == ')') balance--
        }
        return if (balance > 0) expression + ")".repeat(balance) else expression
    }

    private class CalculationException(message: String) : RuntimeException(message)

    internal enum class TokenType { NUMBER, IDENTIFIER, OPERATOR, LEFT, RIGHT, COMMA, END }
    internal data class Token(val type: TokenType, val text: String, val number: Double = 0.0)

    private class Lexer(private val source: String) {
        private var index = 0

        fun tokenize(): List<Token> {
            val raw = mutableListOf<Token>()
            while (index < source.length) {
                val char = source[index]
                when {
                    char.isWhitespace() -> index++
                    char.isDigit() || char == '.' -> raw += readNumber()
                    char.isLetter() || char == '_' -> raw += readIdentifier()
                    char in "+-*/^%!" -> {
                        raw += Token(TokenType.OPERATOR, char.toString())
                        index++
                    }
                    char == '(' -> { raw += Token(TokenType.LEFT, "("); index++ }
                    char == ')' -> { raw += Token(TokenType.RIGHT, ")"); index++ }
                    char == ',' -> { raw += Token(TokenType.COMMA, ","); index++ }
                    else -> throw CalculationException("Unexpected character: $char")
                }
            }
            return injectImplicitMultiplication(raw) + Token(TokenType.END, "")
        }

        private fun readNumber(): Token {
            val start = index
            var dots = 0
            while (index < source.length && (source[index].isDigit() || source[index] == '.')) {
                if (source[index] == '.') dots++
                index++
            }
            if (dots > 1) throw CalculationException("Invalid number")
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                val exponentStart = index++
                if (index < source.length && source[index] in "+-") index++
                val digitStart = index
                while (index < source.length && source[index].isDigit()) index++
                if (digitStart == index) throw CalculationException("Invalid exponent")
                if (exponentStart == start) throw CalculationException("Invalid number")
            }
            val text = source.substring(start, index)
            return Token(TokenType.NUMBER, text, text.toDoubleOrNull() ?: throw CalculationException("Invalid number"))
        }

        private fun readIdentifier(): Token {
            val start = index
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
            return Token(TokenType.IDENTIFIER, source.substring(start, index).lowercase())
        }

        private fun injectImplicitMultiplication(tokens: List<Token>): List<Token> {
            val result = mutableListOf<Token>()
            tokens.forEach { token ->
                val previous = result.lastOrNull()
                val leftCompletes = previous?.type in setOf(TokenType.NUMBER, TokenType.RIGHT) ||
                    (previous?.type == TokenType.IDENTIFIER && previous.text in setOf("pi", "e", "x", "y", "ans"))
                val rightStarts = token.type in setOf(TokenType.NUMBER, TokenType.LEFT) ||
                    (token.type == TokenType.IDENTIFIER && token.text !in FUNCTIONS)
                if (leftCompletes && rightStarts) result += Token(TokenType.OPERATOR, "*")
                result += token
            }
            return result
        }
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleUnit: AngleUnit,
        private val variable: Double,
        private val variableY: Double,
        private val answer: Double
    ) {
        private var position = 0

        fun parse(): Double {
            val value = parseAddSubtract()
            if (peek().type != TokenType.END) throw CalculationException("Unexpected token: ${peek().text}")
            if (!value.isFinite()) throw CalculationException("Result is not finite")
            return value
        }

        private fun parseAddSubtract(): Double {
            var value = parseMultiplyDivide()
            while (peekOperator("+") || peekOperator("-")) {
                value = if (consume().text == "+") value + parseMultiplyDivide() else value - parseMultiplyDivide()
            }
            return value
        }

        private fun parseMultiplyDivide(): Double {
            var value = parseUnary()
            while (peekOperator("*") || peekOperator("/") || peekOperator("%")) {
                when (consume().text) {
                    "*" -> value *= parseUnary()
                    "/" -> {
                        val divisor = parseUnary()
                        if (divisor == 0.0) throw CalculationException("Division by zero")
                        value /= divisor
                    }
                    "%" -> {
                        val divisor = parseUnary()
                        if (divisor == 0.0) throw CalculationException("Division by zero")
                        value %= divisor
                    }
                }
            }
            return value
        }

        private fun parseUnary(): Double = when {
            peekOperator("+") -> { consume(); parseUnary() }
            peekOperator("-") -> { consume(); -parseUnary() }
            else -> parsePower()
        }

        private fun parsePower(): Double {
            var value = parsePostfix()
            if (peekOperator("^")) {
                consume()
                value = value.pow(parseUnary())
            }
            return value
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (peekOperator("!")) {
                consume()
                value = factorial(value)
            }
            return value
        }

        private fun parsePrimary(): Double {
            return when (peek().type) {
                TokenType.NUMBER -> consume().number
                TokenType.LEFT -> {
                    consume()
                    val value = parseAddSubtract()
                    expect(TokenType.RIGHT, "Expected )")
                    value
                }
                TokenType.IDENTIFIER -> parseIdentifier()
                else -> throw CalculationException("Expected a number or function")
            }
        }

        private fun parseIdentifier(): Double {
            val name = consume().text
            return when (name) {
                "pi" -> PI
                "e" -> E
                "x" -> variable
                "y" -> variableY
                "ans" -> answer
                else -> {
                    expect(TokenType.LEFT, "Expected ( after $name")
                    val arguments = mutableListOf<Double>()
                    if (peek().type != TokenType.RIGHT) {
                        arguments += parseAddSubtract()
                        while (peek().type == TokenType.COMMA) {
                            consume()
                            arguments += parseAddSubtract()
                        }
                    }
                    expect(TokenType.RIGHT, "Expected )")
                    applyFunction(name, arguments)
                }
            }
        }

        private fun applyFunction(name: String, args: List<Double>): Double {
            fun unary(block: (Double) -> Double): Double {
                if (args.size != 1) throw CalculationException("$name expects one argument")
                return block(args.first())
            }
            fun binary(block: (Double, Double) -> Double): Double {
                if (args.size != 2) throw CalculationException("$name expects two arguments")
                return block(args[0], args[1])
            }
            return when (name) {
                "sin" -> unary { sin(toRadians(it)) }
                "cos" -> unary { cos(toRadians(it)) }
                "tan" -> unary { tan(toRadians(it)) }
                "asin" -> unary { fromRadians(asin(it)) }
                "acos" -> unary { fromRadians(acos(it)) }
                "atan" -> unary { fromRadians(atan(it)) }
                "sinh" -> unary(::sinh)
                "cosh" -> unary(::cosh)
                "tanh" -> unary(::tanh)
                "sqrt" -> unary { if (it < 0) throw CalculationException("Square root of a negative number") else sqrt(it) }
                "cbrt" -> unary { kotlin.math.cbrt(it) }
                "abs" -> unary(::abs)
                "ln" -> unary { if (it <= 0) throw CalculationException("ln requires a positive number") else ln(it) }
                "log" -> unary { if (it <= 0) throw CalculationException("log requires a positive number") else log10(it) }
                "exp" -> unary(::exp)
                "floor" -> unary(::floor)
                "ceil" -> unary(::ceil)
                "round" -> unary(::round)
                "min" -> binary(::minOf)
                "max" -> binary(::maxOf)
                "pow" -> binary { a, b -> a.pow(b) }
                "mod" -> binary { a, b -> if (b == 0.0) throw CalculationException("Division by zero") else a % b }
                "ncr" -> binary(::combinations)
                "npr" -> binary(::permutations)
                "gcd" -> binary(::gcd)
                "lcm" -> binary { a, b -> val divisor = gcd(a, b); if (divisor == 0.0) 0.0 else abs(a * b) / divisor }
                else -> throw CalculationException("Unknown function: $name")
            }
        }

        private fun toRadians(value: Double): Double = when (angleUnit) {
            AngleUnit.DEG -> value * PI / 180.0
            AngleUnit.RAD -> value
            AngleUnit.GRAD -> value * PI / 200.0
        }

        private fun fromRadians(value: Double): Double = when (angleUnit) {
            AngleUnit.DEG -> value * 180.0 / PI
            AngleUnit.RAD -> value
            AngleUnit.GRAD -> value * 200.0 / PI
        }

        private fun factorial(value: Double): Double {
            if (value < 0 || value % 1.0 != 0.0 || value > 170) throw CalculationException("Factorial requires an integer from 0 to 170")
            return (2..value.toInt()).fold(1.0) { total, item -> total * item }
        }

        private fun combinations(n: Double, r: Double): Double {
            if (n < 0 || r < 0 || n % 1.0 != 0.0 || r % 1.0 != 0.0 || r > n) throw CalculationException("nCr requires integers with 0 ≤ r ≤ n")
            val rr = minOf(r.toInt(), n.toInt() - r.toInt())
            return (1..rr).fold(1.0) { result, i -> result * (n - rr + i) / i }
        }

        private fun permutations(n: Double, r: Double): Double {
            if (n < 0 || r < 0 || n % 1.0 != 0.0 || r % 1.0 != 0.0 || r > n) throw CalculationException("nPr requires integers with 0 ≤ r ≤ n")
            return (0 until r.toInt()).fold(1.0) { result, i -> result * (n - i) }
        }

        private fun gcd(a: Double, b: Double): Double {
            if (a % 1.0 != 0.0 || b % 1.0 != 0.0) throw CalculationException("gcd requires integers")
            var x = abs(a.toLong())
            var y = abs(b.toLong())
            while (y != 0L) { val temp = x % y; x = y; y = temp }
            return x.toDouble()
        }

        private fun peek(): Token = tokens[position]
        private fun consume(): Token = tokens[position++]
        private fun peekOperator(symbol: String): Boolean = peek().type == TokenType.OPERATOR && peek().text == symbol
        private fun expect(type: TokenType, message: String) {
            if (peek().type != type) throw CalculationException(message)
            consume()
        }
    }

    private val FUNCTIONS = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
        "sqrt", "cbrt", "abs", "ln", "log", "exp", "floor", "ceil", "round",
        "min", "max", "pow", "mod", "ncr", "npr", "gcd", "lcm"
    )
}
