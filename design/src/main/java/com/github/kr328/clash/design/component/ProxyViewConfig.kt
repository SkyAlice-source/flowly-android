package com.github.kr328.clash.design.component

import android.content.Context
import android.graphics.Color
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.util.getPixels
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.resolveThemedResourceId

class ProxyViewConfig(val context: Context, var proxyLine: Int) {
    private val colorSurface = context.resolveThemedColor(com.google.android.material.R.attr.colorSurface)

    val clickableBackground =
        context.resolveThemedResourceId(android.R.attr.selectableItemBackground)

    val selectedControl = context.resolveThemedColor(com.google.android.material.R.attr.colorOnPrimary)
    val selectedBackground = context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)

    val unselectedControl = context.resolveThemedColor(com.google.android.material.R.attr.colorOnSurface)
    val unselectedBackground: Int
        get() = if (proxyLine==1) Color.TRANSPARENT else colorSurface

    val layoutPadding = context.getPixels(R.dimen.proxy_layout_padding).toFloat()
    val contentPadding
        get() = if (proxyLine==2) context.getPixels(R.dimen.proxy_content_padding).toFloat() else context.getPixels(R.dimen.proxy_content_padding_grid3).toFloat()
    val textMargin
        get() = if (proxyLine==2) context.getPixels(R.dimen.proxy_text_margin).toFloat() else context.getPixels(R.dimen.proxy_text_margin_grid3).toFloat()
    val textSize
        get() = if (proxyLine==2) context.getPixels(R.dimen.proxy_text_size).toFloat() else context.getPixels(R.dimen.proxy_text_size_grid3).toFloat()

    val shadow = Color.argb(
        0x15,
        Color.red(Color.DKGRAY),
        Color.green(Color.DKGRAY),
        Color.blue(Color.DKGRAY),
    )

    val cardRadius = context.getPixels(R.dimen.proxy_card_radius).toFloat()
    var cardOffset = context.getPixels(R.dimen.proxy_card_offset).toFloat()

    // delay status bar (semantic colors, theme-independent)
    val delayUnknown = Color.rgb(0x9E, 0x9E, 0x9E) // grey   - not tested / timeout
    val delayGood = Color.rgb(0x43, 0xA0, 0x47)    // green  - < 100ms
    val delayMedium = Color.rgb(0xF9, 0xA8, 0x25)  // amber  - 100~300ms
    val delayHigh = Color.rgb(0xFB, 0x8C, 0x00)    // orange - 300~800ms
    val delayTimeout = Color.rgb(0xE5, 0x39, 0x35) // red    - > 800ms

    val delayBarWidth = context.getPixels(R.dimen.proxy_delay_bar_width).toFloat()
    val delayBarInset = context.getPixels(R.dimen.proxy_delay_bar_inset).toFloat()
    val delayBarRadius = context.getPixels(R.dimen.proxy_delay_bar_radius).toFloat()
}