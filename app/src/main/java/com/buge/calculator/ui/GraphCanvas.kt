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
import androidx.compose.ui.unit.dp
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.GraphSettings
import com.buge.calculator.engine.ExpressionEngine
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

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
    LaunchedEffect(graph.offsetX, graph.offsetY, graph.scale, graph.showGrid) {
        viewport = viewport.copy(
            offsetX = graph.offsetX,
            offsetY = graph.offsetY,
            scale = graph.scale,
            showGrid = graph.showGrid
        )
    }
    val latestViewport by rememberUpdatedState(viewport)
    val latestViewportCallback by rememberUpdatedState(onViewportChange)
    val compiledExpression = remember(graph.expression) { ExpressionEngine.compile(graph.expression) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
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
