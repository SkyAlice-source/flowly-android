package com.github.kr328.clash.kernel

import android.content.Context
import android.content.SharedPreferences
import com.github.kr328.clash.common.log.Log
import java.io.File
import java.security.MessageDigest

/**
 * Manages Flowly's downloadable kernel (a rebuilt libbridge.so).
 *
 * Model: each channel's downloaded libbridge.so is cached permanently under
 * [cacheDir] (e.g. kernel/cache/stable.so). The "active" channel is recorded in
 * [activeMarker] (a tiny text file holding the channel name). On app start,
 * Bridge.loadNativeLibrary reads the marker and loads the matching cached .so;
 * if no marker is present it falls back to the bundled library.
 *
 * Reverting to the built-in kernel only clears the marker — cached .so files are
 * kept, so switching back later is instant (no re-download).
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

    private val CHANNELS = setOf(CHANNEL_STABLE, CHANNEL_LATEST, CHANNEL_ALPHA)

    private const val PREFS = "flowly_kernel"
    private const val KEY_ACTIVE = "active_channel"
    private const val KEY_VERSION = "active_version"
    // Flowly's downloadable libbridge.so is a small JNI bridge (~70-85 KB across
    // ABIs), NOT a multi-MB core blob. The old 1 MB floor rejected every valid
    // download. 10 KB is a sane sanity floor that still rejects empty/truncated files.
    const val MIN_SO_BYTES = 10_000L

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun kernelDir(ctx: Context) = File(ctx.filesDir, "kernel").apply { mkdirs() }
    private fun cacheDir(ctx: Context) = File(kernelDir(ctx), "cache").apply { mkdirs() }

    /** 每个通道的永久缓存文件路径（不会被回退操作删除）。 */
    fun cacheFile(ctx: Context, channel: String): File =
        File(cacheDir(ctx), "$channel.so")

    /** 标记当前激活通道的文本文件；不存在表示使用内置内核。 */
    private fun activeMarker(ctx: Context): File =
        File(kernelDir(ctx), "active")

    /**
     * 兼容旧版（单文件 kernel/libbridge.so）遗留数据：首次升级时把旧文件迁到
     * 缓存目录，并写入激活标记，避免用户已下载的内核在升级后“消失”。
     */
    private fun migrateLegacyIfNeeded(ctx: Context) {
        val legacy = File(kernelDir(ctx), "libbridge.so")
        if (!legacy.exists() || legacy.length() < MIN_SO_BYTES) return
        val channel = prefs(ctx).getString(KEY_ACTIVE, CHANNEL_STABLE) ?: CHANNEL_STABLE
        val target = cacheFile(ctx, channel)
        if (!target.exists()) {
            if (!legacy.renameTo(target)) legacy.copyTo(target, overwrite = true)
        }
        val marker = activeMarker(ctx)
        if (!marker.exists()) marker.writeText(channel)
        legacy.delete()
        Log.i("Kernel: migrated legacy $channel kernel -> ${target.absolutePath}")
    }

    /** 已下载且当前激活的通道；null = 使用内置内核。 */
    fun getActiveChannel(ctx: Context): String? {
        migrateLegacyIfNeeded(ctx)
        val marker = activeMarker(ctx)
        if (!marker.exists()) return null
        val ch = marker.readText().trim()
        return if (ch in CHANNELS) ch else null
    }

    fun setActiveChannel(ctx: Context, channel: String?) {
        val marker = activeMarker(ctx)
        if (channel == null) marker.delete()
        else marker.writeText(channel)
        // 同时保留 prefs（供调试 / 旧逻辑兼容）
        prefs(ctx).edit().putString(KEY_ACTIVE, channel).commit()
    }

    /** 某通道的 .so 是否已缓存（下载过且大小合理）。 */
    fun isChannelCached(ctx: Context, channel: String): Boolean {
        val f = cacheFile(ctx, channel)
        return f.exists() && f.length() > MIN_SO_BYTES
    }

    /** 当前是否正在使用某个已下载的自定义内核（而非内置）。 */
    fun isCustomActive(ctx: Context): Boolean {
        val ch = getActiveChannel(ctx) ?: return false
        return isChannelCached(ctx, ch)
    }

    /** 已下载内核的 mihomo 语义版本（如 "1.19.29"），用于「当前版本」展示。 */
    fun getActiveVersion(ctx: Context): String? =
        prefs(ctx).getString(KEY_VERSION, null)

    fun setActiveVersion(ctx: Context, version: String?) {
        prefs(ctx).edit().putString(KEY_VERSION, version).commit()
    }

    /** 按通道记录缓存时对应的内核版本（回退后切回可恢复显示）。 */
    private fun cachedVersionKey(channel: String) = "cached_version_$channel"
    fun setCachedVersion(ctx: Context, channel: String, version: String?) {
        prefs(ctx).edit().putString(cachedVersionKey(channel), version).commit()
    }
    fun getCachedVersion(ctx: Context, channel: String): String? =
        prefs(ctx).getString(cachedVersionKey(channel), null)

    /**
     * Downloads [assetUrl] via [downloader] into a temp file, verifies size (and
     * [expectedSha256] when provided), stores it in the channel cache, and records
     * [channel] as the active channel. Returns true on success.
     */
    fun install(
        ctx: Context,
        channel: String,
        assetUrl: String,
        expectedSha256: String?,
        downloader: (String, File) -> Unit
    ): Boolean {
        val tmp = File(cacheDir(ctx), "$channel.so.tmp")
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
                val dest = cacheFile(ctx, channel)
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

    /** 切换到一个已缓存的通道（无需联网），设为激活并重启后生效。 */
    fun switchToCached(ctx: Context, channel: String): Boolean {
        if (!isChannelCached(ctx, channel)) return false
        setActiveChannel(ctx, channel)
        // 恢复该通道缓存时记录的版本号，保证「当前版本」展示正确
        setActiveVersion(ctx, getCachedVersion(ctx, channel))
        Log.i("Kernel: switched to cached $channel kernel")
        return true
    }

    /** 回退到内置内核：仅清除激活标记，缓存文件保留。 */
    fun clearCustom(ctx: Context) {
        activeMarker(ctx).delete()
        setActiveChannel(ctx, null)
        setActiveVersion(ctx, null)
        Log.i("Kernel: reverted to built-in kernel (cache kept)")
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
