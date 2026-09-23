package com.buge.calculator.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Lightweight Python lexer for the editor. It changes presentation only:
 * the underlying source text and cursor offsets remain identical.
 *
 * Colors intentionally come from Material 3 semantic ColorScheme roles rather
 * than hard-coded hues, so dynamic color, light/dark mode, and user themes all
 * remain consistent with the rest of the app.
 */
class PythonSyntaxVisualTransformation(
    private val colors: ColorScheme
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val builder = AnnotatedString.Builder()
        var index = 0

        while (index < source.length) {
            val current = source[index]

            if (current == '#') {
                val end = source.indexOf('\n', index).let { if (it == -1) source.length else it }
                appendStyled(builder, source.substring(index, end), colors.onSurfaceVariant)
                index = end
                continue
            }

            if (current == '\'' || current == '"') {
                val end = stringEnd(source, index)
                appendStyled(builder, source.substring(index, end), colors.tertiary)
                index = end
                continue
            }

            if (current.isDigit() && (index == 0 || !source[index - 1].isLetterOrDigit() && source[index - 1] != '_')) {
                val end = numberEnd(source, index)
                appendStyled(builder, source.substring(index, end), colors.secondary)
                index = end
                continue
            }

            if (current.isLetter() || current == '_') {
                val end = identifierEnd(source, index)
                val word = source.substring(index, end)
                val color = when {
                    word in PYTHON_KEYWORDS -> colors.primary
                    word in PYTHON_BUILTINS -> colors.secondary
                    word in PYTHON_CONSTANTS -> colors.primary
                    else -> null
                }
                appendStyled(builder, word, color)
                index = end
                continue
            }

            if (current == '@' && index + 1 < source.length && (source[index + 1].isLetter() || source[index + 1] == '_')) {
                val end = identifierEnd(source, index + 1)
                appendStyled(builder, source.substring(index, end), colors.tertiary)
                index = end
                continue
            }

            builder.append(current)
            index++
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun appendStyled(builder: AnnotatedString.Builder, value: String, color: Color?) {
        if (color == null) {
            builder.append(value)
        } else {
            builder.withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) {
                append(value)
            }
        }
    }

    private fun stringEnd(source: String, start: Int): Int {
        val quote = source[start]
        val triple = source.startsWith("$quote$quote$quote", start)
        val delimiterLength = if (triple) 3 else 1
        var index = start + delimiterLength
        while (index < source.length) {
            if (source[index] == '\\') {
                index += 2
                continue
            }
            if (source.startsWith(quote.toString().repeat(delimiterLength), index)) {
                return (index + delimiterLength).coerceAtMost(source.length)
            }
            index++
        }
        return source.length
    }

    private fun identifierEnd(source: String, start: Int): Int {
        var index = start
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
        return index
    }

    private fun numberEnd(source: String, start: Int): Int {
        var index = start
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] in "._+-")) {
            if ((source[index] == '+' || source[index] == '-') && index > start && source[index - 1] !in "eE") break
            index++
        }
        return index
    }

    private companion object {
        val PYTHON_KEYWORDS = setOf(
            "and", "as", "assert", "async", "await", "break", "case", "class", "continue",
            "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
            "if", "import", "in", "is", "lambda", "match", "nonlocal", "not", "or",
            "pass", "raise", "return", "try", "while", "with", "yield"
        )
        val PYTHON_BUILTINS = setOf(
            "abs", "all", "any", "bin", "bool", "bytes", "callable", "chr", "dict", "dir",
            "divmod", "enumerate", "eval", "filter", "float", "format", "frozenset", "getattr",
            "hasattr", "hash", "help", "hex", "id", "input", "int", "isinstance", "issubclass",
            "iter", "len", "list", "map", "max", "memoryview", "min", "next", "object", "oct",
            "open", "ord", "pow", "print", "property", "range", "repr", "reversed", "round",
            "set", "setattr", "slice", "sorted", "staticmethod", "str", "sum", "super", "tuple",
            "type", "vars", "zip"
        )
        val PYTHON_CONSTANTS = setOf("True", "False", "None", "NotImplemented", "Ellipsis")
    }
}
