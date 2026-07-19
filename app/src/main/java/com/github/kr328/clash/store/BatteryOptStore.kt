package com.github.kr328.clash.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider

/**
 * Persists whether we've already nudged the user to exempt Flowly from
 * battery optimization. The prompt only shows once so we don't harass them.
 */
class BatteryOptStore(context: Context) {
    private val store = Store(
        context
            .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .asStoreProvider()
    )

    var promptShown: Boolean by store.boolean(
        key = "prompt_shown",
        defaultValue = false,
    )

    companion object {
        private const val FILE_NAME = "battery_opt"
    }
}
