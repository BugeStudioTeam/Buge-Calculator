package com.buge.calculator.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Lightweight Python lexer for the editor. It changes presentation only:
 * the underlying source text and cursor offsets remain identical.
 *
 * Highlighting uses a dedicated, deliberately saturated palette instead of the
 * muted Material 3 semantic roles. The palette is chosen from the editor
 * background's luminance so tokens stay vivid in both light and dark themes,
 * while the plain identifiers keep the theme's text color so the code as a whole
 * still belongs to the current appearance.
 */
class PythonSyntaxVisualTransformation(
    private val colors: ColorScheme
) : VisualTransformation {
    // Dark surfaces get bright, neon-leaning tokens; light surfaces get deeper,
    // fully saturated tokens that keep strong contrast on a pale background.
    private val dark = colors.surface.luminance() < 0.5f

    private val keywordColor = if (dark) Color(0xFFFF7AB6) else Color(0xFFC2185B)
    private val builtinColor = if (dark) Color(0xFF6FE7FF) else Color(0xFF0077B6)
    private val constantColor = if (dark) Color(0xFFFFB86C) else Color(0xFFB85C00)
    private val stringColor = if (dark) Color(0xFF8CE99A) else Color(0xFF0F7B3F)
    private val numberColor = if (dark) Color(0xFFFFC96B) else Color(0xFFB26A00)
    private val commentColor = if (dark) Color(0xFF9AA5B1) else Color(0xFF5F6368)
    private val decoratorColor = if (dark) Color(0xFFD0A3FF) else Color(0xFF7A3FB5)

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val builder = AnnotatedString.Builder()
        var index = 0

        while (index < source.length) {
            val current = source[index]

            if (current == '#') {
                val end = source.indexOf('\n', index).let { if (it == -1) source.length else it }
                appendStyled(builder, source.substring(index, end), commentColor, italic = true)
                index = end
                continue
            }

            if (current == '\'' || current == '"') {
                val end = stringEnd(source, index)
                appendStyled(builder, source.substring(index, end), stringColor)
                index = end
                continue
            }

            if (current.isDigit() && (index == 0 || !source[index - 1].isLetterOrDigit() && source[index - 1] != '_')) {
                val end = numberEnd(source, index)
                appendStyled(builder, source.substring(index, end), numberColor)
                index = end
                continue
            }

            if (current.isLetter() || current == '_') {
                val end = identifierEnd(source, index)
                val word = source.substring(index, end)
                val color = when {
                    word in PYTHON_KEYWORDS -> keywordColor
                    word in PYTHON_BUILTINS -> builtinColor
                    word in PYTHON_CONSTANTS -> constantColor
                    else -> null
                }
                appendStyled(builder, word, color, bold = color != null)
                index = end
                continue
            }

            if (current == '@' && index + 1 < source.length && (source[index + 1].isLetter() || source[index + 1] == '_')) {
                val end = identifierEnd(source, index + 1)
                appendStyled(builder, source.substring(index, end), decoratorColor, bold = true)
                index = end
                continue
            }

            builder.append(current)
            index++
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun appendStyled(
        builder: AnnotatedString.Builder,
        value: String,
        color: Color?,
        bold: Boolean = false,
        italic: Boolean = false
    ) {
        if (color == null) {
            builder.append(value)
        } else {
            builder.withStyle(
                SpanStyle(
                    color = color,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
                )
            ) {
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
