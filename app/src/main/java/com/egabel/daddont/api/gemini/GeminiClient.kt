package com.egabel.daddont.api.gemini

import com.egabel.daddont.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()

    suspend fun classify(impulseText: String): GeminiClassification = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val requestBody = buildClassificationRequest(impulseText)
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Gemini")

        if (!response.isSuccessful) {
            throw Exception("Gemini API error ${response.code}: $body")
        }

        parseClassificationResponse(body)
    }

    private fun buildClassificationRequest(impulseText: String): String {
        val systemPrompt = CLASSIFICATION_SYSTEM_PROMPT
        val userPrompt = "Classify this impulse: \"$impulseText\""

        return """
        {
            "system_instruction": {
                "parts": [{"text": ${Json.encodeToString(kotlinx.serialization.serializer<String>(), systemPrompt)}}]
            },
            "contents": [
                {
                    "parts": [{"text": ${Json.encodeToString(kotlinx.serialization.serializer<String>(), userPrompt)}}]
                }
            ],
            "generationConfig": {
                "responseMimeType": "application/json",
                "responseSchema": {
                    "type": "OBJECT",
                    "properties": {
                        "tier": {"type": "STRING", "enum": ["LOW", "MEDIUM", "HIGH"]},
                        "category": {"type": "STRING", "enum": ["PURCHASE", "IDEA", "COMMUNICATION", "COMMITMENT", "OTHER"]},
                        "ramonaGate": {"type": "BOOLEAN"},
                        "ramonaReason": {"type": "STRING"}
                    },
                    "required": ["tier", "category", "ramonaGate"]
                }
            }
        }
        """.trimIndent()
    }

    private fun parseClassificationResponse(responseBody: String): GeminiClassification {
        val parsed = json.decodeFromString<GeminiResponse>(responseBody)
        val text = parsed.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No content in Gemini response")
        return json.decodeFromString<GeminiClassification>(text)
    }

    companion object {
        private val CLASSIFICATION_SYSTEM_PROMPT = """
You are a classification engine for an impulse-control app. Given an impulse description, return a JSON object with:

1. "tier" — stakes/consequence level:
   - LOW: small purchases, fleeting ideas, minor commitments
   - MEDIUM: meaningful purchases, real time investments, social commitments
   - HIGH: major purchases, life decisions, anything significantly affecting family or career

2. "category" — one of: PURCHASE, IDEA, COMMUNICATION, COMMITMENT, OTHER

3. "ramonaGate" — true if this impulse is communal (affects partner/family), false if personal:
   Communal (flag true):
   - Anything affecting the kids
   - Real estate, major home purchases or changes
   - Vacations, trips, family time
   - Shared funds, large discretionary spend
   - Anything affecting shared calendar/time

   Personal (flag false):
   - Watches, maker/DIY purchases under threshold
   - Work-related decisions
   - Personal hobbies, individual time
   - Minor consumables

4. "ramonaReason" — if ramonaGate is true, a short explanation of why (e.g., "affects shared funds"). Empty string if false.

Return ONLY valid JSON matching the schema. No markdown, no explanation.
        """.trimIndent()
    }
}
