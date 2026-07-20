package com.github.kr328.clash.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider

/**
 * Persists whether we've already nudged the user to keep Flowly alive in the
 * background. Two one-shot prompts are tracked:
 *  - [promptShown]: preventive nudge on first successful connect;
 *  - [vendorPromptShown]: nudge after we detected the service was killed in
 *    the background, pointing at the vendor's background/auto-start page.
 * Each shows once so we don't harass the user.
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

    var vendorPromptShown: Boolean by store.boolean(
        key = "vendor_prompt_shown",
        defaultValue = false,
    )

    companion object {
        private const val FILE_NAME = "battery_opt"
    }
}
