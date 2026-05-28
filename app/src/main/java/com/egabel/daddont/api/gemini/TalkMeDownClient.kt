package com.egabel.daddont.api.gemini

import android.content.Context
import android.util.Log
import com.egabel.daddont.data.model.ReturnEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Reclassification(
    val tier: String?,
    val category: String?,
    val partnerGate: Boolean?,
    val partnerReason: String?
)

data class DialogResult(
    val response: String,
    val reclassification: Reclassification?
)

class TalkMeDownClient(private val context: Context) {

    companion object {
        private const val TAG = "TalkMeDown"
    }

    private val gemini = GeminiClient(context)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chat(
        impulseText: String,
        currentTier: String?,
        currentCategory: String?,
        currentPartnerGate: Boolean,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?,
        userMessage: String
    ): DialogResult = withContext(Dispatchers.IO) {
        val systemContext = buildSystemContext(
            impulseText, currentTier, currentCategory, currentPartnerGate,
            returnLog, priorTranscript
        )
        val prompt = """
$systemContext

User says: "$userMessage"

Respond as a JSON object with two fields:
1. "response" — your conversational reply (plain text, no markdown, under 150 words)
2. "reclassify" — null if the current classification still looks right, OR an object with any fields that should change:
   - "tier": "LOW" | "MEDIUM" | "HIGH"
   - "category": "PURCHASE" | "IDEA" | "COMMUNICATION" | "COMMITMENT" | "OTHER"
   - "partnerGate": true | false
   - "partnerReason": string (why partner should be involved, empty if partnerGate is false)

Only include fields in "reclassify" that actually need to change. Set "reclassify" to null if nothing changed.

Return ONLY raw JSON. No markdown fences, no explanation outside the JSON.
        """.trimIndent()

        val raw = gemini.callGemini(prompt)
        if (raw == null) {
            return@withContext DialogResult("I couldn't formulate a response. Try again?", null)
        }

        parseDialogResult(raw)
    }

    private fun parseDialogResult(raw: String): DialogResult {
        return try {
            val cleaned = raw.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val obj = json.parseToJsonElement(cleaned).jsonObject

            val response = obj["response"]?.jsonPrimitive?.content
                ?: return DialogResult(raw, null)

            val reclassify = obj["reclassify"]?.let { element ->
                if (element.jsonPrimitive?.content == "null" ||
                    element.toString() == "null") {
                    null
                } else {
                    val r = element.jsonObject
                    Reclassification(
                        tier = r["tier"]?.jsonPrimitive?.content,
                        category = r["category"]?.jsonPrimitive?.content,
                        partnerGate = r["partnerGate"]?.jsonPrimitive?.booleanOrNull,
                        partnerReason = r["partnerReason"]?.jsonPrimitive?.content
                    )
                }
            }

            DialogResult(response, reclassify)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured response, using raw text", e)
            DialogResult(raw, null)
        }
    }

    private fun buildSystemContext(
        impulseText: String,
        currentTier: String?,
        currentCategory: String?,
        currentPartnerGate: Boolean,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?
    ): String {
        val returnHistory = if (returnLog.isEmpty()) {
            "No prior returns."
        } else {
            returnLog.mapIndexed { i, event ->
                val reason = event.rationale?.let { " — \"$it\"" } ?: ""
                "  Return #${i + 1}$reason"
            }.joinToString("\n")
        }

        val priorContext = if (priorTranscript != null) {
            "\n\nPrior conversation about this impulse:\n$priorTranscript"
        } else ""

        return """
You are an incisive friend helping someone examine an impulse they're wrestling with. Not a therapist, not a motivational poster — an honest friend who knows the history.

The impulse: "$impulseText"
Current classification: tier=${currentTier ?: "ungraded"}, category=${currentCategory ?: "ungraded"}, partnerGate=$currentPartnerGate
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
- If the conversation reveals the impulse was misclassified (wrong tier, wrong category, should/shouldn't involve partner), silently update the classification in your reclassify field
- Only reclassify when there's clear evidence from the conversation — not speculatively
        """.trimIndent()
    }
}
