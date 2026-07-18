package com.github.kr328.clash

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.SettingsDesign
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignKernelBinding
import com.github.kr328.clash.util.LocaleHelper
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class SettingsActivity : BaseActivity<SettingsDesign>() {
    override suspend fun main() {
        val design = SettingsDesign(this)

        setContentDesign(design)

        design.setLanguage(LocaleHelper.displayName(LocaleHelper.getSavedLanguage(this)))

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
                        }
                        SettingsDesign.Request.Kernel -> {
                            val kernel = DesignKernelBinding.inflate(layoutInflater)
                            kernel.currentVersion = runCatching { Bridge.nativeCoreVersion() }
                                .getOrDefault(getString(R.string.kernel_current_value))
                            kernel.releasesButton.setOnClickListener {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/MetaCubeX/mihomo/releases"),
                                    ),
                                )
                            }
                            AlertDialog.Builder(this@SettingsActivity)
                                .setView(kernel.root)
                                .show()
                        }
                    }
                }
            }
        }
    }
}
