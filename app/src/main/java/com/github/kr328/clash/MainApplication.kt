package com.github.kr328.clash

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.compat.currentProcessName
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.util.sendServiceRecreated
import com.github.kr328.clash.util.LocaleHelper
import com.github.kr328.clash.util.clashDir
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class MainApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleHelper.wrap(base))

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        // P2-11: Apply saved dark mode preference before any Activity is created
        applyDarkMode()

        val processName = currentProcessName
        extractGeoFiles()

        Log.d("Process $processName started")

        if (processName == packageName) {
            Remote.launch()
        } else {
            sendServiceRecreated()
        }
    }

    private fun applyDarkMode() {
        val mode = getSharedPreferences("ui", Context.MODE_PRIVATE)
            .getString("dark_mode", null)
        val nightMode = when (mode) {
            "ForceDark" -> AppCompatDelegate.MODE_NIGHT_YES
            "ForceLight" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> {
                // Follow system, but also check system setting
                if (android.content.res.Configuration.UI_MODE_NIGHT_MASK and
                    resources.configuration.uiMode ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun extractGeoFile(name: String, target: File, updateDate: Long) {
        if (target.exists() && target.lastModified() >= updateDate) {
            return
        }
        try {
            assets.open(name).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            // Asset not bundled in this build (e.g. dev builds without geo data).
            // Skip gracefully so the app can still launch for UI verification.
            Log.w("Geo asset '$name' not found in package, skipped", e)
        }
    }

    private fun extractGeoFiles() {
        clashDir.mkdirs()

        val updateDate = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        extractGeoFile("geoip.metadb", File(clashDir, "geoip.metadb"), updateDate)
        extractGeoFile("geosite.dat", File(clashDir, "geosite.dat"), updateDate)
        extractGeoFile("ASN.mmdb", File(clashDir, "ASN.mmdb"), updateDate)
        extractGeoFile("BundleMRS.7z", File(clashDir, "BundleMRS.7z"), updateDate)
    }

    fun finalize() {
        Global.destroy()
    }
}
