package com.github.kr328.clash.design

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.formatSpeed
import com.github.kr328.clash.core.util.rawDownloadBytes
import com.github.kr328.clash.core.util.rawUploadBytes
import com.github.kr328.clash.core.util.trafficDownload
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.core.util.trafficUpload
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus,
        ToggleTheme,
        ToggleMode,
        OpenProxy,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    private var clashRunning = false
    private var wasRunning = false
    private var forwardedText = ""
    private var uploadText = ""
    private var downloadText = ""
    private var modeText = ""
    private var startMillis = 0L
    private var lastUpBytes = 0L
    private var lastDownBytes = 0L
    private var lastTrafficTime = 0L
    private val uiStore = UiStore(context)
    private val handler = Handler(Looper.getMainLooper())

    private val colorStarted = context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)
    private val colorStopped = context.resolveThemedColor(R.attr.colorClashStopped)
    private val colorOk = ContextCompat.getColor(context, R.color.color_ok)

    private val durationRunnable = object : Runnable {
        override fun run() {
            updateStatusMeta()
            handler.postDelayed(this, 1000)
        }
    }

    override val root: View
        get() = binding.root

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
            binding.profilesContext = name?.takeIf { it.isNotEmpty() }
                ?: context.getString(R.string.tile_profiles_desc)
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            // Only (re)start the duration timer + runnable on an actual
            // false->true transition, NOT on every refresh (e.g. returning
            // from background). Otherwise the "connected for mm:ss" counter
            // would reset to 0:00 every time the app is switched back to.
            val changed = running != wasRunning
            clashRunning = running
            binding.clashRunning = running
            binding.statusColor = if (running) colorOk else colorStopped
            if (running) {
                if (changed) {
                    startMillis = System.currentTimeMillis()
                    handler.post(durationRunnable)
                }
            } else {
                handler.removeCallbacks(durationRunnable)
                binding.speedChart.clear()
            }
            updateDial()
            updateStatusMeta()
            updateConnButton()
            updateTileContexts()
            wasRunning = running
        }
    }

    suspend fun setTraffic(value: Long) {
        withContext(Dispatchers.Main) {
            forwardedText = value.trafficTotal()
            uploadText = value.trafficUpload()
            downloadText = value.trafficDownload()

            // Real per-second speed from cumulative delta
            val upNow = value.rawUploadBytes()
            val downNow = value.rawDownloadBytes()
            val now = System.currentTimeMillis()
            var upSpeed = 0L
            var downSpeed = 0L
            if (lastTrafficTime > 0 && clashRunning) {
                val dt = (now - lastTrafficTime) / 1000.0
                if (dt > 0) {
                    upSpeed = ((upNow - lastUpBytes) / dt).toLong().coerceAtLeast(0)
                    downSpeed = ((downNow - lastDownBytes) / dt).toLong().coerceAtLeast(0)
                }
            }
            lastUpBytes = upNow
            lastDownBytes = downNow
            lastTrafficTime = now

            binding.forwarded = forwardedText
            binding.upload = uploadText
            binding.download = downloadText
            binding.uploadSpeed = upSpeed.formatSpeed()
            binding.downloadSpeed = downSpeed.formatSpeed()
            binding.forwardedSpeed = (upSpeed + downSpeed).formatSpeed()
            binding.speedChart.appendSample(downSpeed, upSpeed)
            updateDial()
        }
    }

    suspend fun setMode(mode: TunnelState.Mode) {
        withContext(Dispatchers.Main) {
            modeText = when (mode) {
                TunnelState.Mode.Direct -> context.getString(R.string.direct_mode)
                TunnelState.Mode.Global -> context.getString(R.string.global_mode)
                TunnelState.Mode.Rule -> context.getString(R.string.rule_mode)
                else -> context.getString(R.string.rule_mode)
            }
            binding.mode = modeText
            binding.modeColor = when (mode) {
                TunnelState.Mode.Direct -> context.resolveThemedColor(R.attr.colorModeDirect)
                TunnelState.Mode.Global -> context.resolveThemedColor(R.attr.colorModeGlobal)
                TunnelState.Mode.Rule -> context.resolveThemedColor(R.attr.colorModeRule)
                else -> context.resolveThemedColor(R.attr.colorModeRule)
            }
            updateStatusMeta()
        }
    }

    suspend fun setCurrentNode(name: String?, delay: Int?) {
        withContext(Dispatchers.Main) {
            binding.statusNode = name ?: ""
            binding.proxyContext = when {
                name.isNullOrEmpty() -> context.getString(R.string.tile_proxy_desc)
                delay != null && delay > 0 -> "$name · ${delay}ms"
                else -> name
            }
            binding.executePendingBindings()
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val about = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(about.root)
                .show()
        }
    }

    init {
        binding.self = this

        setThemeIcon(UiStore(context).darkMode)

        binding.themeToggle.setOnClickListener {
            request(Request.ToggleTheme)
        }

        binding.modeColor = context.resolveThemedColor(R.attr.colorModeRule)
        binding.statusColor = colorStopped
        binding.dialView.setRunning(false, "")
        updateConnButton()
        resetTileContexts()
    }

    private fun updateDial() {
        binding.dialView.setRunning(clashRunning, "")
    }

    private fun updateStatusMeta() {
        val meta = if (clashRunning) {
            val elapsed = (System.currentTimeMillis() - startMillis) / 1000
            val mm = (elapsed / 60).toString().padStart(2, '0')
            val ss = (elapsed % 60).toString().padStart(2, '0')
            "$modeText · $mm:$ss"
        } else {
            modeText
        }
        binding.statusMeta = meta

        // Update dial with elapsed time when running
        if (clashRunning) {
            val elapsed = (System.currentTimeMillis() - startMillis) / 1000
            val mm = (elapsed / 60).toString().padStart(2, '0')
            val ss = (elapsed % 60).toString().padStart(2, '0')
            binding.dialView.setTimeText("$mm:$ss")
        }

        binding.executePendingBindings()
    }

    private fun updateConnButton() {
        val color = if (clashRunning) colorStopped else colorStarted
        binding.connButton.setBackgroundColor(color)
    }

    fun request(request: Request) {
        requests.trySend(request)
    }

    private fun setThemeIcon(mode: DarkMode) {
        val icon = when (mode) {
            DarkMode.Auto -> R.drawable.ic_baseline_mode_auto
            DarkMode.ForceLight -> R.drawable.ic_baseline_sun
            DarkMode.ForceDark -> R.drawable.ic_baseline_dark_mode
            else -> R.drawable.ic_baseline_mode_auto
        }
        binding.themeToggle.setImageResource(icon)
    }

    private fun resetTileContexts() {
        binding.profilesContext = context.getString(R.string.tile_profiles_desc)
        binding.proxyContext = context.getString(R.string.tile_proxy_desc)
        binding.logsContext = context.getString(R.string.tile_logs_desc)
        binding.settingsContext = context.getString(R.string.tile_settings_desc)
    }

    private fun updateTileContexts() {
        binding.settingsContext = if (clashRunning) {
            context.getString(R.string.running)
        } else {
            context.getString(R.string.stopped)
        }
    }
}
