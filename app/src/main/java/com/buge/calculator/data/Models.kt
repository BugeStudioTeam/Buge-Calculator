package com.buge.calculator.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.buge.calculator.R
import java.text.DecimalFormat
import java.util.UUID

enum class AngleUnit { DEG, RAD, GRAD }
enum class CalculatorMode { BASIC, SCIENTIFIC }
enum class AppLanguage { ENGLISH, CHINESE, SPANISH, FRENCH, JAPANESE, KOREAN, GERMAN }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ThemeSource { DYNAMIC, LAVENDER, OCEAN, FOREST, SUNSET, CUSTOM }
enum class AppDestination { CALCULATE, GRAPH, MODEL_3D, PYTHON }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeSource: ThemeSource = ThemeSource.DYNAMIC,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hapticsEnabled: Boolean = true,
    val customRed: Float = 0.40f,
    val customGreen: Float = 0.31f,
    val customBlue: Float = 0.65f
) {
    val customColor: Color get() = Color(customRed, customGreen, customBlue)
}

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class GraphSettings(
    val expression: String = "sin(x)",
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 42f,
    // Independent vertical scale. Defaults to the horizontal scale (1:1 equidistant view), but the
    // auto-fit pass and pinch gestures can set it independently so that small-amplitude periodic
    // curves such as sin(x) render as a proper wave instead of a flat line hugging the x-axis.
    val scaleY: Float = 42f,
    // When true the two axes are forced to share the same scale (mathematically faithful view).
    val lockAspect: Boolean = false,
    val showGrid: Boolean = true
)

data class SurfaceSettings(
    val expression: String = "sin(sqrt(x^2+y^2))",
    val yaw: Float = 0.62f,
    val pitch: Float = -0.48f,
    val zoom: Float = 36f,
    val showMesh: Boolean = true
)

data class PythonWorkspace(
    val code: String = "import math\nimport statistics\n\nvalues = [1, 2, 3, 4, 5]\nmean = statistics.mean(values)\nprint(f'√81 = {math.sqrt(81)}')\nprint(f'mean = {mean}')",
    val output: String = "",
    val error: String? = null
)

data class CalculatorState(
    val expression: String = "",
    val preview: String = "0",
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val mode: CalculatorMode = CalculatorMode.SCIENTIFIC,
    val lastAnswer: Double = 0.0,
    val error: String? = null,
    val history: List<HistoryEntry> = emptyList(),
    val graph: GraphSettings = GraphSettings(),
    val surface: SurfaceSettings = SurfaceSettings(),
    val python: PythonWorkspace = PythonWorkspace()
)

data class EvaluationResult(val value: Double?, val error: String? = null) {
    val isSuccess: Boolean get() = value != null && error == null
}

object NumberFormatter {
    private val regular = DecimalFormat("0.##########")
    private val scientific = DecimalFormat("0.##########E0")

    fun format(value: Double): String {
        if (!value.isFinite()) return "Undefined"
        if (value == 0.0) return "0"
        return if (kotlin.math.abs(value) >= 1.0E12 || kotlin.math.abs(value) < 1.0E-9) {
            scientific.format(value)
        } else {
            regular.format(value)
        }
    }
}

/**
 * UI text bundle backed by standard Android string resources.
 *
 * All copy now lives in res/values/strings.xml plus the per-locale
 * res/values-xx/strings.xml files. [BugeStrings] is a thin reader over those
 * resources: every property delegates to `context.getString(R.string.<key>)`
 * against a [Context] whose configuration has been pinned to the language
 * chosen in settings, so Android resolves the matching `values-xx` folder —
 * entirely independent of the device's system language.
 *
 * The public API (property names) is unchanged, so no UI call site had to be touched.
 */
class BugeStrings(context: Context, language: AppLanguage) {
    private val context: Context = context.withLanguage(language)

    val calculator: String get() = context.getString(R.string.calculator)
    val graph: String get() = context.getString(R.string.graph)
    val history: String get() = context.getString(R.string.history)
    val settings: String get() = context.getString(R.string.settings)
    val clear: String get() = context.getString(R.string.clear)
    val delete: String get() = context.getString(R.string.delete)
    val equals: String get() = context.getString(R.string.equals)
    val angle: String get() = context.getString(R.string.angle)
    val degrees: String get() = context.getString(R.string.degrees)
    val radians: String get() = context.getString(R.string.radians)
    val gradians: String get() = context.getString(R.string.gradians)
    val scientific: String get() = context.getString(R.string.scientific)
    val basic: String get() = context.getString(R.string.basic)
    val expression: String get() = context.getString(R.string.expression)
    val graphExpressionHint: String get() = context.getString(R.string.graphExpressionHint)
    val plot: String get() = context.getString(R.string.plot)
    val resetView: String get() = context.getString(R.string.resetView)
    val grid: String get() = context.getString(R.string.grid)
    val lockAspect: String get() = context.getString(R.string.lockAspect)
    val noHistory: String get() = context.getString(R.string.noHistory)
    val noHistoryDescription: String get() = context.getString(R.string.noHistoryDescription)
    val reuse: String get() = context.getString(R.string.reuse)
    val clearHistory: String get() = context.getString(R.string.clearHistory)
    val appearance: String get() = context.getString(R.string.appearance)
    val colorSource: String get() = context.getString(R.string.colorSource)
    val dynamic: String get() = context.getString(R.string.dynamic)
    val lavender: String get() = context.getString(R.string.lavender)
    val ocean: String get() = context.getString(R.string.ocean)
    val forest: String get() = context.getString(R.string.forest)
    val sunset: String get() = context.getString(R.string.sunset)
    val custom: String get() = context.getString(R.string.custom)
    val displayMode: String get() = context.getString(R.string.displayMode)
    val system: String get() = context.getString(R.string.system)
    val light: String get() = context.getString(R.string.light)
    val dark: String get() = context.getString(R.string.dark)
    val language: String get() = context.getString(R.string.language)
    val english: String get() = context.getString(R.string.english)
    val chinese: String get() = context.getString(R.string.chinese)
    val haptics: String get() = context.getString(R.string.haptics)
    val customColor: String get() = context.getString(R.string.customColor)
    val red: String get() = context.getString(R.string.red)
    val green: String get() = context.getString(R.string.green)
    val blue: String get() = context.getString(R.string.blue)
    val graphHelp: String get() = context.getString(R.string.graphHelp)
    val graphHelpDescription: String get() = context.getString(R.string.graphHelpDescription)
    val invalidExpression: String get() = context.getString(R.string.invalidExpression)
    val unsupported: String get() = context.getString(R.string.unsupported)
    val model3d: String get() = context.getString(R.string.model3d)
    val python: String get() = context.getString(R.string.python)
    val surfaceExpressionHint: String get() = context.getString(R.string.surfaceExpressionHint)
    val surfaceHelp: String get() = context.getString(R.string.surfaceHelp)
    val surfaceHelpDescription: String get() = context.getString(R.string.surfaceHelpDescription)
    val resetCamera: String get() = context.getString(R.string.resetCamera)
    val showMesh: String get() = context.getString(R.string.showMesh)
    val pythonCode: String get() = context.getString(R.string.pythonCode)
    val pythonCodeHint: String get() = context.getString(R.string.pythonCodeHint)
    val runPython: String get() = context.getString(R.string.runPython)
    val pythonResult: String get() = context.getString(R.string.pythonResult)
    val pythonHelp: String get() = context.getString(R.string.pythonHelp)
    val pythonHelpDescription: String get() = context.getString(R.string.pythonHelpDescription)
}

/**
 * Resolves the UI text bundle against the app locale chosen in settings.
 */
fun AppLanguage.strings(context: Context): BugeStrings = BugeStrings(context, this)

/** BCP-47 tag for the locale backing this language choice. */
fun AppLanguage.localeTag(): String = when (this) {
    AppLanguage.ENGLISH -> "en"
    AppLanguage.CHINESE -> "zh"
    AppLanguage.SPANISH -> "es"
    AppLanguage.FRENCH -> "fr"
    AppLanguage.JAPANESE -> "ja"
    AppLanguage.KOREAN -> "ko"
    AppLanguage.GERMAN -> "de"
}

/**
 * Returns a [Context] whose configuration is pinned to [language].
 *
 * This is what makes an in-app language switch work with plain string resources: the
 * returned wrapper's [android.content.res.Resources] resolves `getString` against the
 * requested locale regardless of the device's system language. The device-wide locale is
 * never modified.
 */
fun Context.withLanguage(language: AppLanguage): Context {
    val locale = java.util.Locale.forLanguageTag(language.localeTag())
    val configuration = android.content.res.Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return createConfigurationContext(configuration)
}

fun AppLanguage.displayName(): String = when (this) {
    AppLanguage.ENGLISH -> "English"
    AppLanguage.CHINESE -> "中文"
    AppLanguage.SPANISH -> "Español"
    AppLanguage.FRENCH -> "Français"
    AppLanguage.JAPANESE -> "日本語"
    AppLanguage.KOREAN -> "한국어"
    AppLanguage.GERMAN -> "Deutsch"
}
