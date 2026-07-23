package com.github.kr328.clash.remote

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.service.RemoteService
import com.github.kr328.clash.service.StatusProvider
import com.github.kr328.clash.service.remote.IRemoteService
import com.github.kr328.clash.service.remote.unwrap
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.unbindServiceSilent
import java.util.concurrent.TimeUnit

class Service(private val context: Application, val crashed: () -> Unit) {
    val remote = Resource<IRemoteService>()

    private val restartHandler = Handler(Looper.getMainLooper())

    private val connection = object : ServiceConnection {
        private var lastCrashed: Long = -1

        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            remote.set(service.unwrap(IRemoteService::class))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote.set(null)

            val now = System.currentTimeMillis()
            val frequent = now - lastCrashed < TOGGLE_CRASHED_INTERVAL
            lastCrashed = now
            Log.w("RemoteService killed or crashed")

            if (frequent) {
                unbind()
                crashed()
                return
            }

            // 单次崩溃且用户期望 VPN 运行：延迟 2s 静默自愈重启，
            // 兜底 libclash.so 偶发 native crash（几分钟一次），避免 VPN 裸掉。
            // 后台启动受 Android 12+ 限制会抛异常，静默跳过（回前台/解锁时另有恢复）。
            if (StatusProvider.shouldStartClashOnBoot) {
                restartHandler.postDelayed({
                    try {
                        if (context.startClashService() != null) {
                            Log.w("Auto restart skipped: VPN permission grant required")
                        }
                    } catch (e: Exception) {
                        Log.w("Auto restart after crash failed: ${e.message}")
                    }
                }, AUTO_RESTART_DELAY)
            }
        }
    }

    fun bind() {
        try {
            context.bindService(RemoteService::class.intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            unbind()

            crashed()
        }
    }

    fun unbind() {
        context.unbindServiceSilent(connection)

        remote.set(null)
    }

    companion object {
        private val TOGGLE_CRASHED_INTERVAL = TimeUnit.SECONDS.toMillis(10)
        private val AUTO_RESTART_DELAY = TimeUnit.SECONDS.toMillis(2)
    }
}
