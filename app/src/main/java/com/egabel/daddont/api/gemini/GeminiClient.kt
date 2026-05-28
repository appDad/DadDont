package com.egabel.daddont.api.gemini

import android.content.Context
import android.util.Log
import com.egabel.daddont.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient(private val context: Context) {

    companion object {
        private const val TAG = "GeminiApi"
    }

    private fun storedApiKey(): String =
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
            .getString(Prefs.KEY_GEMINI_API_KEY, null)
            ?.trim()
            .orEmpty()

    @Serializable private data class Part(val text: String)
    @Serializable private data class Content(val parts: List<Part>)
    @Serializable private data class GeminiRequest(val contents: List<Content>)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val apiUrls = listOf(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
    )

    data class ClassificationResult(
        val tier: String,
        val category: String,
        val partnerGate: Boolean,
        val partnerReason: String
    )

    suspend fun classify(impulseText: String): ClassificationResult? = withContext(Dispatchers.IO) {
        val prompt = """
You are a classification engine for an impulse-control app. Given an impulse description, return a JSON object with:

1. "tier" — stakes/consequence level:
   - LOW: small purchases, fleeting ideas, minor commitments
   - MEDIUM: meaningful purchases, real time investments, social commitments
   - HIGH: major purchases, life decisions, anything significantly affecting family or career

2. "category" — one of: PURCHASE, IDEA, COMMUNICATION, COMMITMENT, OTHER

3. "partnerGate" — true if this impulse is communal (affects partner/family), false if personal:
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

4. "partnerReason" — if partnerGate is true, a short explanation of why. Empty string if false.

Return ONLY raw JSON. No markdown fences, no explanation.

Impulse: "$impulseText"
        """.trimIndent()

        val responseText = callGemini(prompt) ?: return@withContext null

        runCatching {
            val obj = json.parseToJsonElement(responseText.stripFences()).jsonObject
            val tier = obj["tier"]?.jsonPrimitive?.content ?: return@runCatching null
            val category = obj["category"]?.jsonPrimitive?.content ?: return@runCatching null
            val partnerGate = obj["partnerGate"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            val partnerReason = obj["partnerReason"]?.jsonPrimitive?.content ?: ""

            ClassificationResult(tier, category, partnerGate, partnerReason)
        }.onFailure {
            Log.e(TAG, "JSON parse failed: $responseText", it)
        }.getOrNull()
    }

    fun callGemini(prompt: String): String? {
        val apiKey = storedApiKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "No Gemini API key configured")
            return null
        }

        val bodyStr = json.encodeToString(
            GeminiRequest(listOf(Content(listOf(Part(prompt)))))
        )

        for (url in apiUrls) {
            val body = bodyStr.toRequestBody(jsonMedia)
            val request = Request.Builder()
                .url("$url?key=$apiKey")
                .post(body)
                .build()

            val result = runCatching {
                http.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@use null
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Gemini $url failed: ${response.code}")
                        return@use null
                    }
                    json.parseToJsonElement(responseBody).jsonObject["candidates"]
                        ?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content
                }
            }.getOrNull()

            if (result != null) return result
            Log.d(TAG, "Falling back to next model...")
        }
        Log.e(TAG, "All Gemini models failed")
        return null
    }

    private fun String.stripFences() =
        trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
