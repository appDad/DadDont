package com.egabel.daddont.api.gemini

import com.egabel.daddont.BuildConfig
import com.egabel.daddont.data.model.ReturnEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TalkMeDownClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()

    suspend fun chat(
        impulseText: String,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val systemPrompt = buildSystemPrompt(impulseText, returnLog, priorTranscript)
        val requestBody = buildChatRequest(systemPrompt, userMessage)

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Gemini")

        if (!response.isSuccessful) {
            throw Exception("Gemini API error ${response.code}: $body")
        }

        val parsed = json.decodeFromString<GeminiResponse>(body)
        parsed.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: "I couldn't formulate a response. Try again?"
    }

    private fun buildSystemPrompt(
        impulseText: String,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?
    ): String {
        val returnHistory = if (returnLog.isEmpty()) {
            "No prior returns."
        } else {
            returnLog.joinToString("\n") { event ->
                val reason = event.rationale?.let { " — \"$it\"" } ?: ""
                "  Return #${returnLog.indexOf(event) + 1}$reason"
            }
        }

        val priorContext = if (priorTranscript != null) {
            "\n\nPrior conversation about this impulse:\n$priorTranscript"
        } else ""

        return """
You are an incisive friend helping someone examine an impulse they're wrestling with. Not a therapist, not a motivational poster — an honest friend who knows the history.

The impulse: "$impulseText"
Times it has returned: ${returnLog.size}
Return history:
$returnHistory$priorContext

Your approach:
- Surface the user's own prior reasons for letting go
- Reference their history concretely ("You've been here three times...")
- Ask honest questions, don't lecture
- If they have good reasons this time, acknowledge it
- Keep responses conversational and under 150 words
- Don't be preachy or use clichés
        """.trimIndent()
    }

    private fun buildChatRequest(systemPrompt: String, userMessage: String): String {
        return """
        {
            "system_instruction": {
                "parts": [{"text": ${Json.encodeToString(kotlinx.serialization.serializer<String>(), systemPrompt)}}]
            },
            "contents": [
                {
                    "parts": [{"text": ${Json.encodeToString(kotlinx.serialization.serializer<String>(), userMessage)}}]
                }
            ]
        }
        """.trimIndent()
    }
}
