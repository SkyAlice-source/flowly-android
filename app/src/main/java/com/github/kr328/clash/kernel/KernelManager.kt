package com.github.kr328.clash.kernel

import android.content.Context
import android.content.SharedPreferences
import com.github.kr328.clash.common.log.Log
import java.io.File
import java.security.MessageDigest

/**
 * Manages Flowly's downloadable kernel (a rebuilt libbridge.so).
 *
 * The app always loads libbridge.so from [soFile] if it exists and looks valid;
 * otherwise it falls back to the bundled library (see Bridge.loadNativeLibrary).
 *
 * The artifacts are produced by the companion repo [REPO] (SkyAlice-source/flowly-kernel),
 * which rebuilds Flowly's `core` module against different mihomo versions.
 */
object KernelManager {
    const val REPO_OWNER = "SkyAlice-source"
    const val REPO_NAME = "flowly-kernel"
    const val REPO = "$REPO_OWNER/$REPO_NAME"

    const val CHANNEL_STABLE = "stable"
    const val CHANNEL_LATEST = "latest"
    const val CHANNEL_ALPHA = "alpha"

    private const val PREFS = "flowly_kernel"
    private const val KEY_ACTIVE = "active_channel"
    private const val KEY_VERSION = "active_version"
    private const val MIN_SO_BYTES = 1_000_000L

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun soFile(ctx: Context): File =
        File(ctx.filesDir, "kernel/libbridge.so")

    fun isCustomActive(ctx: Context): Boolean {
        val f = soFile(ctx)
        return f.exists() && f.length() > MIN_SO_BYTES
    }

    fun getActiveChannel(ctx: Context): String? =
        prefs(ctx).getString(KEY_ACTIVE, null)

    fun setActiveChannel(ctx: Context, channel: String?) {
        prefs(ctx).edit().putString(KEY_ACTIVE, channel).apply()
    }

    /** 已下载内核的 mihomo 语义版本（如 "1.19.29"），用于「当前版本」展示 */
    fun getActiveVersion(ctx: Context): String? =
        prefs(ctx).getString(KEY_VERSION, null)

    fun setActiveVersion(ctx: Context, version: String?) {
        prefs(ctx).edit().putString(KEY_VERSION, version).apply()
    }

    /**
     * Downloads [assetUrl] via [downloader] into a temp file, verifies size (and
     * [expectedSha256] when provided), atomically installs it as the active
     * kernel, and records [channel] as active. Returns true on success.
     */
    fun install(
        ctx: Context,
        channel: String,
        assetUrl: String,
        expectedSha256: String?,
        downloader: (String, File) -> Unit
    ): Boolean {
        val dir = File(ctx.filesDir, "kernel").apply { mkdirs() }
        val tmp = File(dir, "libbridge.so.tmp")
        tmp.delete()
        return try {
            downloader(assetUrl, tmp)
            if (!tmp.exists() || tmp.length() < MIN_SO_BYTES) {
                Log.e("Kernel: downloaded file invalid (size=${tmp.length()})")
                false
            } else if (!expectedSha256.isNullOrBlank() &&
                !sha256(tmp).equals(expectedSha256, ignoreCase = true)
            ) {
                Log.e("Kernel: sha256 mismatch (expected=$expectedSha256)")
                false
            } else {
                val dest = soFile(ctx)
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                setActiveChannel(ctx, channel)
                Log.i("Kernel: installed $channel kernel -> ${dest.absolutePath}")
                true
            }
        } catch (e: Throwable) {
            Log.e("Kernel: install failed", e)
            false
        } finally {
            tmp.delete()
        }
    }

    fun clearCustom(ctx: Context) {
        soFile(ctx).delete()
        setActiveChannel(ctx, null)
        setActiveVersion(ctx, null)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buf = ByteArray(8192)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
