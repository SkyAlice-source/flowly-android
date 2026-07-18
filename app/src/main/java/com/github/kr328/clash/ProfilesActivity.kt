package com.github.kr328.clash

import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.ProfilesDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlin.text.Charsets
import java.util.*
import java.util.concurrent.TimeUnit

class ProfilesActivity : BaseActivity<ProfilesDesign>() {
    private val scanLauncher = registerForActivityResult(ScanQRCode()) { result ->
        scanResultHandler(result)
    }

    override suspend fun main() {
        val design = ProfilesDesign(this)

        setContentDesign(design)

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ProfileChanged -> {
                            design.fetch()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        ProfilesDesign.Request.Create ->
                            startActivity(NewProfileActivity::class.intent)
                        ProfilesDesign.Request.CreateUrl ->
                            createProfile(Profile.Type.Url)
                        ProfilesDesign.Request.CreateFile ->
                            createProfile(Profile.Type.File)
                        ProfilesDesign.Request.CreateQR ->
                            scanLauncher.launch(null)
                        ProfilesDesign.Request.UpdateAll ->
                            withProfile {
                                try {
                                    queryAll().forEach { p ->
                                        if (p.imported && p.type != Profile.Type.File)
                                            update(p.uuid)
                                    }
                                }
                                finally {
                                    withContext(Dispatchers.Main) {
                                        design.finishUpdateAll();
                                    }
                                }
                            }
                        is ProfilesDesign.Request.Update ->
                            withProfile { update(it.profile.uuid) }
                        is ProfilesDesign.Request.Delete ->
                            withProfile { delete(it.profile.uuid) }
                        is ProfilesDesign.Request.Edit ->
                            startActivity(PropertiesActivity::class.intent.setUUID(it.profile.uuid))
                        is ProfilesDesign.Request.Active -> {
                            withProfile {
                                if (it.profile.imported)
                                    setActive(it.profile)
                                else
                                    design.requestSave(it.profile)
                            }
                        }
                        is ProfilesDesign.Request.Duplicate -> {
                            val uuid = withProfile { clone(it.profile.uuid) }

                            startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                        }
                        ProfilesDesign.Request.OpenDashboard -> {
                            startActivity(MainActivity::class.intent)
                            finish()
                        }
                        ProfilesDesign.Request.OpenProxy -> {
                            startActivity(ProxyActivity::class.intent)
                            finish()
                        }
                        ProfilesDesign.Request.OpenSettings -> {
                            startActivity(SettingsActivity::class.intent)
                            finish()
                        }
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        design.updateElapsed()
                    }
                }
            }
        }
    }

    private fun createProfile(type: Profile.Type) {
        launch {
            val uuid = withProfile {
                create(type, getString(R.string.new_profile))
            }

            startActivity(PropertiesActivity::class.intent.setUUID(uuid))
        }
    }

    private fun scanResultHandler(result: QRResult) {
        lifecycleScope.launch {
            when (result) {
                is QRResult.QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.toString(Charsets.UTF_8)
                        ?: ""

                    if (url.isBlank()) {
                        design?.showToast(
                            getString(R.string.import_from_qr_empty),
                            ToastDuration.Short
                        )
                    } else {
                        createProfileByQrCode(url)
                    }
                }
                QRResult.QRUserCanceled -> Unit
                is QRResult.QRMissingPermission -> {
                    design?.showExceptionToast(getString(R.string.import_from_qr_no_permission))
                }
                is QRResult.QRError -> {
                    design?.showExceptionToast(getString(R.string.import_from_qr_exception))
                }
            }
        }
    }

    private suspend fun createProfileByQrCode(url: String) {
        val uuid = withProfile {
            create(Profile.Type.Url, getString(R.string.new_profile), url)
        }

        startActivity(PropertiesActivity::class.intent.setUUID(uuid))
    }

    private suspend fun ProfilesDesign.fetch() {
        withProfile {
            patchProfiles(queryAll())
        }
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if (uuid == null)
            return;
        launch {
            var name: String? = null;
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            design?.showToast(
                getString(R.string.toast_profile_updated_complete, name),
                ToastDuration.Long
            )
        }
    }

    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if (uuid == null)
            return;
        launch {
            var name: String? = null;
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            design?.showToast(
                getString(R.string.toast_profile_updated_failed, name, reason),
                ToastDuration.Long
            ) {
                setAction(R.string.edit) {
                    startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                }
            }
        }
    }
}
