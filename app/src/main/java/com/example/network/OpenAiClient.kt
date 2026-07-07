package com.example.network

import com.example.data.SettingsRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenAiRequest(
    val model: String,
    val messages: List<Message>
)

@JsonClass(generateAdapter = true)
data class Message(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiResponse(
    val choices: List<Choice>?
)

@JsonClass(generateAdapter = true)
data class Choice(
    val message: Message?
)

object OpenAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(OpenAiRequest::class.java)
    private val responseAdapter = moshi.adapter(OpenAiResponse::class.java)

    suspend fun generateContent(settings: SettingsRepository, systemPrompt: String, userPrompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = OpenAiRequest(
                    model = settings.modelName,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = userPrompt)
                    )
                )

                val jsonStr = requestAdapter.toJson(requestBody)
                val body = jsonStr.toRequestBody("application/json".toMediaType())

                var url = settings.endpoint
                if (!url.endsWith("/chat/completions")) {
                    if (!url.endsWith("/")) url += "/"
                    url += "chat/completions"
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${settings.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    val apiResponse = responseAdapter.fromJson(responseStr)
                    apiResponse?.choices?.firstOrNull()?.message?.content ?: "Empty response"
                } else {
                    "Error: ${response.code} - ${response.message}"
                }
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }
}
