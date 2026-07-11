package com.egabel.daddont.api.gemini

import android.content.Context
import android.util.Log
import com.egabel.daddont.data.model.DesireCheckIn
import com.egabel.daddont.data.model.Impulse
import com.egabel.daddont.data.model.ReturnEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        impulse: Impulse,
        desireCurve: List<DesireCheckIn>,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?,
        userMessage: String
    ): DialogResult = withContext(Dispatchers.IO) {
        val systemContext = buildSystemContext(impulse, desireCurve, returnLog, priorTranscript)
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
                if (element.toString() == "null") {
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
        impulse: Impulse,
        desireCurve: List<DesireCheckIn>,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?
    ): String {
        val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())

        val curveText = if (desireCurve.isEmpty()) {
            "No desire readings recorded."
        } else {
            desireCurve.joinToString(" → ") {
                "${it.strength}/10 (${dateFmt.format(Date(it.timestamp))})"
            }
        }

        val returnHistory = if (returnLog.isEmpty()) {
            "No prior returns."
        } else {
            returnLog.mapIndexed { i, event ->
                val reason = event.rationale?.let { " — \"$it\"" } ?: ""
                "  Return #${i + 1} (${dateFmt.format(Date(event.timestamp))})$reason"
            }.joinToString("\n")
        }

        val priorContext = if (priorTranscript != null) {
            "\n\nPrior conversation about this impulse:\n$priorTranscript"
        } else ""

        val prediction = impulse.prediction?.let {
            "\nAt capture, they predicted they would ${
                if (it.name == "STILL_WANT") "STILL WANT this once it cooled"
                else "have MOVED ON by the time it cooled"
            }."
        } ?: ""

        return """
You are an incisive friend helping someone examine an impulse they're wrestling with. Not a therapist, not a motivational poster — an honest friend who knows the full history.

The impulse: "${impulse.content}"
Current classification: tier=${impulse.tier?.name ?: "ungraded"}, category=${impulse.category?.name ?: "ungraded"}, partnerGate=${impulse.partnerGate}
Their stated reason at capture: ${impulse.rationale ?: "none recorded"}
Likely trigger: ${impulse.trigger ?: "unknown"}
Estimated cost: ${impulse.estimatedCost?.let { "$$it" } ?: "n/a"}$prediction

Desire over time: $curveText
Times it has returned: ${returnLog.size} (deferred ${impulse.deferCount}x)
Return history:
$returnHistory$priorContext

Your approach:
- Use the desire curve as evidence. If it's decaying, show them their own numbers. If it's holding steady or climbing, acknowledge that honestly — maybe they really want this.
- Compare against their capture-time prediction if one exists
- Reference their own stated rationale and interrogate it gently
- Reference their history concretely ("You've been here three times...")
- Ask honest questions, don't lecture
- If they have good reasons this time, say so
- Keep responses conversational and under 150 words
- Don't be preachy or use clichés
- If the conversation reveals the impulse was misclassified (wrong tier, wrong category, should/shouldn't involve partner), silently update the classification in your reclassify field
- Only reclassify when there's clear evidence from the conversation — not speculatively
        """.trimIndent()
    }
}
