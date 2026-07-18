package com.flowly.net.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

/*
 * Lucide-style icon set (SPEC §4): 24x24 viewBox, 2px stroke,
 * round caps/joins, no fill. Embedded as ImageVector so the project
 * carries ZERO raster assets. Tint is applied by the caller (Icon/Image).
 *
 * Additional brand icons (filled) are included for the launcher-style
 * logo and design-system glyph used in the app bar.
 */

private fun lucide(d: String): ImageVector = ImageVector.Builder(
    name = "lucide",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = PathParser().parsePathString(d).toNodes(),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).build()

private fun fillPath(d: String, fillAlpha: Float = 1f): ImageVector = ImageVector.Builder(
    name = "flowly-brand",
    defaultWidth = 108.dp,
    defaultHeight = 108.dp,
    viewportWidth = 108f,
    viewportHeight = 108f,
).addPath(
    pathData = PathParser().parsePathString(d).toNodes(),
    fill = SolidColor(Color.White),
    fillAlpha = fillAlpha,
).build()

object LucideIcons {
    val Box = lucide(
        "M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" +
        "|M3.3 7 8.7 5 8.7-5|M12 22V12"
    )
    val LayoutDashboard = lucide(
        "M3 3h7v7H3z|M14 3h7v4h-7z|M14 12h7v9h-7z|M3 14h7v5H3z"
    )
    val Globe = lucide(
        "M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10Z" +
        "|M2 12h20"
    )
    val FileText = lucide(
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z" +
        "|M14 2v4a2 2 0 0 0 2 2h4|M16 13H8|M16 17H8|M10 9H8"
    )
    val Settings = lucide(
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l.43.25a2 2 0 0 1 2 0l.15-.08a2 2 0 0 0 2.73.73l.22.38a2 2 0 0 0 .73 2.73l-.15.1a2 2 0 0 1-1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l-.43-.25a2 2 0 0 1-2 0l-.15.08a2 2 0 0 0-2.73-.73l-.22-.38a2 2 0 0 0-.73-2.73l.15-.08a2 2 0 0 1 1-1.74v-.5a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73.73l.22.39a2 2 0 0 0 .73 2.73l-.15.08a2 2 0 0 1-1 1.74V20a2 2 0 0 0 2 2Z" +
        "|M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0"
    )
    val Zap = lucide("M13 2 3 14h9l-1 8 10-12h-9l1-8-2 0Z")
    val Shuffle = lucide(
        "M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.6-8.6C14.1 6.6 15.3 6 16.6 6H22" +
        "|m18 6 3-3-3-3|M2 6h1.9c1.3 0 2.5.6 3.3 1.7l6.5 8.6c.8 1.1 2 1.7 3.3 1.7H22" +
        "|m18 18 3 3-3 3"
    )
    val RefreshCw = lucide(
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8|M21 3v5h-5" +
        "|M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16|M8 16H3v5"
    )
    val Link = lucide(
        "M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1" +
        "|M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1"
    )
    val QrCode = lucide(
        "M3 8V6a2 2 0 0 1 2-2h2|M17 3h2a2 2 0 0 1 2 2v2|M21 16v2a2 2 0 0 1-2 2h-2|M3 16v2a2 2 0 0 0 2 2h2" +
        "|M7 7h.01|M17 7h.01|M7 17h.01|M17 17h.01|M7 12h10|M12 7v10"
    )
    val Folder = lucide(
        "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"
    )
    val Network = lucide(
        "M16 16h6v6h-6z|M2 16h6v6H2z|M9 2h6v6H9z" +
        "|M5 16v-3a1 1 0 0 1 1-1H17a1 1 0 0 1 1 1v3|M12 12V8"
    )
    val Server = lucide(
        "M2 2h20v8H2z|M2 14h20v8H2z|M6 6h.01|M6 18h.01"
    )
    val Globe2 = lucide(
        "M12 2a10 10 0 0 0 0 20 10 10 0 0 1 0-20Z|M2 12h20"
    )
    val Type = lucide(
        "M4 7V4h16v3|M9 20h6|M12 4v16"
    )
    val Contrast = lucide(
        "M12 2a10 10 0 0 0 0 20 10 10 0 0 1 0-20Z" + "|M12 2a10 10 0 0 0 0 20v-20Z"
    )
    val Info = lucide("M12 2a10 10 0 0 0 0 20 10 10 0 0 1 0-20Z|M12 16v-4|M12 8h.01")
    val Search = lucide("M11 11m-8 0a8 8 0 1 0 16 0a8 8 0 1 0-16 0|m21 21-4.3-4.3")
    val Check = lucide("M20 6 9 17l-5-5")
    val Sun = lucide(
        "M12 4V2|M12 20v2|M4.93 4.93l1.41 1.41|M17.66 17.66l1.41 1.41|M2 12h2|M20 12h2" +
        "|M6.34 17.66l-1.41 1.41|M19.07 4.93l-1.41 1.41"
    )
    val Moon = lucide("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Monitor = lucide("M12 17v4|M8.5 21h7|M2 4h20v12H2z")
    val X = lucide("M18 6 6 18|M6 6l12 12")
    val ChevronRight = lucide("m9 6 6 6-6 6")
    val ArrowLeft = lucide("m12 19-7-7 7-7|M19 12H5")

    /** Design-system glyph (diamond/kite) used in the app bar. */
    val DesignSystem = lucide(
        "M5.5 8.5 9 12l-3.5 3.5L2 12z" +
        "|m12 2 3.5 3.5L12 9 8.5 5.5z" +
        "|M18.5 8.5 22 12l-3.5 3.5L15 12z" +
        "|m12 15 3.5 3.5L12 22l-3.5-3.5z"
    )

    /**
     * Brand wave logo (matches the launcher icon foreground).
     * Rendered at 34dp inside the gradient app-bar logo box.
     */
    val WaveLogo = ImageVector.Builder(
        name = "flowly-wave-logo",
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M0,68 C18,52 36,52 54,68 C72,84 90,84 108,68 L108,108 L0,108 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.5f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M0,80 C18,64 36,64 54,80 C72,96 90,96 108,80 L108,108 L0,108 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.92f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M32,37 a7,7 0 1,0 0,14 a7,7 0 1,0 0,-14 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 1f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M24,56.6 a2.4,2.4 0 1,0 0,4.8 a2.4,2.4 0 1,0 0,-4.8 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.95f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M33,53.3 a1.7,1.7 0 1,0 0,3.4 a1.7,1.7 0 1,0 0,-3.4 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.8f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M41,58 a2,2 0 1,0 0,4 a2,2 0 1,0 0,-4 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.9f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M50,62.6 a1.4,1.4 0 1,0 0,2.8 a1.4,1.4 0 1,0 0,-2.8 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.65f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M60,68.4 a1.6,1.6 0 1,0 0,3.2 a1.6,1.6 0 1,0 0,-3.2 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.7f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M70,64.7 a1.3,1.3 0 1,0 0,2.6 a1.3,1.3 0 1,0 0,-2.6 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.6f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M80,76.1 a1.9,1.9 0 1,0 0,3.8 a1.9,1.9 0 1,0 0,-3.8 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.8f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M90,70.7 a1.3,1.3 0 1,0 0,2.6 a1.3,1.3 0 1,0 0,-2.6 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.6f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M46,48.7 a1.3,1.3 0 1,0 0,2.6 a1.3,1.3 0 1,0 0,-2.6 Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.7f,
    ).build()
}
