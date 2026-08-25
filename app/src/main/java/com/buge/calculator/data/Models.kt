package com.buge.calculator.data

import androidx.compose.ui.graphics.Color
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
    val code: String = "def f(x):\n    return sin(x) + x**2\n\nprint(f(2))",
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

data class BugeStrings(
    val calculator: String,
    val graph: String,
    val history: String,
    val settings: String,
    val clear: String,
    val delete: String,
    val equals: String,
    val angle: String,
    val degrees: String,
    val radians: String,
    val gradians: String,
    val scientific: String,
    val basic: String,
    val expression: String,
    val graphExpressionHint: String,
    val plot: String,
    val resetView: String,
    val grid: String,
    val noHistory: String,
    val noHistoryDescription: String,
    val reuse: String,
    val clearHistory: String,
    val appearance: String,
    val colorSource: String,
    val dynamic: String,
    val lavender: String,
    val ocean: String,
    val forest: String,
    val sunset: String,
    val custom: String,
    val displayMode: String,
    val system: String,
    val light: String,
    val dark: String,
    val language: String,
    val english: String,
    val chinese: String,
    val haptics: String,
    val customColor: String,
    val red: String,
    val green: String,
    val blue: String,
    val graphHelp: String,
    val graphHelpDescription: String,
    val invalidExpression: String,
    val unsupported: String,
    val model3d: String,
    val python: String,
    val surfaceExpressionHint: String,
    val surfaceHelp: String,
    val surfaceHelpDescription: String,
    val resetCamera: String,
    val showMesh: String,
    val pythonCode: String,
    val pythonCodeHint: String,
    val runPython: String,
    val pythonResult: String,
    val pythonHelp: String,
    val pythonHelpDescription: String
)

val EnglishStrings = BugeStrings(
    calculator = "Calculate", graph = "Graph", history = "History", settings = "Settings",
    clear = "Clear", delete = "Delete", equals = "Equals", angle = "Angle",
    degrees = "DEG", radians = "RAD", gradians = "GRAD", scientific = "Scientific", basic = "Basic",
    expression = "Expression", graphExpressionHint = "e.g. sin(x), x^2 - 4",
    plot = "Plot", resetView = "Reset view", grid = "Grid", noHistory = "No calculations yet",
    noHistoryDescription = "Your completed calculations will appear here.", reuse = "Reuse",
    clearHistory = "Clear history", appearance = "Appearance", colorSource = "Color source",
    dynamic = "Dynamic", lavender = "Lavender", ocean = "Ocean", forest = "Forest", sunset = "Sunset",
    custom = "Custom", displayMode = "Display mode", system = "System", light = "Light", dark = "Dark",
    language = "Language", english = "English", chinese = "Chinese", haptics = "Haptic feedback",
    customColor = "Custom seed color", red = "Red", green = "Green", blue = "Blue",
    graphHelp = "Interactive graph", graphHelpDescription = "Pinch to zoom and drag to pan. Use x as the independent variable.",
    invalidExpression = "Invalid expression", unsupported = "Unsupported operation",
    model3d = "3D Model", python = "Python", surfaceExpressionHint = "e.g. sin(sqrt(x^2+y^2))",
    surfaceHelp = "Formula surface", surfaceHelpDescription = "Write z = f(x, y), then drag to rotate and pinch to zoom.",
    resetCamera = "Reset camera", showMesh = "Mesh", pythonCode = "Python code", pythonCodeHint = "Write a mathematical function or calculation", runPython = "Run Python",
    pythonResult = "Output", pythonHelp = "Local mathematical Python", pythonHelpDescription = "Runs safe mathematical Python code on this device. Imports, files and network access are disabled."
)

val ChineseStrings = BugeStrings(
    calculator = "计算", graph = "绘图", history = "历史", settings = "设置",
    clear = "清除", delete = "删除", equals = "等于", angle = "角度",
    degrees = "度", radians = "弧度", gradians = "梯度", scientific = "科学", basic = "基础",
    expression = "表达式", graphExpressionHint = "例如 sin(x)、x^2 - 4",
    plot = "绘制", resetView = "重置视图", grid = "网格", noHistory = "尚无计算记录",
    noHistoryDescription = "已完成的计算会显示在此处。", reuse = "复用",
    clearHistory = "清空历史", appearance = "外观", colorSource = "配色来源",
    dynamic = "动态", lavender = "薰衣草", ocean = "海洋", forest = "森林", sunset = "日落",
    custom = "自定义", displayMode = "显示模式", system = "跟随系统", light = "浅色", dark = "深色",
    language = "语言", english = "English", chinese = "中文", haptics = "触感反馈",
    customColor = "自定义种子色", red = "红", green = "绿", blue = "蓝",
    graphHelp = "交互式图像", graphHelpDescription = "双指缩放、拖动平移。请使用 x 作为自变量。",
    invalidExpression = "表达式无效", unsupported = "暂不支持的运算",
    model3d = "三维模型", python = "Python", surfaceExpressionHint = "例如 sin(sqrt(x^2+y^2))",
    surfaceHelp = "公式曲面", surfaceHelpDescription = "输入 z = f(x, y)，拖动旋转，双指缩放。",
    resetCamera = "重置视角", showMesh = "网格", pythonCode = "Python 代码", pythonCodeHint = "编写数学函数或计算", runPython = "运行 Python",
    pythonResult = "输出", pythonHelp = "本地数学 Python", pythonHelpDescription = "在本设备运行安全的数学 Python 代码；导入、文件和网络访问均被禁用。"
)

fun AppLanguage.strings(): BugeStrings = when (this) {
    AppLanguage.CHINESE -> ChineseStrings
    AppLanguage.SPANISH -> EnglishStrings.copy(calculator = "Calcular", graph = "Gráfica", history = "Historial", settings = "Ajustes", model3d = "Modelo 3D", language = "Idioma", colorSource = "Fuente de color", pythonHelp = "Python matemático local")
    AppLanguage.FRENCH -> EnglishStrings.copy(calculator = "Calculer", graph = "Graphique", history = "Historique", settings = "Réglages", model3d = "Modèle 3D", language = "Langue", colorSource = "Source de couleur", pythonHelp = "Python mathématique local")
    AppLanguage.JAPANESE -> EnglishStrings.copy(calculator = "計算", graph = "グラフ", history = "履歴", settings = "設定", model3d = "3Dモデル", language = "言語", colorSource = "カラーソース", pythonHelp = "ローカル数学 Python")
    AppLanguage.KOREAN -> EnglishStrings.copy(calculator = "계산", graph = "그래프", history = "기록", settings = "설정", model3d = "3D 모델", language = "언어", colorSource = "색상 소스", pythonHelp = "로컬 수학 Python")
    AppLanguage.GERMAN -> EnglishStrings.copy(calculator = "Rechnen", graph = "Diagramm", history = "Verlauf", settings = "Einstellungen", model3d = "3D-Modell", language = "Sprache", colorSource = "Farbquelle", pythonHelp = "Lokales mathematisches Python")
    AppLanguage.ENGLISH -> EnglishStrings
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
