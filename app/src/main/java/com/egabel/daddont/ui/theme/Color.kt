package com.egabel.daddont.ui.theme

import androidx.compose.ui.graphics.Color

// ── State colors (border / accent) ───────────────────────────────────────────
val PendingState  = Color(0xFF5B8DEF)   // Soft blue — waiting for LLM
val RedState      = Color(0xFFE05C5C)   // Hot
val YellowState   = Color(0xFFE59933)   // Cooling (amber, not harsh yellow)
val GreenState    = Color(0xFF2FA36C)   // Cooled
val GrayState     = Color(0xFF9EA8BE)   // Archived

// ── State container tints (very subtle backgrounds) ──────────────────────────
val PendingStateContainer  = Color(0xFFE8F0FE)
val RedStateContainer      = Color(0xFFFDECEC)
val YellowStateContainer   = Color(0xFFFFF4E5)
val GreenStateContainer    = Color(0xFFE6F5ED)
val GrayStateContainer     = Color(0xFFEFF1F5)

// ── Partner badge ────────────────────────────────────────────────────────────
val PartnerBadge = Color(0xFF6F69AF)   // Purple from gradient — family tie-in
