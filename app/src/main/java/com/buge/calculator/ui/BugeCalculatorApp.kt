package com.buge.calculator.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buge.calculator.HistoryActivity
import com.buge.calculator.SettingsActivity
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.AppDestination
import com.buge.calculator.data.AppLanguage
import com.buge.calculator.data.AppSettings
import com.buge.calculator.data.BugeStrings
import com.buge.calculator.data.CalculatorMode
import com.buge.calculator.data.CalculatorState
import com.buge.calculator.data.HistoryEntry
import com.buge.calculator.data.ThemeMode
import com.buge.calculator.data.ThemeSource
import com.buge.calculator.data.displayName
import com.buge.calculator.data.strings
import com.buge.calculator.ui.theme.BugeTheme
import com.buge.calculator.viewmodel.CalculatorViewModel

@Composable
fun BugeCalculatorApp(viewModel: CalculatorViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = settings.language.strings()
    BugeTheme(settings) {
        CalculatorRoot(
            state = state,
            settings = settings,
            strings = strings,
            onInput = viewModel::input,
            onClear = viewModel::clear,
            onDelete = viewModel::delete,
            onEvaluate = viewModel::evaluate,
            onAngle = viewModel::setAngleUnit,
            onMode = viewModel::setMode,
            onGraphExpression = viewModel::setGraphExpression,
            onGraphGrid = viewModel::setGraphGrid,
            onGraphViewport = viewModel::updateGraphViewport,
            onResetGraph = viewModel::resetGraphView,
            onSurfaceExpression = viewModel::setSurfaceExpression,
            onSurfaceMesh = viewModel::setSurfaceMesh,
            onSurfaceCamera = viewModel::updateSurfaceCamera,
            onResetSurfaceCamera = viewModel::resetSurfaceCamera,
            onPythonCode = viewModel::setPythonCode,
            onRunPython = viewModel::runPython
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorRoot(
    state: CalculatorState,
    settings: AppSettings,
    strings: BugeStrings,
    onInput: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onEvaluate: () -> Unit,
    onAngle: (AngleUnit) -> Unit,
    onMode: (CalculatorMode) -> Unit,
    onGraphExpression: (String) -> Unit,
    onGraphGrid: (Boolean) -> Unit,
    onGraphViewport: (Float, Float, Float) -> Unit,
    onResetGraph: () -> Unit,
    onSurfaceExpression: (String) -> Unit,
    onSurfaceMesh: (Boolean) -> Unit,
    onSurfaceCamera: (Float, Float, Float) -> Unit,
    onResetSurfaceCamera: () -> Unit,
    onPythonCode: (String) -> Unit,
    onRunPython: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.CALCULATE) }
    val context = LocalContext.current
    val title = when (destination) {
        AppDestination.CALCULATE -> "Buge Calculator"
        AppDestination.GRAPH -> strings.graph
        AppDestination.MODEL_3D -> strings.model3d
        AppDestination.PYTHON -> strings.python
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Medium) },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, HistoryActivity::class.java)) }) { Icon(Icons.Filled.History, strings.history) }
                    IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) { Icon(Icons.Filled.Settings, strings.settings) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DestinationItem(Modifier.weight(1f), destination, AppDestination.CALCULATE, strings.calculator, Icons.Filled.Calculate) { destination = it }
                    DestinationItem(Modifier.weight(1f), destination, AppDestination.GRAPH, strings.graph, Icons.Filled.ShowChart) { destination = it }
                    DestinationItem(Modifier.weight(1f), destination, AppDestination.MODEL_3D, strings.model3d, Icons.Filled.Functions) { destination = it }
                    DestinationItem(Modifier.weight(1f), destination, AppDestination.PYTHON, strings.python, Icons.Filled.Code) { destination = it }
                }
            }
        }
    ) { padding ->
        Crossfade(targetState = destination, label = "screen") { active ->
            when (active) {
                AppDestination.CALCULATE -> CalculatorScreen(
                    modifier = Modifier.padding(padding), state = state, settings = settings, strings = strings,
                    onInput = onInput, onClear = onClear, onDelete = onDelete, onEvaluate = onEvaluate,
                    onAngle = onAngle, onMode = onMode
                )
                AppDestination.GRAPH -> GraphScreen(
                    modifier = Modifier.padding(padding), state = state, strings = strings,
                    onExpressionChange = onGraphExpression, onGridChange = onGraphGrid,
                    onViewportChange = onGraphViewport, onReset = onResetGraph
                )
                AppDestination.MODEL_3D -> Model3DScreen(
                    modifier = Modifier.padding(padding), surface = state.surface, angleUnit = state.angleUnit, strings = strings,
                    onExpressionChange = onSurfaceExpression, onMeshChange = onSurfaceMesh,
                    onCameraChange = onSurfaceCamera, onResetCamera = onResetSurfaceCamera
                )
                AppDestination.PYTHON -> PythonWorkbenchScreen(
                    modifier = Modifier.padding(padding), workspace = state.python, strings = strings,
                    onCodeChange = onPythonCode, onRun = onRunPython
                )
            }
        }
    }
}

@Composable
private fun DestinationItem(
    modifier: Modifier,
    current: AppDestination,
    destination: AppDestination,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: (AppDestination) -> Unit
) {
    val selected = current == destination
    Column(
        modifier = modifier.padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The selected indicator is deliberately behind the icon only: the M3 navigation-bar pill.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .clickable { onSelect(destination) }
                .padding(horizontal = 20.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalculatorScreen(
    modifier: Modifier,
    state: CalculatorState,
    settings: AppSettings,
    strings: BugeStrings,
    onInput: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onEvaluate: () -> Unit,
    onAngle: (AngleUnit) -> Unit,
    onMode: (CalculatorMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    fun tap(action: () -> Unit) {
        if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxHeight < 580.dp
        val expanded = maxHeight >= 720.dp
        // On a phone the keypad has priority. The display grows only as vertical room becomes available.
        val displayHeight: Dp = when {
            maxHeight < 580.dp -> 76.dp
            maxHeight < 680.dp -> 104.dp
            maxHeight < 800.dp -> 144.dp
            maxHeight < 980.dp -> 192.dp
            else -> (maxHeight * 0.30f).coerceIn(248.dp, 360.dp)
        }
        val contentScroll = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(contentScroll)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    ModeToggle(state.mode, strings, onToggle = { tap { onMode(it) } })
                    AngleToggle(state.angleUnit, strings, onToggle = { tap { onAngle(it) } })
                }
                CalculatorDisplay(state, strings, displayHeight)
            }
            Keypad(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 8.dp),
                compact = compact,
                expanded = expanded,
                scientific = state.mode == CalculatorMode.SCIENTIFIC,
                onInput = { tap { onInput(it) } },
                onClear = { tap(onClear) },
                onDelete = { tap(onDelete) },
                onLongDelete = { tap(onClear) },
                onEvaluate = { tap(onEvaluate) }
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: CalculatorMode, strings: BugeStrings, onToggle: (CalculatorMode) -> Unit) {
    AssistChip(
        onClick = { onToggle(if (mode == CalculatorMode.SCIENTIFIC) CalculatorMode.BASIC else CalculatorMode.SCIENTIFIC) },
        label = { Text(if (mode == CalculatorMode.SCIENTIFIC) strings.scientific else strings.basic) },
        leadingIcon = { Icon(Icons.Filled.Functions, null, Modifier.size(18.dp)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    )
}

@Composable
private fun AngleToggle(unit: AngleUnit, strings: BugeStrings, onToggle: (AngleUnit) -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(AngleUnit.DEG, AngleUnit.RAD, AngleUnit.GRAD).forEach { option ->
            val selected = option == unit
            Text(
                text = when (option) { AngleUnit.DEG -> strings.degrees; AngleUnit.RAD -> strings.radians; AngleUnit.GRAD -> strings.gradians },
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onToggle(option) }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CalculatorDisplay(state: CalculatorState, strings: BugeStrings, minHeight: Dp) {
    val expressionScroll = rememberScrollState()
    val isVeryCompact = minHeight <= 80.dp
    val isCompact = minHeight <= 112.dp
    val contentPadding = when {
        isVeryCompact -> 10.dp
        isCompact -> 12.dp
        minHeight <= 160.dp -> 16.dp
        else -> 20.dp
    }
    val expressionSize = when {
        isVeryCompact || state.expression.length > 24 -> 14.sp
        isCompact || state.expression.length > 16 -> 16.sp
        minHeight <= 160.dp -> 18.sp
        else -> 22.sp
    }
    val targetResultSize = when {
        isVeryCompact -> if (state.preview.length > 10) 22.dp else 28.dp
        isCompact -> if (state.preview.length > 12) 26.dp else 32.dp
        minHeight <= 160.dp -> if (state.preview.length > 14) 30.dp else 38.dp
        state.preview.length > 14 -> 34.dp
        else -> 46.dp
    }
    val resultSize by animateDpAsState(targetValue = targetResultSize, label = "result_size")
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.expression.ifBlank { "0" },
                modifier = Modifier.fillMaxWidth().horizontalScroll(expressionScroll),
                textAlign = TextAlign.End,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = expressionSize
            )
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = state.error != null) {
                    Text(state.error ?: strings.invalidExpression, color = MaterialTheme.colorScheme.error, fontSize = if (isCompact) 10.sp else 13.sp)
                }
                Text(
                    text = state.preview,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = resultSize.value.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ScientificKeyGrid(
    modifier: Modifier,
    compact: Boolean,
    onInput: (String) -> Unit
) {
    val groups = listOf(
        listOf("sin" to "sin(", "cos" to "cos(", "tan" to "tan(", "asin" to "asin(", "acos" to "acos("),
        listOf("log" to "log(", "ln" to "ln(", "√" to "sqrt(", "abs" to "abs(", "!" to "!"),
        listOf("x²" to "^2", "xʸ" to "^", "π" to "π", "e" to "e", "Ans" to "ans"),
        listOf("x" to "x", "nCr" to "nCr(", "nPr" to "nPr(", "mod" to "mod(", "⌈⌉" to "round("),
        listOf("gcd" to "gcd(", "lcm" to "lcm(", "min" to "min(", "max" to "max(", "," to ",")
    )
    // No outer container: every scientific function is a directly visible pill button.
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        groups.forEach { group ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
            ) {
                group.forEach { (label, value) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onInput(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = if (compact) 10.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Keypad(
    modifier: Modifier,
    compact: Boolean,
    expanded: Boolean,
    scientific: Boolean,
    onInput: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    onEvaluate: () -> Unit
) {
    val rows = listOf(
        listOf(CalculatorKeySpec("AC", KeyKind.ACTION, action = onClear), CalculatorKeySpec("(", KeyKind.ACTION) { onInput("(") }, CalculatorKeySpec(")", KeyKind.ACTION) { onInput(")") }, CalculatorKeySpec("⌫", KeyKind.ACTION, Icons.Filled.Backspace, longAction = onLongDelete, action = onDelete)),
        listOf(CalculatorKeySpec("7", KeyKind.NUMBER) { onInput("7") }, CalculatorKeySpec("8", KeyKind.NUMBER) { onInput("8") }, CalculatorKeySpec("9", KeyKind.NUMBER) { onInput("9") }, CalculatorKeySpec("÷", KeyKind.OPERATOR) { onInput("÷") }),
        listOf(CalculatorKeySpec("4", KeyKind.NUMBER) { onInput("4") }, CalculatorKeySpec("5", KeyKind.NUMBER) { onInput("5") }, CalculatorKeySpec("6", KeyKind.NUMBER) { onInput("6") }, CalculatorKeySpec("×", KeyKind.OPERATOR) { onInput("×") }),
        listOf(CalculatorKeySpec("1", KeyKind.NUMBER) { onInput("1") }, CalculatorKeySpec("2", KeyKind.NUMBER) { onInput("2") }, CalculatorKeySpec("3", KeyKind.NUMBER) { onInput("3") }, CalculatorKeySpec("−", KeyKind.OPERATOR) { onInput("−") }),
        listOf(CalculatorKeySpec("0", KeyKind.NUMBER) { onInput("0") }, CalculatorKeySpec(".", KeyKind.NUMBER) { onInput(".") }, CalculatorKeySpec("%", KeyKind.OPERATOR) { onInput("%") }, CalculatorKeySpec("+", KeyKind.OPERATOR) { onInput("+") }),
        listOf(CalculatorKeySpec("=", KeyKind.EQUALS, action = onEvaluate))
    )
    Column(modifier = modifier.fillMaxWidth()) {
        if (scientific) {
            // Five science rows and six ordinary rows share every remaining pixel.
            // At larger sizes both groups grow together rather than leaving blank space.
            ScientificKeyGrid(
                modifier = Modifier.fillMaxWidth().weight(5f),
                compact = compact,
                onInput = onInput
            )
            Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(if (scientific) 6f else 1f),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        CalculatorKey(key, Modifier.weight(if (row.size == 3) 1.333f else 1f), compact, expanded)
                    }
                }
            }
        }
    }
}

private enum class KeyKind { NUMBER, OPERATOR, ACTION, EQUALS }
private data class CalculatorKeySpec(
    val label: String,
    val kind: KeyKind,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val longAction: (() -> Unit)? = null,
    val action: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorKey(
    spec: CalculatorKeySpec,
    modifier: Modifier = Modifier,
    compact: Boolean,
    expanded: Boolean
) {
    val colors = when (spec.kind) {
        KeyKind.NUMBER -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
        KeyKind.OPERATOR -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        KeyKind.ACTION -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
        KeyKind.EQUALS -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    }
    val containerColor = when (spec.kind) {
        KeyKind.NUMBER -> MaterialTheme.colorScheme.surfaceContainerHigh
        KeyKind.OPERATOR -> MaterialTheme.colorScheme.secondaryContainer
        KeyKind.ACTION -> MaterialTheme.colorScheme.tertiaryContainer
        KeyKind.EQUALS -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when (spec.kind) {
        KeyKind.NUMBER -> MaterialTheme.colorScheme.onSurface
        KeyKind.OPERATOR -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyKind.ACTION -> MaterialTheme.colorScheme.onTertiaryContainer
        KeyKind.EQUALS -> MaterialTheme.colorScheme.onPrimary
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .combinedClickable(onClick = spec.action, onLongClick = spec.longAction)
            .padding(ButtonDefaults.ContentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (spec.icon != null) {
            Icon(spec.icon, contentDescription = spec.label, modifier = Modifier.size(if (expanded) 28.dp else 24.dp), tint = contentColor)
        } else {
            Text(spec.label, fontSize = if (expanded) 26.sp else if (compact) 18.sp else 21.sp, fontWeight = FontWeight.Medium, color = contentColor)
        }
    }
}

@Composable
private fun GraphScreen(
    modifier: Modifier,
    state: CalculatorState,
    strings: BugeStrings,
    onExpressionChange: (String) -> Unit,
    onGridChange: (Boolean) -> Unit,
    onViewportChange: (Float, Float, Float) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(strings.graphHelp, fontWeight = FontWeight.Medium)
                Text(strings.graphHelpDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        OutlinedTextField(
            value = state.graph.expression,
            onValueChange = onExpressionChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(strings.expression) },
            placeholder = { Text(strings.graphExpressionHint) },
            leadingIcon = { Icon(Icons.Filled.Functions, null) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = state.graph.showGrid, onClick = { onGridChange(!state.graph.showGrid) }, label = { Text(strings.grid) })
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onReset) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.resetView)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            FunctionGraphCanvas(
                graph = state.graph,
                angleUnit = state.angleUnit,
                modifier = Modifier.padding(6.dp),
                onViewportChange = onViewportChange
            )
        }
    }
}

@Composable
fun HistoryScreen(
    modifier: Modifier,
    history: List<HistoryEntry>,
    strings: BugeStrings,
    onUse: (HistoryEntry) -> Unit,
    onClear: () -> Unit
) {
    if (history.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.History, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(strings.noHistory, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(strings.noHistoryDescription, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClear) { Icon(Icons.Filled.Delete, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(strings.clearHistory) }
                }
            }
            items(history, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onUse(entry) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.expression, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.result, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { onUse(entry) }) { Text(strings.reuse) }
                    }
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier,
    settings: AppSettings,
    strings: BugeStrings,
    onChange: ((AppSettings) -> AppSettings) -> Unit
) {
    var showColorSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val colorOptions = listOf(
        ThemeSource.DYNAMIC to strings.dynamic, ThemeSource.LAVENDER to strings.lavender,
        ThemeSource.OCEAN to strings.ocean, ThemeSource.FOREST to strings.forest,
        ThemeSource.SUNSET to strings.sunset, ThemeSource.CUSTOM to strings.custom
    )
    if (showColorSourceDialog) {
        SingleChoiceDialog(
            title = strings.colorSource,
            options = colorOptions.map { it.second },
            selectedIndex = colorOptions.indexOfFirst { it.first == settings.themeSource },
            onDismiss = { showColorSourceDialog = false },
            onSelect = { index ->
                onChange { it.copy(themeSource = colorOptions[index].first) }
                showColorSourceDialog = false
            }
        )
    }
    if (showLanguageDialog) {
        val languages = AppLanguage.entries
        SingleChoiceDialog(
            title = strings.language,
            options = languages.map { it.displayName() },
            selectedIndex = languages.indexOf(settings.language),
            onDismiss = { showLanguageDialog = false },
            onSelect = { index ->
                onChange { it.copy(language = languages[index]) }
                showLanguageDialog = false
            }
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item { SettingsSection(strings.appearance, Icons.Filled.Palette) }
        item {
            SettingsCard {
                SettingsSelectionTab(
                    icon = Icons.Filled.Palette,
                    label = strings.colorSource,
                    value = colorOptions.firstOrNull { it.first == settings.themeSource }?.second.orEmpty(),
                    onClick = { showColorSourceDialog = true }
                )
                if (settings.themeSource == ThemeSource.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    Text(strings.customColor, style = MaterialTheme.typography.labelLarge)
                    ColorSlider(strings.red, settings.customRed, Color.Red) { onChange { current -> current.copy(customRed = it) } }
                    ColorSlider(strings.green, settings.customGreen, Color.Green) { onChange { current -> current.copy(customGreen = it) } }
                    ColorSlider(strings.blue, settings.customBlue, Color.Blue) { onChange { current -> current.copy(customBlue = it) } }
                }
            }
        }
        item {
            SettingsCard {
                Text(strings.displayMode, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to strings.system, ThemeMode.LIGHT to strings.light, ThemeMode.DARK to strings.dark).forEach { (mode, label) ->
                        FilterChip(selected = settings.themeMode == mode, onClick = { onChange { it.copy(themeMode = mode) } }, label = { Text(label) })
                    }
                }
            }
        }
        item { SettingsSection(strings.language, Icons.Filled.Language) }
        item {
            SettingsCard {
                SettingsSelectionTab(
                    icon = Icons.Filled.Language,
                    label = strings.language,
                    value = settings.language.displayName(),
                    onClick = { showLanguageDialog = true }
                )
            }
        }
        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Vibration, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Text(strings.haptics, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = settings.hapticsEnabled, onCheckedChange = { enabled -> onChange { it.copy(hapticsEnabled = enabled) } })
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) { content() }
    }
}

@Composable
private fun ThemeSourceChooser(selected: ThemeSource, strings: BugeStrings, onSelect: (ThemeSource) -> Unit) {
    val sources = listOf(
        ThemeSource.DYNAMIC to strings.dynamic, ThemeSource.LAVENDER to strings.lavender, ThemeSource.OCEAN to strings.ocean,
        ThemeSource.FOREST to strings.forest, ThemeSource.SUNSET to strings.sunset, ThemeSource.CUSTOM to strings.custom
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sources.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (source, label) ->
                    FilterChip(selected = source == selected, onClick = { onSelect(source) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SettingsSelectionTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.indices.toList()) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = { onSelect(index) })
                        Spacer(Modifier.width(10.dp))
                        Text(options[index], style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, tint: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f))
        Text((value * 255).toInt().toString(), modifier = Modifier.width(34.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun LanguageChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
}
