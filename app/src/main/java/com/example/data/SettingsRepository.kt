package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lucientness_settings", Context.MODE_PRIVATE)

    var endpoint: String
        get() = prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions"
        set(value) = prefs.edit().putString("endpoint", value).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var modelName: String
        get() = prefs.getString("model_name", "gpt-4") ?: "gpt-4"
        set(value) = prefs.edit().putString("model_name", value).apply()

    fun isConfigured(): Boolean {
        return endpoint.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }
}
