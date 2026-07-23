package com.github.kr328.clash.design.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kr328.clash.design.ui.Insets

fun View.setOnInsertsChangedListener(adaptLandscape: Boolean = true, listener: (Insets) -> Unit) {
    setOnApplyWindowInsetsListener { v, ins ->
        val compat = WindowInsetsCompat.toWindowInsetsCompat(ins)
        // systemBars() 在部分设备的手势导航下可能不返回导航栏高度（bottom=0），
        // 故同时取 navigationBars() 并取各边最大值，确保底部安全区域不被裁切。
        val systemBars = compat.getInsets(WindowInsetsCompat.Type.systemBars())
        val navBars = compat.getInsets(WindowInsetsCompat.Type.navigationBars())

        val insets = androidx.core.graphics.Insets.of(
            maxOf(systemBars.left, navBars.left),
            maxOf(systemBars.top, navBars.top),
            maxOf(systemBars.right, navBars.right),
            maxOf(systemBars.bottom, navBars.bottom),
        )

        val rInsets = if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            Insets(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom,
            )
        } else {
            Insets(
                insets.right,
                insets.top,
                insets.left,
                insets.bottom,
            )
        }

        listener(if (adaptLandscape) rInsets.landscape(v.context) else rInsets)

        compat.toWindowInsets()!!
    }

    requestApplyInsets()
}
