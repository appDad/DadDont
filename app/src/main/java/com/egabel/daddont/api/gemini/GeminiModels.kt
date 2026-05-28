package com.egabel.daddont.api.gemini

import kotlinx.serialization.Serializable

@Serializable
data class GeminiClassification(
    val tier: String,
    val category: String,
    val ramonaGate: Boolean,
    val ramonaReason: String = ""
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart> = emptyList()
)

@Serializable
data class GeminiPart(
    val text: String? = null
)
