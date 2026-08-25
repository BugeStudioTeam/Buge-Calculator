package com.buge.calculator.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("buge_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        themeMode = preferences.enum("theme_mode", ThemeMode.SYSTEM),
        themeSource = preferences.enum("theme_source", ThemeSource.DYNAMIC),
        language = preferences.enum("language", AppLanguage.ENGLISH),
        hapticsEnabled = preferences.getBoolean("haptics", true),
        customRed = preferences.getFloat("custom_red", 0.40f),
        customGreen = preferences.getFloat("custom_green", 0.31f),
        customBlue = preferences.getFloat("custom_blue", 0.65f)
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString("theme_mode", settings.themeMode.name)
            .putString("theme_source", settings.themeSource.name)
            .putString("language", settings.language.name)
            .putBoolean("haptics", settings.hapticsEnabled)
            .putFloat("custom_red", settings.customRed)
            .putFloat("custom_green", settings.customGreen)
            .putFloat("custom_blue", settings.customBlue)
            .apply()
    }

    fun loadHistory(): List<HistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString("history", "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(HistoryEntry(
                    id = item.optString("id"),
                    expression = item.getString("expression"),
                    result = item.getString("result"),
                    createdAt = item.optLong("createdAt")
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveHistory(history: List<HistoryEntry>) {
        val array = JSONArray()
        history.take(100).forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("expression", entry.expression)
                put("result", entry.result)
                put("createdAt", entry.createdAt)
            })
        }
        preferences.edit().putString("history", array.toString()).apply()
    }

    fun savePendingExpression(expression: String) {
        preferences.edit().putString("pending_expression", expression).apply()
    }

    fun consumePendingExpression(): String? {
        val value = preferences.getString("pending_expression", null)
        preferences.edit().remove("pending_expression").apply()
        return value
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.enum(key: String, fallback: T): T =
        getString(key, fallback.name)?.let { saved -> enumValues<T>().firstOrNull { it.name == saved } } ?: fallback
}
