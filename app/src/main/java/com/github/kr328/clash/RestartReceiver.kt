package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.service.StatusProvider
import com.github.kr328.clash.util.startClashService

/**
 * 连接自动恢复：
 *  - 开机 / 应用更新后，若上次是连接态（service_running.lock 还在）则自动重连；
 *  - 用户解锁（USER_PRESENT）时同样尝试恢复 —— 这是对抗厂商后台强杀的关键：
 *    进程被杀后，下一次解锁即静默拉起 VPN（授权仍有效，不会弹确认框）。
 *
 * 注意：Android 12+ 限制后台启动前台服务，USER_PRESENT 不在豁免清单内，
 * 抛出 ForegroundServiceStartNotAllowedException 时静默跳过 ——
 * 用户打开 App 后 MainActivity 的 recoverIfKilledInBackground() 会兜底恢复。
 */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT -> {
                if (!StatusProvider.shouldStartClashOnBoot)
                    return

                try {
                    // VPN 授权被撤销时返回确认 Intent，Receiver 中无法弹 UI，跳过
                    context.startClashService()
                } catch (e: Exception) {
                    Log.w("Auto restart clash service failed: ${e.message}")
                }
            }
        }
    }
}
