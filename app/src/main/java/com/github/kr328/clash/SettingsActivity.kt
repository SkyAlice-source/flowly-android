package com.github.kr328.clash

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.SettingsDesign
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignKernelBinding
import com.github.kr328.clash.kernel.KernelManager
import com.github.kr328.clash.util.LocaleHelper
import com.github.kr328.clash.util.VendorBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : BaseActivity<SettingsDesign>() {

    companion object {
        /** Flowly 应用本体仓库（关于弹窗的 GitHub 按钮指向这里） */
        private const val APP_REPO = "SkyAlice-source/flowly-android"

        /** GitHub releases API for Flowly's rebuilt kernels (libbridge.so) */
        private const val GITHUB_API_RELEASES =
            "https://api.github.com/repos/${KernelManager.REPO}/releases?per_page=30"

        /**
         * 内置（出厂）内核的 mihomo 语义版本。
         * 来源：core/src/foss/golang/clash/constant/version.go 的 Version 字段。
         * nativeCoreVersion() 只返回构建日期（如 260713），无法体现语义版本，故在此显式声明。
         */
        private const val BUNDLED_KERNEL_VERSION = "1.19.27"
    }

    override suspend fun main() {
        val design = SettingsDesign(this)

        setContentDesign(design)

        design.setLanguage(LocaleHelper.displayName(LocaleHelper.getSavedLanguage(this)))

        // 设置页「关于」项副标题显示版本号（替代「已更新」）
        val appVersion = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("???")
        design.setVersion("v" + (appVersion ?: "???").removeSuffix(".debug"))

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        SettingsDesign.Request.StartApp ->
                            startActivity(AppSettingsActivity::class.intent)
                        SettingsDesign.Request.StartNetwork ->
                            startActivity(NetworkSettingsActivity::class.intent)
                        SettingsDesign.Request.StartOverride ->
                            startActivity(OverrideSettingsActivity::class.intent)
                        SettingsDesign.Request.StartMetaFeature ->
                            startActivity(MetaFeatureSettingsActivity::class.intent)
                        SettingsDesign.Request.SetLanguage -> {
                            val codes = arrayOf("", "zh", "en", "ja", "ko", "ru", "vi")
                            val current = LocaleHelper.getSavedLanguage(this@SettingsActivity)
                            val checked = maxOf(0, codes.indexOf(current))
                            val names = codes.map { code -> LocaleHelper.displayName(code) }
                                .toTypedArray()

                            AlertDialog.Builder(this@SettingsActivity)
                                .setTitle(R.string.choose_language)
                                .setSingleChoiceItems(names, checked) { d, which ->
                                    d.dismiss()
                                    LocaleHelper.saveLanguage(this@SettingsActivity, codes[which])
                                    if (Build.VERSION.SDK_INT < 33) {
                                        recreate()
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                        SettingsDesign.Request.OpenDashboard -> {
                            startActivity(MainActivity::class.intent)
                            finish()
                        }
                        SettingsDesign.Request.OpenProxy -> {
                            startActivity(ProxyActivity::class.intent)
                            finish()
                        }
                        SettingsDesign.Request.OpenProfiles -> {
                            startActivity(ProfilesActivity::class.intent)
                            finish()
                        }
                        SettingsDesign.Request.OpenBackground ->
                            VendorBackground.openBackgroundSettings(this@SettingsActivity)
                        SettingsDesign.Request.About -> {
                            val versionName = runCatching {
                                packageManager.getPackageInfo(packageName, 0).versionName
                            }.getOrDefault("???")
                            val about = DesignAboutBinding.inflate(layoutInflater).apply {
                                this.versionName = versionName
                            }
                            AlertDialog.Builder(this@SettingsActivity)
                                .setView(about.root)
                                .show()
                            about.aboutGithubRow.setOnClickListener {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/$APP_REPO"))
                                )
                            }
                            about.aboutKernelRow.setOnClickListener {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/MetaCubeX/mihomo"))
                                )
                            }
                        }
                        SettingsDesign.Request.Kernel -> showKernelDialog()
                    }
                }
            }
        }
    }

    // ── Kernel Dialog: download rebuilt libbridge.so per channel ──

    private suspend fun showKernelDialog() {
        val kernel = DesignKernelBinding.inflate(layoutInflater)

        // Bridge.nativeCoreVersion() 是 JNI 调用，可能在主线程阻塞导致 ANR
        val rawVersion = withContext(Dispatchers.Default) {
            runCatching { Bridge.nativeCoreVersion() }.getOrNull()
        }
        val active = KernelManager.getActiveChannel(this)
        val baseVersion = prettyPrintVersion(rawVersion)
        // 内置内核即稳定版：显示「稳定版-1.19.27」（version 来自 core/.../clash/constant/version.go）
        val isCustom = active != null && KernelManager.isCustomActive(this)
        val channelLabel = when {
            !isCustom -> getString(R.string.kernel_channel_short_stable)
            active == KernelManager.CHANNEL_STABLE -> getString(R.string.kernel_channel_short_stable)
            active == KernelManager.CHANNEL_LATEST -> getString(R.string.kernel_channel_short_latest)
            active == KernelManager.CHANNEL_ALPHA -> getString(R.string.kernel_channel_short_alpha)
            else -> active!!
        }
        val verNumber = if (!isCustom) {
            BUNDLED_KERNEL_VERSION
        } else {
            KernelManager.getActiveVersion(this)
                ?: baseVersion.removePrefix("mihomo ").ifBlank { baseVersion }
        }
        kernel.currentVersion = "$channelLabel-$verNumber"

        val dialog = AlertDialog.Builder(this@SettingsActivity)
            .setView(kernel.root)
            .show()

        launch(Dispatchers.Main) {
            var stableVer = getString(R.string.kernel_fetching)
            var latestVer = getString(R.string.kernel_fetching)
            var alphaVer = getString(R.string.kernel_fetching)

            kernel.stableVersion = stableVer
            kernel.latestVersion = latestVer
            kernel.alphaVersion = alphaVer

            runCatching {
                withContext(Dispatchers.IO) {
                    val json = URL(GITHUB_API_RELEASES).readText()
                    parseGithubReleases(json)?.let { (s, l, a) ->
                        stableVer = s; latestVer = l; alphaVer = a
                    }
                }
            }

            kernel.stableVersion = stableVer
            kernel.latestVersion = latestVer
            kernel.alphaVersion = alphaVer
        }

        // 根据本地已安装的通道更新按钮状态
        refreshKernelButtonStates(kernel, active)

        // 自定义内核已激活时，显示「恢复内置内核」按钮
        kernel.btnRevertBuiltin.visibility = if (isCustom) View.VISIBLE else View.GONE
        kernel.btnRevertBuiltin.setOnClickListener {
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle(R.string.kernel_revert_builtin)
                .setMessage(getString(R.string.kernel_revert_confirm, "v$BUNDLED_KERNEL_VERSION"))
                .setPositiveButton(R.string.kernel_revert_go) { _, _ ->
                    KernelManager.clearCustom(this@SettingsActivity)
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.kernel_revert_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    restartApp()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        kernel.btnDownloadStable.setOnClickListener {
            startDownload(it, KernelManager.CHANNEL_STABLE)
        }
        kernel.btnDownloadLatest.setOnClickListener {
            startDownload(it, KernelManager.CHANNEL_LATEST)
        }
        kernel.btnDownloadAlpha.setOnClickListener {
            startDownload(it, KernelManager.CHANNEL_ALPHA)
        }

        kernel.releasesButton.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/${KernelManager.REPO}/releases"))
            )
        }
    }

    /**
     * 根据本地缓存状态更新三个按钮的文案与状态（三态）：
     * - 当前活跃通道 → "✓ 已安装"（禁用，已在用）
     * - 已缓存但未激活 → "切换"（点击秒切，不联网）
     * - 未缓存 → "下载"（点击联网下载）
     */
    private fun refreshKernelButtonStates(
        kernel: DesignKernelBinding,
        activeChannel: String?
    ) {
        val btnStable = kernel.btnDownloadStable
        val btnLatest = kernel.btnDownloadLatest
        val btnAlpha = kernel.btnDownloadAlpha

        val installedText = getString(R.string.kernel_installed)
        val downloadText = getString(R.string.kernel_download)
        val switchText = getString(R.string.kernel_switch)

        fun setState(btn: com.google.android.material.button.MaterialButton, channel: String) {
            when {
                channel == activeChannel -> {
                    btn.text = installedText; btn.isEnabled = false
                }
                KernelManager.isChannelCached(this, channel) -> {
                    btn.text = switchText; btn.isEnabled = true
                }
                else -> {
                    btn.text = downloadText; btn.isEnabled = true
                }
            }
        }
        setState(btnStable, KernelManager.CHANNEL_STABLE)
        setState(btnLatest, KernelManager.CHANNEL_LATEST)
        setState(btnAlpha, KernelManager.CHANNEL_ALPHA)
    }

    private fun startDownload(btn: View, channel: String) {
        btn.isEnabled = false
        (btn as com.google.android.material.button.MaterialButton).text =
            getString(R.string.kernel_switching)
        launch { kernelAction(btn, channel) }
    }

    /**
     * 点击某通道按钮的统一入口：
     * - 若该通道已缓存 → 直接秒切（不联网），重启后生效；
     * - 否则联网下载、校验、安装，重启后生效。
     */
    private suspend fun kernelAction(btn: View, channel: String) {
        val ctx = this@SettingsActivity
        val ok = if (KernelManager.isChannelCached(ctx, channel)) {
            // 本地已缓存：直接切换，无需重新下载
            KernelManager.switchToCached(ctx, channel)
        } else {
            // 未缓存：下载 → 校验 → 安装
            runCatching {
                val info = withContext(Dispatchers.IO) {
                    val json = URL(GITHUB_API_RELEASES).readText()
                    findAsset(ctx, json, channel)
                        ?: throw IllegalStateException(getString(R.string.kernel_no_asset))
                }
                withContext(Dispatchers.IO) {
                    KernelManager.install(ctx, channel, info.url, info.sha256) { url, file ->
                        downloadFile(url, file)
                    }
                }
                // 记录已下载内核的语义版本，供「当前版本」展示与切回恢复
                if (!info.version.isNullOrBlank()) {
                    KernelManager.setActiveVersion(ctx, info.version)
                    KernelManager.setCachedVersion(ctx, channel, info.version)
                }
                true
            }.getOrElse { false }
        }

        if (ok) {
            (btn as com.google.android.material.button.MaterialButton).text =
                getString(R.string.kernel_ready)
            Toast.makeText(ctx, getString(R.string.kernel_download_success), Toast.LENGTH_LONG).show()
            restartApp()
        } else {
            (btn as com.google.android.material.button.MaterialButton).text =
                getString(R.string.kernel_download)
            btn.isEnabled = true
            Toast.makeText(ctx, getString(R.string.kernel_download_failed), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Find the libbridge.so asset URL (and expected sha256) for [channel].
     * The kernel repo publishes, per channel, a release containing
     * libbridge-<abi>.so and a sha256.txt manifest.
     */
    private data class AssetInfo(
        val url: String,
        val sha256: String?,
        val version: String?,
    )

    private fun findAsset(ctx: Context, releasesJson: String, channel: String): AssetInfo? {
        val arr = JSONArray(releasesJson)
        var target: JSONObject? = null
        for (i in 0 until arr.length()) {
            val rel = arr.getJSONObject(i)
            val isPre = rel.optBoolean("prerelease", false)
            val tag = rel.optString("tag_name", "")
            when (channel) {
                KernelManager.CHANNEL_STABLE -> if (tag != "stable") continue
                KernelManager.CHANNEL_LATEST -> if (tag != "latest") continue
                KernelManager.CHANNEL_ALPHA -> if (!isPre) continue
            }
            target = rel
            break
        }
        val rel = target ?: return null

        val abi = supportedAbi(ctx)
        val assets = rel.getJSONArray("assets")
        var assetUrl: String? = null
        var shaUrl: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            when (a.getString("name")) {
                "libbridge-${abi}.so" -> assetUrl = a.getString("browser_download_url")
                "sha256.txt" -> shaUrl = a.getString("browser_download_url")
            }
        }
        if (assetUrl == null) return null

        val expected = shaUrl?.let { url ->
            runCatching {
                URL(url).readText().lineSequence()
                    .firstOrNull { it.contains("libbridge-${abi}.so") }
                    ?.substringBefore(" ")?.trim()
            }.getOrNull()
        }

        // release 命名形如 "Kernel stable · mihomo v1.19.29" → 提取 1.19.29
        val version = Regex("""mihomo\s+v?(\d+\.\d+\.\d+)""")
            .find(rel.optString("name", ""))?.groupValues?.get(1)

        return AssetInfo(assetUrl, expected, version)
    }

    private fun supportedAbi(ctx: Context): String {
        val order = listOf("arm64-v8a", "x86_64", "armeabi-v7a", "x86")
        return Build.SUPPORTED_ABIS.firstOrNull { it in order } ?: "arm64-v8a"
    }

    private fun parseGithubReleases(json: String): Triple<String, String, String>? {
        val arr = JSONArray(json)
        var stable: String? = null
        var latest: String? = null
        var alpha: String? = null
        for (i in 0 until arr.length()) {
            val rel = arr.getJSONObject(i)
            val tag = rel.optString("tag_name", "")
            val name = rel.optString("name", "N/A")
            val isPre = rel.optBoolean("prerelease", false)
            when {
                tag == "stable" -> if (stable == null) stable = name
                tag == "latest" -> if (latest == null) latest = name
                isPre -> if (alpha == null) alpha = name
            }
        }
        if (stable == null && latest == null && alpha == null) return null
        return Triple(stable ?: "N/A", latest ?: stable ?: "N/A", alpha ?: "N/A")
    }

    /**
     * Restart the app process shortly after exiting so the freshly installed
     * libbridge.so is loaded. Uses AlarmManager to re-launch the launcher.
     */
    private fun restartApp() {
        val ctx = this@SettingsActivity
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: return
        val pi = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarm = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC, System.currentTimeMillis() + 400, pi
        )
        System.exit(0)
    }

    private fun prettyPrintVersion(raw: String?): String {
        if (raw.isNullOrBlank()) return getString(R.string.kernel_current_value)

        val cleaned = raw.removePrefix("unknown_unknown_")
            .removePrefix("unknown_")
            .trim()

        if (cleaned.contains(".") && cleaned.any { it.isDigit() }) {
            val verMatch = Regex("""v?\d+\.\d+[\.\w]*""").find(cleaned)
            return verMatch?.value?.let { "mihomo $it" } ?: cleaned
        }

        return if (cleaned.length > 4) cleaned else getString(R.string.kernel_current_value)
    }

    private fun downloadFile(urlString: String, dest: File) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/octet-stream")
            conn.setRequestProperty("User-Agent", "Flowly-KernelUpdater")

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("HTTP ${conn.responseCode}")
            }

            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
