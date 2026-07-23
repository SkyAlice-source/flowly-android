package com.github.kr328.clash.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 厂商「后台运行 / 自启动」设置页深链。
 *
 * 国产 ROM（MIUI、EMUI/HarmonyOS、ColorOS、OriginOS/Funtouch、OneUI…）杀后台
 * 用的是各自厂商的「后台活动管理 / 自启动管理」，与 Android 标准电池优化
 * （Doze 白名单）是两套独立机制——只引导用户去标准电池页往往无效。
 * 这里按厂商依次尝试已知组件，全部失败再逐级回退到通用页面。
 */
object VendorBackground {

    /**
     * 打开当前设备厂商的后台运行管理页。
     * @return 是否成功打开了某个设置页（最终兜底为应用详情页）。
     */
    fun openBackgroundSettings(context: Context): Boolean {
        for (intent in candidateIntents(context)) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // 该厂商组件不存在，尝试下一个
            }
        }
        return false
    }

    private fun candidateIntents(context: Context): List<Intent> {
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()

        val vendor = when {
            "xiaomi" in manufacturer || "redmi" in manufacturer -> xiaomiIntents(context)
            "huawei" in manufacturer -> huaweiIntents()
            "honor" in manufacturer || "hihonor" in manufacturer -> honorIntents()
            "oppo" in manufacturer || "oneplus" in manufacturer || "realme" in manufacturer ->
                oppoIntents()
            "vivo" in manufacturer -> vivoIntents()
            "samsung" in manufacturer -> samsungIntents()
            "meizu" in manufacturer -> meizuIntents(context)
            else -> emptyList()
        }

        // 厂商页全部不可用时的通用回退链
        val fallback = buildList {
            add(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        return vendor + fallback
    }

    private fun componentIntent(pkg: String, cls: String): Intent =
        Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** 小米 / Redmi（MIUI / HyperOS）：自启动管理 + 神隐模式应用配置 */
    private fun xiaomiIntents(context: Context): List<Intent> = listOf(
        componentIntent(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        ),
        componentIntent(
            "com.miui.powerkeeper",
            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
        ).apply {
            putExtra("package_name", context.packageName)
            putExtra("package_label", appName(context))
        },
    )

    /** 华为（EMUI / HarmonyOS）：应用启动管理 + 受保护应用 + 电池管理 */
    private fun huaweiIntents(): List<Intent> = listOf(
        componentIntent(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        ),
        componentIntent(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        ),
        componentIntent(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"
        ),
    )

    /** 荣耀（MagicOS）：独立后的系统管家包名 */
    private fun honorIntents(): List<Intent> = listOf(
        componentIntent(
            "com.hihonor.systemmanager",
            "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        ),
        componentIntent(
            "com.hihonor.systemmanager",
            "com.hihonor.systemmanager.optimize.process.ProtectActivity"
        ),
    )

    /** OPPO / OnePlus / realme（ColorOS）：自启动管理页（新旧安全中心包名） */
    private fun oppoIntents(): List<Intent> = listOf(
        componentIntent(
            "com.oplus.safecenter",
            "com.oplus.safecenter.startupapp.StartupAppListActivity"
        ),
        componentIntent(
            "com.coloros.safecenter",
            "com.coloros.safecenter.startupapp.StartupAppListActivity"
        ),
        componentIntent(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        ),
    )

    /** vivo（OriginOS / FuntouchOS）：后台高耗电 / 自启动管理 */
    private fun vivoIntents(): List<Intent> = listOf(
        componentIntent(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ),
        componentIntent(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
        ),
        componentIntent(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        ),
    )

    /** 三星（OneUI）：电池管理页 */
    private fun samsungIntents(): List<Intent> = listOf(
        componentIntent(
            "com.samsung.android.lool",
            "com.samsung.android.sm.battery.ui.BatteryActivity"
        ),
        componentIntent(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        ),
    )

    /** 魅族（Flyme）：应用安全页（需带包名参数） */
    private fun meizuIntents(context: Context): List<Intent> = listOf(
        componentIntent("com.meizu.safe", "com.meizu.safe.security.SHOW_APPSEC").apply {
            putExtra("packageName", context.packageName)
        },
    )

    private fun appName(context: Context): String = try {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    } catch (_: Exception) {
        context.packageName
    }
}
