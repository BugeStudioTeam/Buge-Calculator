package com.buge.calculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.GraphSettings
import com.buge.calculator.engine.ExpressionEngine
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Maps between screen pixels and graph values using **independent** horizontal and vertical scales.
 *
 * Historically both axes shared one `scale` (1 unit x == 1 unit y on screen). That equidistant
 * constraint is mathematically incompatible with drawing a *recognisable* wave such as sin(x):
 * showing 1–2 full periods horizontally requires a small scale, which makes the ±1 amplitude only a
 * few percent of the screen height (a "flat line"). Conversely, making ±1 prominent vertically
 * requires a large scale, which fits less than one period across the screen.
 *
 * The viewport therefore carries `scale` (horizontal) and `scaleY` (vertical) separately
 * (anisotropic scaling). A user toggle ([GraphSettings.lockAspect]) can force them equal again for
 * those who prefer true equidistance.
 */

@Composable
fun FunctionGraphCanvas(
    graph: GraphSettings,
    angleUnit: AngleUnit,
    modifier: Modifier = Modifier,
    onViewportChange: (offsetX: Float, offsetY: Float, scaleX: Float, scaleY: Float) -> Unit
) {
    // Keep viewport movement local to the canvas. This makes pinch/drag smooth even while the
    // view-model persists the latest position in parallel.
    var viewport by remember { mutableStateOf(graph) }
    // Canvas size is captured so the auto-fit pass can reason about real pixels without
    // forcing a draw. It is refreshed on every layout pass.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Remember which expression the current viewport was auto-fitted for. Auto-fit must run at
    // most once per expression, otherwise user zoom/pan would be overwritten on every
    // recomposition. The lock-aspect flag is part of the key so toggling it re-fits.
    var autoFitKey by remember { mutableStateOf<String?>(null) }
    // The last viewport we pushed to the view-model. Any incoming `graph` that matches this is an
    // echo of our own gesture/auto-fit and must be ignored. Any other value is a genuine external
    // change (Reset button, restored session) and should be adopted.
    var lastEmitted by remember { mutableStateOf<ViewportEcho?>(null) }
    LaunchedEffect(graph.offsetX, graph.offsetY, graph.scale, graph.scaleY, graph.showGrid, graph.lockAspect) {
        val incoming = ViewportEcho(graph.offsetX, graph.offsetY, graph.scale, graph.scaleY)
        val isEcho = incoming == lastEmitted
        if (!isEcho) {
            // Genuine external change: adopt it and treat the view as freshly (un)fitted so the
            // auto-fit pass re-evaluates. A Reset (scales back to the default) re-fits; any other
            // external jump simply adopts the incoming viewport.
            viewport = viewport.copy(
                offsetX = graph.offsetX,
                offsetY = graph.offsetY,
                scale = graph.scale,
                scaleY = graph.scaleY
            )
            if (graph.scale == DEFAULT_GRAPH_SCALE && graph.scaleY == DEFAULT_GRAPH_SCALE) {
                autoFitKey = null
            }
        }
        // These are cosmetic/structural and must always be reflected, echo or not.
        if (viewport.showGrid != graph.showGrid) {
            viewport = viewport.copy(showGrid = graph.showGrid)
        }
        if (viewport.lockAspect != graph.lockAspect) {
            viewport = viewport.copy(lockAspect = graph.lockAspect)
        }
    }
    val latestViewport by rememberUpdatedState(viewport)
    val latestViewportCallback by rememberUpdatedState(onViewportChange)
    val compiledExpression = remember(graph.expression) { ExpressionEngine.compile(graph.expression) }
    // Shared text measurer for axis tick labels. Reused across frames so measuring is cheap.
    val textMeasurer = rememberTextMeasurer()

    // Auto-fit runs once per (expression, lockAspect) pair, as soon as the canvas has a real size.
    //
    // It picks the horizontal scale so a small number of periods (or a sensible span for
    // non-periodic functions) fill the width, and the vertical scale so the curve's amplitude fills
    // the height. The two are chosen independently so sin(x) finally looks like a wave. When
    // [GraphSettings.lockAspect] is on, the horizontal scale wins and the vertical one is forced to
    // match it, restoring equidistance.
    //
    // The result is applied to the local viewport ONLY. It is deliberately NOT pushed back to
    // the view-model: doing so would overwrite the persisted scale with the fitted value and
    // (via the state->viewport sync above) fight with the auto-fit, leaving sin(x) flat again.
    LaunchedEffect(graph.expression, canvasSize, graph.lockAspect) {
        val size = canvasSize
        if (size.width == 0 || size.height == 0) return@LaunchedEffect
        val key = graph.expression + "|" + graph.lockAspect
        if (autoFitKey == key) return@LaunchedEffect
        val fitted = autoFitViewport(
            expression = compiledExpression,
            angleUnit = angleUnit,
            widthPx = size.width.toFloat(),
            heightPx = size.height.toFloat(),
            lockAspect = graph.lockAspect
        ) ?: return@LaunchedEffect
        autoFitKey = key
        viewport = viewport.copy(
            scale = fitted.scaleX,
            scaleY = fitted.scaleY,
            offsetX = 0f,
            offsetY = fitted.offsetY
        )
        // Record the fitted viewport as "ours" WITHOUT emitting it to the view-model. Otherwise the
        // persisted scale would be overwritten and the echo guard would be defeated, which is what
        // previously made a zoomed grid and curve disagree.
        lastEmitted = ViewportEcho(0f, fitted.offsetY, fitted.scaleX, fitted.scaleY)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val current = latestViewport
                    // Zoom scales both axes; when the aspect is locked they must stay equal. The
                    // horizontal scale drives the lock so a pinch never breaks the ratio.
                    val nextScaleX = (current.scale * zoom).coerceIn(MIN_GRAPH_SCALE, MAX_GRAPH_SCALE)
                    val nextScaleY = if (current.lockAspect) {
                        nextScaleX
                    } else {
                        (current.scaleY * zoom).coerceIn(MIN_GRAPH_SCALE, MAX_GRAPH_SCALE)
                    }
                    val next = current.copy(
                        offsetX = current.offsetX + pan.x,
                        offsetY = current.offsetY + pan.y,
                        scale = nextScaleX,
                        scaleY = nextScaleY
                    )
                    viewport = next
                    lastEmitted = ViewportEcho(next.offsetX, next.offsetY, next.scale, next.scaleY)
                    latestViewportCallback(next.offsetX, next.offsetY, next.scale, next.scaleY)
                }
            }
    ) {
        val center = Offset(size.width / 2f + viewport.offsetX, size.height / 2f + viewport.offsetY)
        // Each axis gets its own "nice" grid unit so tick labels stay readable at any aspect.
        val gridUnitX = preferredGridUnit(viewport.scale)
        val gridUnitY = preferredGridUnit(viewport.scaleY)
        if (viewport.showGrid) {
            drawGrid(center, viewport.scale, viewport.scaleY, gridUnitX, gridUnitY)
        }
        drawAxes(center)
        drawAxisLabels(textMeasurer, center, viewport.scale, viewport.scaleY, gridUnitX, gridUnitY)
        drawFunction(compiledExpression, angleUnit, center, viewport.scale, viewport.scaleY)
    }
}

/**
 * Chooses a viewport that makes the curve fill the canvas. Horizontal and vertical scales are
 * computed **independently** (anisotropic) so a wave like sin(x) shows both its periodicity and its
 * amplitude; when [lockAspect] is true the horizontal scale is reused for both axes (1:1).
 *
 * The vertical offset is recentred so the curve is vertically centred rather than pushed against an
 * edge.
 *
 * Returns null when the function cannot be sampled (invalid expression) or has no finite,
 * reasonably sized values in view, in which case the caller keeps the current viewport.
 */
private fun autoFitViewport(
    expression: ExpressionEngine.CompiledExpression?,
    angleUnit: AngleUnit,
    widthPx: Float,
    heightPx: Float,
    lockAspect: Boolean
): FitResult? {
    if (expression == null || widthPx <= 0f || heightPx <= 0f) return null

    // Sample across a generous horizontal window; the actual window depends on the scale we
    // are about to choose, so we first sample on a fixed wide domain and derive amplitude.
    val sampleCount = 400
    val sampleDomain = 40f // x in [-20, 20] is wide enough for typical on-screen ranges
    val values = ArrayList<Float>(sampleCount + 1)
    for (i in 0..sampleCount) {
        val x = -sampleDomain + (2f * sampleDomain) * i / sampleCount
        val y = expression.evaluate(angleUnit, variable = x.toDouble()).value?.toFloat() ?: continue
        if (y.isFinite() && abs(y) < 1e6f) values += y
    }
    if (values.isEmpty()) return null

    // Use a high percentile of |y| instead of the max so a single narrow spike (e.g. tan)
    // does not collapse the whole curve into a flat line.
    val magnitudes = values.map { abs(it) }.sorted()
    val percentileIndex = (magnitudes.size * 0.9f).roundToInt().coerceIn(0, magnitudes.size - 1)
    val amplitude = magnitudes[percentileIndex]
    if (!amplitude.isFinite() || amplitude <= 1e-6f) return null

    // ---- Vertical scale: make the curve's amplitude fill the height (with ~10% padding). ----
    val verticalPadding = 1.15f
    val scaleFromHeight = heightPx / (2f * amplitude * verticalPadding)
    val scaleY = scaleFromHeight.coerceIn(MIN_GRAPH_SCALE, MAX_GRAPH_SCALE)

    // ---- Horizontal scale: show a small, pleasant number of x-units across the width. ----
    // Detect periodicity cheaply; if a period is found we show ~3 of them, otherwise we fall back
    // to a fixed span that suits polynomials and other aperiodic functions.
    val period = estimatePeriod(expression, angleUnit)
    val spanUnits = if (period != null) period * WAVE_PERIODS_ON_SCREEN else DEFAULT_X_SPAN_UNITS
    val scaleFromWidth = widthPx / spanUnits
    val scaleX = scaleFromWidth.coerceIn(MIN_GRAPH_SCALE, MAX_GRAPH_SCALE)

    // Equidistance lock: the horizontal scale wins and drives the vertical one.
    val finalScaleY = if (lockAspect) scaleX else scaleY

    // Centre vertically on the mid of the sampled range, clamped so the axes remain on screen.
    val minY = values.min()
    val maxY = values.max()
    val midY = (minY + maxY) / 2f
    // pixel offset = -midY * scaleY because screen y grows downward.
    val rawOffsetY = -midY * finalScaleY
    // Keep the origin within the canvas so the axes are visible.
    val maxOffsetY = heightPx / 2f
    val offsetY = rawOffsetY.coerceIn(-maxOffsetY, maxOffsetY)

    return FitResult(scaleX = scaleX, scaleY = finalScaleY, offsetY = offsetY)
}

/**
 * Cheaply estimates the fundamental period of a periodic function by scanning for the first x in
 * (0, maxScan] at which f(x) returns close to f(0) once it has moved away from the value at 0.
 *
 * Returns null when no clear period is found (e.g. polynomials, exponentials, monotonic functions)
 * so the caller can fall back to a fixed horizontal span.
 */
private fun estimatePeriod(
    expression: ExpressionEngine.CompiledExpression,
    angleUnit: AngleUnit
): Float? {
    val maxScan = 20f
    val stride = 0.002f
    val tolerance = 0.02f
    val f0 = expression.evaluate(angleUnit, variable = 0.0).value?.toFloat() ?: return null
    if (!f0.isFinite()) return null

    var x = stride
    var movedAway = false
    while (x <= maxScan) {
        val y = expression.evaluate(angleUnit, variable = x.toDouble()).value?.toFloat()
        if (y == null || !y.isFinite()) return null
        if (!movedAway) {
            if (abs(y - f0) > 0.15f) movedAway = true
        } else if (abs(y - f0) <= tolerance) {
            return x
        }
        x += stride
    }
    return null
}

private data class FitResult(val scaleX: Float, val scaleY: Float, val offsetY: Float)

/**
 * Snapshot of the viewport fields we emit back to the view-model. Used to recognise the returning
 * state update as an echo of our own action rather than an external change.
 */
private data class ViewportEcho(
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val scaleY: Float
)

/** Default scale used by [com.buge.calculator.data.GraphSettings]; a Reset restores this value. */
private const val DEFAULT_GRAPH_SCALE = 42f

/** Interactive bounds for both scales. */
private const val MIN_GRAPH_SCALE = 12f
private const val MAX_GRAPH_SCALE = 250f

/** Horizontal span (in x-units) used when the function has no detectable period. */
private const val DEFAULT_X_SPAN_UNITS = 12f

/** How many periods of a periodic function to fit across the width during auto-fit. */
private const val WAVE_PERIODS_ON_SCREEN = 3f

private fun DrawScope.drawGrid(
    center: Offset,
    scaleX: Float,
    scaleY: Float,
    gridUnitX: Float,
    gridUnitY: Float
) {
    // Vertical lines are spaced by the x-unit grid; horizontal lines by the y-unit grid. Each axis
    // now has its own pixel step because the scales can differ.
    val stepX = gridUnitX * scaleX
    val stepY = gridUnitY * scaleY
    val minorColor = Color(0x332D2F34)
    if (stepX > 0f && stepX.isFinite()) {
        val startX = center.x - floor(center.x / stepX) * stepX
        var x = startX
        while (x <= size.width) {
            drawLine(minorColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            x += stepX
        }
        x = startX - stepX
        while (x >= 0f) {
            drawLine(minorColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            x -= stepX
        }
    }
    if (stepY > 0f && stepY.isFinite()) {
        val startY = center.y - floor(center.y / stepY) * stepY
        var y = startY
        while (y <= size.height) {
            drawLine(minorColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += stepY
        }
        y = startY - stepY
        while (y >= 0f) {
            drawLine(minorColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y -= stepY
        }
    }
}

private fun DrawScope.drawAxes(center: Offset) {
    val axisColor = Color(0xFF62656D)
    if (center.x in 0f..size.width) drawLine(axisColor, Offset(center.x, 0f), Offset(center.x, size.height), 1.5.dp.toPx())
    if (center.y in 0f..size.height) drawLine(axisColor, Offset(0f, center.y), Offset(size.width, center.y), 1.5.dp.toPx())
}

/**
 * Draws numeric tick labels along both axes. The x-axis uses [gridUnitX] * [scaleX] and the y-axis
 * uses [gridUnitY] * [scaleY], so the labels stay meaningful even when the two axes have different
 * scales (anisotropic view). Only ticks inside the canvas are labelled, and the origin is drawn once
 * as a single "0".
 *
 * Labels sit just outside the axes when they are on-screen; when an axis is scrolled off-screen the
 * labels are pinned to the corresponding edge so the user always knows the current scale.
 */
private fun DrawScope.drawAxisLabels(
    textMeasurer: TextMeasurer,
    center: Offset,
    scaleX: Float,
    scaleY: Float,
    gridUnitX: Float,
    gridUnitY: Float
) {
    val labelColor = Color(0xFF5A5D64)
    val labelStyle = TextStyle(color = labelColor, fontSize = 11.sp)
    // Skip labels when ticks are too dense to read (keep at least ~34px between labels); computed
    // per axis because each axis now has its own pixel step.
    val minSpacingPx = 34f

    val stepX = gridUnitX * scaleX
    if (stepX > 0f && stepX.isFinite()) {
        val tickEvery = maxOf(1, ceil(minSpacingPx / stepX).toInt())
        // X axis labels: numbers below the x-axis line (or pinned to the bottom edge if off-screen).
        val axisY = center.y.coerceIn(0f, size.height)
        var index = floor((0f - center.x) / stepX).toInt() - 1
        var ticks = 0
        while (true) {
            val pixelX = center.x + index * stepX
            if (pixelX > size.width) break
            if (pixelX >= 0f && index % tickEvery == 0 && index != 0) {
                val value = index * gridUnitX
                val text = formatTick(value)
                val layout = textMeasurer.measure(text, labelStyle)
                val labelY = (axisY + 4.dp.toPx()).coerceAtMost(size.height - layout.size.height)
                val labelX = (pixelX - layout.size.width / 2f)
                    .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(labelX, labelY))
                ticks++
            }
            index++
            if (ticks > 200) break
        }
    }

    val stepY = gridUnitY * scaleY
    if (stepY > 0f && stepY.isFinite()) {
        val tickEvery = maxOf(1, ceil(minSpacingPx / stepY).toInt())
        // Y axis labels: numbers to the left of the y-axis line (or pinned to the left edge).
        val axisX = center.x.coerceIn(0f, size.width)
        var index = floor((0f - center.y) / stepY).toInt() - 1
        var ticks = 0
        while (true) {
            val pixelY = center.y + index * stepY
            if (pixelY > size.height) break
            if (pixelY >= 0f && index % tickEvery == 0 && index != 0) {
                val value = -index * gridUnitY
                val text = formatTick(value)
                val layout = textMeasurer.measure(text, labelStyle)
                val labelX = (axisX - 6.dp.toPx() - layout.size.width)
                    .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
                val labelY = (pixelY - layout.size.height / 2f)
                    .coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(labelX, labelY))
                ticks++
            }
            index++
            if (ticks > 200) break
        }
    }

    // Origin: a single "0" placed at the bottom-left of the crossing point, only if visible.
    if (center.x in 0f..size.width && center.y in 0f..size.height) {
        val layout = textMeasurer.measure("0", labelStyle)
        val originX = (center.x - 4.dp.toPx() - layout.size.width)
            .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        val originY = (center.y + 4.dp.toPx()).coerceAtMost(size.height - layout.size.height)
        drawText(layout, topLeft = Offset(originX, originY))
    }
}

/** Formats a tick value without trailing floating-point noise (e.g. 0.3 instead of 0.30000001). */
private fun formatTick(value: Float): String {
    if (value == 0f) return "0"
    val absValue = abs(value)
    return when {
        absValue >= 100000f || absValue < 0.001f -> {
            // Exponential for extreme magnitudes, trimmed of trailing zeros.
            String.format(Locale.US, "%.1e", value).replace(".0e", "e")
        }
        absValue >= 1000f -> {
            if (value % 1f == 0f) value.toLong().toString()
            else String.format(Locale.US, "%.0f", value)
        }
        absValue >= 1f -> {
            if (value % 1f == 0f) value.toLong().toString()
            else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
        else -> String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
    }
}


private fun DrawScope.drawFunction(
    expression: ExpressionEngine.CompiledExpression?,
    angleUnit: AngleUnit,
    center: Offset,
    scaleX: Float,
    scaleY: Float
) {
    if (expression == null) return
    val graphColor = Color(0xFF6750A4)
    // Sampling at three pixels is visually smooth on phone-density displays while reducing
    // mathematical evaluations by one third compared with the previous two-pixel loop.
    val pixelStep = 3
    val path = Path()
    var hasPreviousPoint = false
    var previousY = Float.NaN
    for (pixelX in 0..size.width.toInt() step pixelStep) {
        val xValue = (pixelX - center.x) / scaleX
        val yValue = expression.evaluate(angleUnit, variable = xValue.toDouble()).value?.toFloat()
        if (yValue == null || !yValue.isFinite()) {
            hasPreviousPoint = false
            continue
        }
        val pixelY = center.y - yValue * scaleY
        val visible = pixelY in -size.height * 1.5f..size.height * 2.5f
        val continuous = hasPreviousPoint && abs(pixelY - previousY) < size.height * 0.8f
        if (!visible || !continuous) {
            path.moveTo(pixelX.toFloat(), pixelY)
        } else {
            path.lineTo(pixelX.toFloat(), pixelY)
        }
        hasPreviousPoint = visible
        previousY = pixelY
    }
    drawPath(path, graphColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

private fun preferredGridUnit(scale: Float): Float {
    val targetUnits = 64f / scale
    val power = floor(log10(targetUnits.toDouble())).toInt()
    val base = 10.0.pow(power.toDouble()).toFloat()
    val candidates = listOf(1f, 2f, 5f, 10f).map { it * base }
    return candidates.minByOrNull { abs(it - targetUnits) } ?: 1f
}