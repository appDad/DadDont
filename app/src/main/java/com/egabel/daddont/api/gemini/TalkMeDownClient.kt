package com.egabel.daddont.api.gemini

import android.content.Context
import com.egabel.daddont.data.model.ReturnEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TalkMeDownClient(private val context: Context) {

    private val gemini = GeminiClient(context)

    suspend fun chat(
        impulseText: String,
        returnLog: List<ReturnEvent>,
        priorTranscript: String?,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val systemContext = buildSystemContext(impulseText, returnLog, priorTranscript)
        val prompt = """
$systemContext

User says: "$userMessage"

Respond as described above. ONLY plain text, no JSON, no markdown fences.
        """.trimIndent()

        gemini.callGemini(prompt) ?: "I couldn't formulate a response. Try again?"
    }

    private fun buildSystemContext(
        impulseText: String,
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
}
