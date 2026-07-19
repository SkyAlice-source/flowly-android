package com.github.kr328.clash

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.store.BatteryOptStore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.R as DesignR

class MainActivity : BaseActivity<MainDesign>() {
    private var currentMode: TunnelState.Mode = TunnelState.Mode.Rule

    // 主题切换后 recreate() 会销毁旧界面的 Snackbar，故先把提示文案存起来，
    // 等新界面就绪后再弹，使其与「规则切换」的提示表现一致。
    companion object {
        private var pendingThemeToast: Int? = null
    }

    private val vpnRequestLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { }

    override suspend fun main() {
        val design = MainDesign(this)

        setContentDesign(design)

        design.fetch()

        // 在 recreate() 后的新界面上补弹主题切换提示（与「规则切换」表现一致）
        pendingThemeToast?.let { res ->
            design.showToast(res, ToastDuration.Short)
            pendingThemeToast = null
        }

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart -> {
                            design.fetch()
                            promptBatteryOptimizationIfNeeded()
                        }
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop,
                        Event.ProfileLoaded, Event.ProfileChanged -> design.fetch()
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        MainDesign.Request.ToggleStatus -> {
                            if (clashRunning) {
                                stopClashService()
                            } else {
                                val vpnRequest = startClashService()
                                if (vpnRequest != null) {
                                    vpnRequestLauncher.launch(vpnRequest)
                                }
                            }
                        }
                        MainDesign.Request.ToggleMode -> {
                            val next = when (currentMode) {
                                TunnelState.Mode.Rule -> TunnelState.Mode.Global
                                TunnelState.Mode.Global -> TunnelState.Mode.Direct
                                TunnelState.Mode.Direct -> TunnelState.Mode.Rule
                                else -> TunnelState.Mode.Rule
                            }

                            withClash {
                                val override = queryOverride(Clash.OverrideSlot.Session)
                                override.mode = next
                                patchOverride(Clash.OverrideSlot.Session, override)
                            }

                            currentMode = next
                            design.setMode(currentMode)

                            val text = when (currentMode) {
                                TunnelState.Mode.Rule -> DesignR.string.rule_mode
                                TunnelState.Mode.Global -> DesignR.string.global_mode
                                TunnelState.Mode.Direct -> DesignR.string.direct_mode
                                else -> DesignR.string.rule_mode
                            }

                            design.showToast(text, ToastDuration.Short)
                        }
                        MainDesign.Request.ToggleTheme -> {
                            val next = when (uiStore.darkMode) {
                                DarkMode.Auto -> DarkMode.ForceLight
                                DarkMode.ForceLight -> DarkMode.ForceDark
                                DarkMode.ForceDark -> DarkMode.Auto
                                else -> DarkMode.Auto
                            }

                            uiStore.darkMode = next

                            // Apply dark mode so recreate() picks up the new theme
                            val nightMode = when (next) {
                                DarkMode.ForceDark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                DarkMode.ForceLight -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            }
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)

                            val text = when (next) {
                                DarkMode.Auto -> DesignR.string.theme_auto
                                DarkMode.ForceLight -> DesignR.string.theme_light
                                DarkMode.ForceDark -> DesignR.string.theme_dark
                                else -> DesignR.string.theme_auto
                            }

                            pendingThemeToast = text
                            recreate()
                        }
                        MainDesign.Request.OpenProxy -> {
                            startActivity(ProxyActivity::class.intent)
                            overridePendingTransition(DesignR.anim.slide_in_right, DesignR.anim.slide_out_left)
                        }
                        MainDesign.Request.OpenProfiles -> {
                            startActivity(ProfilesActivity::class.intent)
                            overridePendingTransition(DesignR.anim.slide_in_right, DesignR.anim.slide_out_left)
                        }
                        MainDesign.Request.OpenProviders -> {
                            startActivity(ProvidersActivity::class.intent)
                            overridePendingTransition(DesignR.anim.slide_in_right, DesignR.anim.slide_out_left)
                        }
                        MainDesign.Request.OpenLogs -> {
                            if (LogcatService.running) {
                                startActivity(LogcatActivity::class.intent)
                            } else {
                                startActivity(LogsActivity::class.intent)
                            }
                            overridePendingTransition(DesignR.anim.slide_in_right, DesignR.anim.slide_out_left)
                        }
                        MainDesign.Request.OpenSettings -> {
                            startActivity(SettingsActivity::class.intent)
                            overridePendingTransition(DesignR.anim.slide_in_right, DesignR.anim.slide_out_left)
                        }
                        MainDesign.Request.OpenHelp -> {
                            startActivity(HelpActivity::class.intent)
                            overridePendingTransition(DesignR.anim.slide_in_right, DesignR.anim.slide_out_left)
                        }
                        MainDesign.Request.OpenAbout ->
                            design.showAbout(queryAppVersionName())
                    }
                }
                if (clashRunning) {
                    ticker.onReceive {
                        design.fetchTraffic()
                    }
                }
            }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(clashRunning)

        val state = withClash {
            queryTunnelState()
        }
        val providers = withClash {
            queryProviders()
        }

        currentMode = state.mode
        setMode(state.mode)

        withProfile {
            setProfileName(queryActive()?.name)
        }

        // Current proxy node + its measured delay, shown on the Proxy tile
        val groupNames = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
        if (groupNames.isNotEmpty()) {
            val group = withClash { queryProxyGroup(groupNames.first(), uiStore.proxySort) }
            val delay = group.proxies.firstOrNull { it.name == group.now }?.delay
            setCurrentNode(group.now, delay)
        }
    }

    private suspend fun MainDesign.fetchTraffic() {
        withClash {
            setTraffic(queryTrafficNow())
        }
    }

    private fun queryAppVersionName(): String {
        val str = packageManager.getPackageInfo(packageName, 0).versionName
        return str ?: "???"
    }

    /**
     * Nudge the user (once) to exempt Flowly from battery optimization so the
     * foreground VPN service survives being sent to the background. Triggered
     * on ClashStart, after VPN permission is already granted.
     */
    private fun promptBatteryOptimizationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        val store = BatteryOptStore(this)
        if (store.promptShown) return
        store.promptShown = true

        AlertDialog.Builder(this).apply {
            setTitle(DesignR.string.battery_opt_title)
            setMessage(DesignR.string.battery_opt_message)
            setPositiveButton(DesignR.string.battery_opt_go) { _, _ -> openBatterySettings() }
            setNegativeButton(DesignR.string.battery_opt_later) { _, _ -> }
            setCancelable(true)
            show()
        }
    }

    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            // Deep link straight to Flowly's battery-optimization entry.
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            // Some vendors (Xiaomi/Huawei/...) don't resolve that intent — fall
            // back to the generic battery-optimization list.
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                // Last resort: nothing we can do, silently skip.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher =
                registerForActivityResult(RequestPermission()
                ) { isGranted: Boolean ->
                }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setupShortcuts()
    }

    private fun setupShortcuts() {
        // Skip dynamic shortcut setup when the app icon is hidden.
        if (uiStore.hideAppIcon) return

        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
            Intent.FLAG_ACTIVITY_NO_ANIMATION

        val toggle = ShortcutInfoCompat.Builder(this, "toggle_clash")
            .setShortLabel(getString(DesignR.string.shortcut_toggle_short))
            .setLongLabel(getString(DesignR.string.shortcut_toggle_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_all))
            .setIntent(
                Intent(Intents.ACTION_TOGGLE_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(0)
            .build()

        val start = ShortcutInfoCompat.Builder(this, "start_clash")
            .setShortLabel(getString(DesignR.string.shortcut_start_short))
            .setLongLabel(getString(DesignR.string.shortcut_start_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_on))
            .setIntent(
                Intent(Intents.ACTION_START_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(1)
            .build()

        val stop = ShortcutInfoCompat.Builder(this, "stop_clash")
            .setShortLabel(getString(DesignR.string.shortcut_stop_short))
            .setLongLabel(getString(DesignR.string.shortcut_stop_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_off))
            .setIntent(
                Intent(Intents.ACTION_STOP_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(2)
            .build()

        ShortcutManagerCompat.setDynamicShortcuts(this, listOf(toggle, start, stop))
    }
}
