package com.buge.calculator.engine

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

data class PythonExecutionResult(
    val output: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
}

object PythonMathEngine {
    fun execute(context: Context, code: String): PythonExecutionResult {
        return try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            val raw = Python.getInstance()
                .getModule("safe_math")
                .callAttr("execute", code)
                .toString()
            val result = JSONObject(raw)
            if (result.optBoolean("ok")) {
                PythonExecutionResult(output = result.optString("output", "Completed."))
            } else {
                PythonExecutionResult(error = result.optString("error", "Python execution failed."))
            }
        } catch (exception: Exception) {
            PythonExecutionResult(error = exception.message ?: "Python runtime failed to start.")
        }
    }
}
