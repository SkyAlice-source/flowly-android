package com.github.kr328.clash.core.bridge

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.log.Log
import kotlinx.coroutines.CompletableDeferred
import java.io.File

@Keep
object Bridge {
    external fun nativeReset()
    external fun nativeForceGc()
    external fun nativeSuspend(suspend: Boolean)
    external fun nativeQueryTunnelState(): String
    external fun nativeQueryTrafficNow(): Long
    external fun nativeQueryTrafficTotal(): Long
    external fun nativeNotifyDnsChanged(dnsList: String)
    external fun nativeNotifyTimeZoneChanged(name: String, offset: Int)
    external fun nativeNotifyInstalledAppChanged(uidList: String)
    external fun nativeStartTun(fd: Int, stack: String, gateway: String, portal: String, dns: String, cb: TunInterface)
    external fun nativeStopTun()
    external fun nativeStartHttp(listenAt: String): String?
    external fun nativeStopHttp()
    external fun nativeQueryGroupNames(excludeNotSelectable: Boolean): String
    external fun nativeQueryGroup(name: String, sort: String): String?
    external fun nativeHealthCheck(completable: CompletableDeferred<Unit>, name: String)
    external fun nativeHealthCheckAll()
    external fun nativePatchSelector(selector: String, name: String): Boolean
    external fun nativeFetchAndValid(
        completable: FetchCallback,
        path: String,
        url: String,
        force: Boolean
    )

    external fun nativeLoad(completable: CompletableDeferred<Unit>, path: String)
    external fun nativeQueryProviders(): String
    external fun nativeUpdateProvider(
        completable: CompletableDeferred<Unit>,
        type: String,
        name: String
    )

    external fun nativeReadOverride(slot: Int): String
    external fun nativeWriteOverride(slot: Int, content: String)
    external fun nativeClearOverride(slot: Int)
    external fun nativeQueryConfiguration(): String
    external fun nativeSubscribeLogcat(callback: LogcatInterface)
    external fun nativeCoreVersion(): String

    external fun nativeSetAgeSecretKey(key: String?)
    external fun nativeGenX25519KeyPair(): String?
    external fun nativeGenHybridKeyPair(): String?
    external fun nativeVeritySecretKeys(secretKeys: String): Boolean
    external fun nativeToPublicKeys(secretKeys: String): String?
    external fun nativeVerityPublicKeys(publicKeys: String): Boolean

    private external fun nativeInit(home: String, versionName: String, sdkVersion: Int)

    private fun loadNativeLibrary() {
        // Flowly 下载内核模型：每个通道的 libbridge.so 缓存在 kernel/cache/<channel>.so，
        // 激活通道记录在 kernel/active 文本文件。Bridge 启动时按标记加载对应缓存；
        // 无标记则回退到内置 bundled 库。
        // 注意：下载的 libbridge.so 是约 70-85KB 的小型 JNI 桥，不是 MB 级核心包，
        // 故用 10KB 下限（旧代码 1MB 阈值会拒绝每一个合法下载）。
        val filesDir = Global.application.filesDir
        val marker = File(filesDir, "kernel/active")
        var loaded = false
        if (marker.exists()) {
            val channel = marker.readText().trim()
            if (channel == "stable" || channel == "latest" || channel == "alpha") {
                val so = File(filesDir, "kernel/cache/$channel.so")
                if (so.exists() && so.length() > 10_000L) {
                    try {
                        System.load(so.absolutePath)
                        Log.i("Bridge: loaded custom $channel kernel from ${so.absolutePath}")
                        loaded = true
                    } catch (e: Throwable) {
                        Log.e("Bridge: failed to load custom $channel kernel, falling back to bundled", e)
                    }
                }
            }
        }
        if (!loaded) {
            System.loadLibrary("bridge")
        }
    }

    init {
        loadNativeLibrary()

        val ctx = Global.application

        ParcelFileDescriptor.open(File(ctx.packageCodePath), ParcelFileDescriptor.MODE_READ_ONLY)
            .detachFd()

        val home = ctx.filesDir.resolve("clash").apply { mkdirs() }.absolutePath
        val versionName = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "unknown"
        val sdkVersion = Build.VERSION.SDK_INT

        Log.d("Home = $home")

        nativeInit(home, versionName, sdkVersion)
    }
}