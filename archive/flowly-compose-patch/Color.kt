package com.flowly.net.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/* Single source of truth = CMFA-SPEC hex tokens.
   ok/warn/bad are shared across themes (per SPEC §1.2). */

val Accent       = Color(0xFF496CEF) // brand primary
val AccentPress  = Color(0xFF3655CD) // pressed primary
val AccentSoft   = Color(0x1F496CEF) // 12% accent (light)
val OkColor      = Color(0xFF3FC168) // connected / low latency
val WarnColor    = Color(0xFFEFA831) // mid latency / caution
val BadColor     = Color(0xFFEA3C3F) // high latency / error
val Ink3Light    = Color(0xFF83868C) // design ink-3 (light) — SPEC §1.1
val BgGradLight  = Color(0xFFF8F7F2) // design bg-grad (light) — SPEC §1.1
val Ink3Dark     = Color(0xFF7D8086) // design ink-3 (dark) — SPEC §1.2
val BgGradDark   = Color(0xFF07080B) // design bg-grad (dark) — SPEC §1.2

@Immutable
data class FlowlyExtraColors(
    val accent: Color,
    val accentPress: Color,
    val accentSoft: Color,
    val ok: Color,
    val warn: Color,
    val bad: Color,
    val ink3: Color,
    val bgGradient: Color,
)

val LocalFlowlyExtra = staticCompositionLocalOf {
    FlowlyExtraColors(
        accent = Accent,
        accentPress = AccentPress,
        accentSoft = AccentSoft,
        ok = OkColor,
        warn = WarnColor,
        bad = BadColor,
        ink3 = Ink3Light,
        bgGradient = BgGradLight,
    )
}
