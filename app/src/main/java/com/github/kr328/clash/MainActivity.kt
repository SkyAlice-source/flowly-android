package com.github.kr328.clash

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
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
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.service.StatusProvider
import com.github.kr328.clash.store.BatteryOptStore
import com.github.kr328.clash.util.VendorBackground
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

        // 后台被杀自动恢复：lock 文件仍标记「应处于连接态」（用户未主动断开），
        // 但服务已不在 —— 说明 :background 进程被系统/厂商后台管理杀掉。
        // 此时静默重连（VPN 已授权，VpnService.prepare() 返回 null，不会弹确认框），
        // 并借机引导用户去厂商的后台运行设置页，从根上减少被杀。
        // 隧道仍通（tun_active.lock 文件标记 true，跨进程可读）：立即同步 UI，
        // 避免进应用先闪「未连接」再转圈。recover 仍只在 tunActive=false 时真正重建。
        if (StatusProvider.tunActive) {
            design.setClashRunning(true)
        }

        recoverIfKilledInBackground(design)

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))
        var lastHealthCheck: Long = 0

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart -> {
                            design.fetch()
                            promptBatteryOptimizationIfNeeded()
                        }
                        Event.ServiceRecreated,
                        Event.ProfileLoaded, Event.ProfileChanged -> design.fetch()
                        // ClashStop: the :background process was just killed (e.g. system
                        // foreground-service timeout while backgrounded). Do NOT query it via
                        // withClash here — the binder is dead and would throw DeadObjectException
                        // uncaught in this coroutine, crashing the app in the background.
                        // Just reflect the stopped state on the UI.
                        Event.ClashStop -> design.setClashRunning(false)
                        else -> Unit
                    }
                }
                if (clashRunning) {
                    ticker.onReceive {
                        design.fetchTraffic()

                        // Health check every 30 seconds: probe the :background process
                        // to detect Go runtime corruption before ColorOS kills us permanently.
                        if (System.currentTimeMillis() - lastHealthCheck > 30_000L) {
                            lastHealthCheck = System.currentTimeMillis()
                            var healthy = true
                            try {
                                kotlinx.coroutines.withTimeout(TimeUnit.SECONDS.toMillis(5)) {
                                    withClash { queryTunnelState() }
                                }
                            } catch (e: Exception) {
                                healthy = false
                            }
                            if (!healthy) {
                                // ColorOS 会瞬时冻结 :background 进程，使 queryTunnelState
                                // 超时（healthy=false），但隧道其实还通（tunActive 文件仍为 true）。
                                // 此时若直接 ClashStop 会导致进应用/运行中莫名重建圆环。
                                // 只有隧道标记也消失时才判定真死，触发恢复。
                                if (!StatusProvider.tunActive) {
                                    Log.w("Health check failed AND tunnel inactive, triggering recovery")
                                    events.trySend(Event.ClashStop)
                                } else {
                                    Log.d("Health check timed out but tunnel marker active; skip recovery")
                                }
                            }
                        }
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
            }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(clashRunning)

        val state = withClash {
            queryTunnelState()
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
        try {
            withClash {
                setTraffic(queryTrafficNow())
            }
        } catch (e: Exception) {
            // :background process was reclaimed by the system (foreground-service
            // timeout) between ticks — skip this sample instead of crashing.
        }
    }

    private fun queryAppVersionName(): String {
        val str = packageManager.getPackageInfo(packageName, 0).versionName
        return str ?: "???"
    }

    /**
     * 检测并恢复「后台被系统杀掉」的连接。
     *
     * 判定依据：[StatusProvider.shouldStartClashOnBoot]（service_running.lock）
     * 在用户主动断开时会被删除，因此它为 true 而 [clashRunning] 为 false，
     * 只可能是服务进程被系统强杀。恢复动作对用户静默（VPN 授权仍然有效）。
     */
    /**
     * 后台连接被系统中断后，App 打开时只如实反映隧道状态——不强制重连、不弹提示。
     *
     * 之前的实现会在 onStart 里自动 startClashService 并弹「连接被系统中断，已自动恢复」，
     * 造成「一打开应用就连接、还显示被恢复」的回归（用户本意是手动控制 VPN）。
     * 现在：连着显示已连接，断了显示未连接，由用户自己点「启动」。
     * 保活引导放在设置页「后台运行设置」中，不再每次打开都打扰。
     */
    private suspend fun recoverIfKilledInBackground(design: MainDesign) {
        // 故意为空：不在打开 App 时自动重连，也不弹 toast。
    }

    /**
     * Nudge the user (once) to keep Flowly alive in the background after the
     * first successful connect. On OEM ROMs the standard battery page is often
     * not the real kill switch, so the button opens the vendor-specific
     * background/auto-start page (with battery/app-details fallbacks).
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
            setPositiveButton(DesignR.string.battery_opt_go) { _, _ -> openVendorBackgroundSettings() }
            setNegativeButton(DesignR.string.battery_opt_later) { _, _ -> }
            setCancelable(true)
            show()
        }
    }

    private fun openVendorBackgroundSettings() {
        VendorBackground.openBackgroundSettings(this)
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
