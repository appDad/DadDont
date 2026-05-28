package com.egabel.daddont.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Dad-family shared palette ────────────────────────────────────────────────
val BlueLeft    = Color(0xFF1A60A5)
val PurpleRight = Color(0xFF6F69AF)
val TitleGradient = Brush.horizontalGradient(listOf(BlueLeft, PurpleRight))

val BgLight      = Color(0xFFF4F6FA)
val SurfaceLight = Color(0xFFEAEDF5)
val BorderColor  = Color(0xFFD4DAE8)
val TextMain     = Color(0xFF1E2235)
val TextDim      = Color(0xFF7A82A0)
val TextMid      = Color(0xFF4A5270)

private val LightColorScheme = lightColorScheme(
    primary        = BlueLeft,
    secondary      = PurpleRight,
    background     = BgLight,
    surface        = Color.White,
    surfaceVariant = SurfaceLight,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = TextMain,
    onSurface      = TextMain,
    outline        = BorderColor,
)

@Composable
fun DadDontTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography(),
        content     = content
    )
}
