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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.SurfaceSettings
import com.buge.calculator.engine.ExpressionEngine
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FormulaSurfaceCanvas(
    surface: SurfaceSettings,
    angleUnit: AngleUnit,
    modifier: Modifier = Modifier,
    onCameraChange: (yaw: Float, pitch: Float, zoom: Float) -> Unit
) {
    var camera by remember { mutableStateOf(surface) }
    LaunchedEffect(surface.yaw, surface.pitch, surface.zoom, surface.showMesh) {
        camera = camera.copy(yaw = surface.yaw, pitch = surface.pitch, zoom = surface.zoom, showMesh = surface.showMesh)
    }
    val currentCamera by rememberUpdatedState(camera)
    val cameraCallback by rememberUpdatedState(onCameraChange)
    val formula = remember(surface.expression) { ExpressionEngine.compile(surface.expression) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val current = currentCamera
                    val next = current.copy(
                        yaw = current.yaw + pan.x / 170f,
                        pitch = (current.pitch + pan.y / 170f).coerceIn(-1.35f, 1.35f),
                        zoom = (current.zoom * zoom).coerceIn(16f, 110f)
                    )
                    camera = next
                    cameraCallback(next.yaw, next.pitch, next.zoom)
                }
            }
    ) {
        drawSurface(formula, angleUnit, camera)
    }
}

private fun DrawScope.drawSurface(
    formula: ExpressionEngine.CompiledExpression?,
    angleUnit: AngleUnit,
    camera: SurfaceSettings
) {
    val background = Color(0x12000000)
    drawRect(background)
    if (formula == null) return
    val resolution = 22
    val range = 5.2f
    val unit = range * 2f / resolution
    val yawCos = cos(camera.yaw)
    val yawSin = sin(camera.yaw)
    val pitchCos = cos(camera.pitch)
    val pitchSin = sin(camera.pitch)
    val center = Offset(size.width / 2f, size.height / 2f)

    fun point(x: Float, y: Float): SurfacePoint? {
        val z = formula.evaluate(angleUnit, variable = x.toDouble(), variableY = y.toDouble()).value?.toFloat() ?: return null
        if (!z.isFinite() || z !in -8f..8f) return null
        val rotatedX = yawCos * x - yawSin * y
        val rotatedY = yawSin * x + yawCos * y
        val projectedY = pitchCos * rotatedY - pitchSin * z
        val depth = pitchSin * rotatedY + pitchCos * z
        return SurfacePoint(
            position = Offset(center.x + rotatedX * camera.zoom, center.y - projectedY * camera.zoom),
            depth = depth,
            height = z
        )
    }

    val cells = mutableListOf<SurfaceCell>()
    for (row in 0 until resolution) {
        for (column in 0 until resolution) {
            val x = -range + column * unit
            val y = -range + row * unit
            val p1 = point(x, y)
            val p2 = point(x + unit, y)
            val p3 = point(x + unit, y + unit)
            val p4 = point(x, y + unit)
            if (p1 != null && p2 != null && p3 != null && p4 != null) {
                cells += SurfaceCell(listOf(p1, p2, p3, p4), (p1.depth + p2.depth + p3.depth + p4.depth) / 4f)
            }
        }
    }
    // Painter's order produces a coherent surface with a lightweight software renderer.
    cells.sortedBy { it.depth }.forEach { cell ->
        val path = Path().apply {
            moveTo(cell.points[0].position.x, cell.points[0].position.y)
            lineTo(cell.points[1].position.x, cell.points[1].position.y)
            lineTo(cell.points[2].position.x, cell.points[2].position.y)
            lineTo(cell.points[3].position.x, cell.points[3].position.y)
            close()
        }
        val averageHeight = cell.points.map { it.height }.average().toFloat().coerceIn(-5f, 5f)
        val lightness = ((averageHeight + 5f) / 10f)
        val fill = Color(
            red = 0.18f + 0.30f * lightness,
            green = 0.28f + 0.28f * lightness,
            blue = 0.58f + 0.28f * lightness,
            alpha = 0.86f
        )
        drawPath(path, fill)
        if (camera.showMesh) drawPath(path, Color(0x667B61FF), style = Stroke(0.65.dp.toPx()))
    }
}

private data class SurfacePoint(val position: Offset, val depth: Float, val height: Float)
private data class SurfaceCell(val points: List<SurfacePoint>, val depth: Float)
