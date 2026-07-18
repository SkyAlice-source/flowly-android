package com.flowly.net.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/*
 * Typography per SPEC §1.3.
 *
 * Font embedding (CHOOSE ONE approach):
 *
 * === A. Google Fonts (runtime download, no binary bloat) ===
 * Add to app/build.gradle.kts:
 *   implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
 *
 * Then replace FontFamily.Default with:
 *   import androidx.compose.ui.text.googlefonts.GoogleFont
 *   import androidx.compose.ui.text.font.Font
 *   val provider = GoogleFont.Provider(
 *     providerAuthority = "com.google.android.gms.fonts",
 *     providerPackage = "com.google.android.gms",
 *     certificates = R.array.com_google_android_gms_fonts_certificates
 *   )
 *   val FlowlyDisplay = FontFamily(
 *     Font(GoogleFont("Space Grotesk"), FontWeight.Bold, provider),
 *     Font(GoogleFont("Space Grotesk"), FontWeight.SemiBold, provider),
 *     Font(GoogleFont("Space Grotesk"), FontWeight.Medium, provider),
 *     Font(GoogleFont("Space Grotesk"), FontWeight.Normal, provider),
 *   )
 *   val FlowlyBody = FontFamily(
 *     Font(GoogleFont("Manrope"), FontWeight.Bold, provider),
 *     Font(GoogleFont("Manrope"), FontWeight.SemiBold, provider),
 *     Font(GoogleFont("Manrope"), FontWeight.Medium, provider),
 *     Font(GoogleFont("Manrope"), FontWeight.Normal, provider),
 *   )
 *   val FlowlyMono = FontFamily(
 *     Font(GoogleFont("Space Mono"), FontWeight.Bold, provider),
 *     Font(GoogleFont("Space Mono"), FontWeight.Normal, provider),
 *   )
 *
 * === B. Bundled TTF (offline, larger APK) ===
 * Download from https://fonts.google.com/:
 *   space_grotesk/static/SpaceGrotesk-Regular.ttf (etc.)
 *   manrope/static/Manrope-Regular.ttf (etc.)
 *   space_mono/static/SpaceMono-Regular.ttf (etc.)
 * Place in app/src/main/res/font/ and reference as:
 *   FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold))
 *
 * Until fonts are embedded, the system default fallback is used.
 */
val FlowlyDisplay: FontFamily = FontFamily.Default // TODO: Space Grotesk
val FlowlyBody: FontFamily    = FontFamily.Default // TODO: Manrope
val FlowlyMono: FontFamily    = FontFamily.Monospace // TODO: Space Mono

/** Shared style for tabular data (rates, latency, stats). */
val FlowlyMetric: TextStyle = TextStyle(
    fontFamily = FlowlyMono,
    fontWeight = FontWeight.Bold,
)
