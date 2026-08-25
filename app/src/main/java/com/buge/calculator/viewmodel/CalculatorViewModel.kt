package com.buge.calculator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.AppSettings
import com.buge.calculator.data.CalculatorMode
import com.buge.calculator.data.CalculatorState
import com.buge.calculator.data.GraphSettings
import com.buge.calculator.data.HistoryEntry
import com.buge.calculator.data.NumberFormatter
import com.buge.calculator.data.SettingsStore
import com.buge.calculator.engine.ExpressionEngine
import com.buge.calculator.engine.PythonMathEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _state = MutableStateFlow(
        CalculatorState(history = settingsStore.loadHistory())
    )
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    fun input(token: String) {
        val current = _state.value
        val next = current.expression + token
        updateExpression(next)
    }

    fun replaceExpression(expression: String) = updateExpression(expression)

    fun clear() {
        _state.value = _state.value.copy(expression = "", preview = "0", error = null)
    }

    fun delete() {
        val current = _state.value.expression
        updateExpression(current.dropLast(1))
    }

    fun evaluate() {
        val current = _state.value
        if (current.expression.isBlank()) return
        val result = ExpressionEngine.evaluate(current.expression, current.angleUnit, answer = current.lastAnswer)
        if (result.isSuccess) {
            val formatted = NumberFormatter.format(result.value!!)
            val history = listOf(HistoryEntry(expression = current.expression, result = formatted)) + current.history
            settingsStore.saveHistory(history)
            _state.value = current.copy(
                expression = formatted,
                preview = formatted,
                lastAnswer = result.value,
                history = history,
                error = null
            )
        } else {
            _state.value = current.copy(error = result.error ?: "Invalid expression", preview = "Error")
        }
    }

    fun setAngleUnit(angleUnit: AngleUnit) {
        _state.value = _state.value.copy(angleUnit = angleUnit)
        refreshPreview()
    }

    fun setMode(mode: CalculatorMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    fun useHistory(entry: HistoryEntry) {
        updateExpression(entry.expression)
    }

    fun prepareHistoryReuse(entry: HistoryEntry) {
        settingsStore.savePendingExpression(entry.expression)
    }

    fun refreshFromActivities() {
        _settings.value = settingsStore.load()
        val pendingExpression = settingsStore.consumePendingExpression()
        _state.value = _state.value.copy(history = settingsStore.loadHistory())
        if (pendingExpression != null) updateExpression(pendingExpression)
    }

    fun clearHistory() {
        settingsStore.saveHistory(emptyList())
        _state.value = _state.value.copy(history = emptyList())
    }

    fun setGraphExpression(expression: String) {
        _state.value = _state.value.copy(graph = _state.value.graph.copy(expression = expression))
    }

    fun setGraphGrid(enabled: Boolean) {
        _state.value = _state.value.copy(graph = _state.value.graph.copy(showGrid = enabled))
    }

    fun updateGraphViewport(offsetX: Float, offsetY: Float, scale: Float) {
        _state.value = _state.value.copy(
            graph = _state.value.graph.copy(offsetX = offsetX, offsetY = offsetY, scale = scale.coerceIn(12f, 250f))
        )
    }

    fun resetGraphView() {
        _state.value = _state.value.copy(graph = _state.value.graph.copy(offsetX = 0f, offsetY = 0f, scale = 42f))
    }

    fun setSurfaceExpression(expression: String) {
        _state.value = _state.value.copy(surface = _state.value.surface.copy(expression = expression))
    }

    fun setSurfaceMesh(enabled: Boolean) {
        _state.value = _state.value.copy(surface = _state.value.surface.copy(showMesh = enabled))
    }

    fun updateSurfaceCamera(yaw: Float, pitch: Float, zoom: Float) {
        _state.value = _state.value.copy(
            surface = _state.value.surface.copy(
                yaw = yaw,
                pitch = pitch.coerceIn(-1.35f, 1.35f),
                zoom = zoom.coerceIn(16f, 110f)
            )
        )
    }

    fun resetSurfaceCamera() {
        _state.value = _state.value.copy(surface = _state.value.surface.copy(yaw = 0.62f, pitch = -0.48f, zoom = 36f))
    }

    fun setPythonCode(code: String) {
        _state.value = _state.value.copy(python = _state.value.python.copy(code = code, error = null))
    }

    fun runPython() {
        val code = _state.value.python.code
        _state.value = _state.value.copy(python = _state.value.python.copy(output = "", error = null))
        viewModelScope.launch(Dispatchers.Default) {
            val result = PythonMathEngine.execute(getApplication<Application>(), code)
            _state.value = _state.value.copy(
                python = _state.value.python.copy(
                    output = result.output.orEmpty(),
                    error = result.error
                )
            )
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
        settingsStore.save(_settings.value)
    }

    private fun updateExpression(expression: String) {
        _state.value = _state.value.copy(expression = expression, error = null)
        refreshPreview()
    }

    private fun refreshPreview() {
        val current = _state.value
        if (current.expression.isBlank()) {
            _state.value = current.copy(preview = "0", error = null)
            return
        }
        val result = ExpressionEngine.evaluate(current.expression, current.angleUnit, answer = current.lastAnswer)
        _state.value = if (result.isSuccess) {
            current.copy(preview = NumberFormatter.format(result.value!!), error = null)
        } else {
            current.copy(preview = "…", error = null)
        }
    }
}
