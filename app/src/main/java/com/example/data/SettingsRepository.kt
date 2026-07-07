package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lucientness_settings", Context.MODE_PRIVATE)

    // v1.3.0: endpoint and modelName now default to empty strings so the
    // AI configuration dialog opens with empty input fields. Users had to
    // manually delete the pre-filled "https://api.openai.com/v1/chat/completions"
    // and "gpt-4" defaults before typing their own values, which was
    // annoying. isConfigured() still requires all three fields to be
    // non-blank before the assistant can be opened, so the empty defaults
    // do not weaken the validation.
    var endpoint: String
        get() = prefs.getString("endpoint", "") ?: ""
        set(value) = prefs.edit().putString("endpoint", value).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var modelName: String
        get() = prefs.getString("model_name", "") ?: ""
        set(value) = prefs.edit().putString("model_name", value).apply()

    fun isConfigured(): Boolean {
        return endpoint.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }
}
