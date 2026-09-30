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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.GraphSettings
import com.buge.calculator.engine.ExpressionEngine
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun FunctionGraphCanvas(
    graph: GraphSettings,
    angleUnit: AngleUnit,
    modifier: Modifier = Modifier,
    onViewportChange: (offsetX: Float, offsetY: Float, scale: Float) -> Unit
) {
    // Keep viewport movement local to the canvas. This makes pinch/drag smooth even while the
    // view-model persists the latest position in parallel.
    var viewport by remember { mutableStateOf(graph) }
    // Canvas size is captured so the auto-fit pass can reason about real pixels without
    // forcing a draw. It is refreshed on every layout pass.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Remember which expression the current viewport was auto-fitted for. Auto-fit must run at
    // most once per expression, otherwise user zoom/pan would be overwritten on every
    // recomposition.
    var autoFittedExpression by remember { mutableStateOf<String?>(null) }
    // Sync from the view-model only for genuine external changes (e.g. the Reset button or a
    // restored session). IMPORTANT: this must not undo the auto-fit. Auto-fit intentionally does
    // NOT write its scale back to the view-model, so graph.scale keeps its stale default (42f);
    // blindly applying it here would snap sin(x) back to a flat line on the next recomposition
    // (for instance when the grid toggle changes). We therefore re-apply the persistent values
    // only when the user has not yet been auto-fitted for this expression, or when the incoming
    // scale differs from what we would have fitted (a real external reset).
    LaunchedEffect(graph.offsetX, graph.offsetY, graph.scale, graph.showGrid) {
        val syncViewport = autoFittedExpression != graph.expression || graph.scale != viewport.scale
        // A Reset (view-model scale back to the default 42f) should re-fit, not show a flat line.
        if (syncViewport && graph.scale == DEFAULT_GRAPH_SCALE) autoFittedExpression = null
        viewport = viewport.copy(
            offsetX = if (syncViewport) graph.offsetX else viewport.offsetX,
            offsetY = if (syncViewport) graph.offsetY else viewport.offsetY,
            scale = if (syncViewport) graph.scale else viewport.scale,
            showGrid = graph.showGrid
        )
    }
    val latestViewport by rememberUpdatedState(viewport)
    val latestViewportCallback by rememberUpdatedState(onViewportChange)
    val compiledExpression = remember(graph.expression) { ExpressionEngine.compile(graph.expression) }

    // Auto-fit runs once per expression, as soon as the canvas has a real size. It keeps the
    // axes 1:1 equidistant and only rescales/pans: it never distorts one axis, so the picture
    // stays mathematically correct while small-amplitude curves such as sin(x) fill the canvas
    // instead of hugging the x-axis.
    //
    // The result is applied to the local viewport ONLY. It is deliberately NOT pushed back to
    // the view-model: doing so would overwrite the persisted scale with the fitted value and
    // (via the state->viewport sync above) fight with the auto-fit, leaving sin(x) flat again.
    LaunchedEffect(graph.expression, canvasSize) {
        val size = canvasSize
        if (size.width == 0 || size.height == 0) return@LaunchedEffect
        if (autoFittedExpression == graph.expression) return@LaunchedEffect
        val fitted = autoFitViewport(
            expression = compiledExpression,
            angleUnit = angleUnit,
            widthPx = size.width.toFloat(),
            heightPx = size.height.toFloat()
        ) ?: return@LaunchedEffect
        autoFittedExpression = graph.expression
        viewport = viewport.copy(
            scale = fitted.scale,
            offsetX = 0f,
            offsetY = fitted.offsetY
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val current = latestViewport
                    val next = current.copy(
                        offsetX = current.offsetX + pan.x,
                        offsetY = current.offsetY + pan.y,
                        scale = (current.scale * zoom).coerceIn(12f, 250f)
                    )
                    viewport = next
                    latestViewportCallback(next.offsetX, next.offsetY, next.scale)
                }
            }
    ) {
        val center = Offset(size.width / 2f + viewport.offsetX, size.height / 2f + viewport.offsetY)
        val gridUnit = preferredGridUnit(viewport.scale)
        if (viewport.showGrid) drawGrid(center, viewport.scale, gridUnit)
        drawAxes(center)
        drawFunction(compiledExpression, angleUnit, center, viewport.scale)
    }
}

/**
 * Chooses a viewport that makes the curve fill the canvas while keeping both axes at the
 * same scale (1 unit x == 1 unit y in pixels). The vertical offset is recentred so the curve
 * is vertically centred rather than pushed against an edge.
 *
 * Returns null when the function cannot be sampled (invalid expression) or has no finite,
 * reasonably sized values in view, in which case the caller keeps the current viewport.
 */
private fun autoFitViewport(
    expression: ExpressionEngine.CompiledExpression?,
    angleUnit: AngleUnit,
    widthPx: Float,
    heightPx: Float
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

    // Vertical padding: leave ~10% head/foot room so peaks stay visible.
    val verticalPadding = 1.15f
    val scaleFromHeight = heightPx / (2f * amplitude * verticalPadding)
    // Do not zoom in beyond a pleasant maximum (isolated vertical lines look odd), and keep
    // within the same interactive bounds used by pinch-zoom.
    val scale = scaleFromHeight.coerceIn(12f, 250f)

    // Centre vertically on the mid of the sampled range, clamped so the axes remain on screen.
    val minY = values.min()
    val maxY = values.max()
    val midY = (minY + maxY) / 2f
    // pixel offset = -midY * scale because screen y grows downward.
    val rawOffsetY = -midY * scale
    // Keep the origin within the canvas so the axes are visible.
    val maxOffsetY = heightPx / 2f
    val offsetY = rawOffsetY.coerceIn(-maxOffsetY, maxOffsetY)

    return FitResult(scale = scale, offsetY = offsetY)
}

private data class FitResult(val scale: Float, val offsetY: Float)

/** Default scale used by [com.buge.calculator.data.GraphSettings]; a Reset restores this value. */
private const val DEFAULT_GRAPH_SCALE = 42f

private fun DrawScope.drawGrid(center: Offset, scale: Float, gridUnit: Float) {
    val step = gridUnit * scale
    val minorColor = Color(0x332D2F34)
    val startX = center.x - floor(center.x / step) * step
    var x = startX
    while (x <= size.width) {
        drawLine(minorColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        x += step
    }
    x = startX - step
    while (x >= 0f) {
        drawLine(minorColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        x -= step
    }
    val startY = center.y - floor(center.y / step) * step
    var y = startY
    while (y <= size.height) {
        drawLine(minorColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += step
    }
    y = startY - step
    while (y >= 0f) {
        drawLine(minorColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y -= step
    }
}

private fun DrawScope.drawAxes(center: Offset) {
    val axisColor = Color(0xFF62656D)
    if (center.x in 0f..size.width) drawLine(axisColor, Offset(center.x, 0f), Offset(center.x, size.height), 1.5.dp.toPx())
    if (center.y in 0f..size.height) drawLine(axisColor, Offset(0f, center.y), Offset(size.width, center.y), 1.5.dp.toPx())
}

private fun DrawScope.drawFunction(
    expression: ExpressionEngine.CompiledExpression?,
    angleUnit: AngleUnit,
    center: Offset,
    scale: Float
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
        val xValue = (pixelX - center.x) / scale
        val yValue = expression.evaluate(angleUnit, variable = xValue.toDouble()).value?.toFloat()
        if (yValue == null || !yValue.isFinite()) {
            hasPreviousPoint = false
            continue
        }
        val pixelY = center.y - yValue * scale
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
