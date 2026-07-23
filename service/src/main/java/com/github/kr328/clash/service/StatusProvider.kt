package com.github.kr328.clash.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Process
import com.github.kr328.clash.common.Global

class StatusProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            METHOD_CURRENT_PROFILE -> {
                return if (serviceRunning)
                    Bundle().apply {
                        putString("name", currentProfile)
                    }
                else
                    null
            }
            else -> super.call(method, arg, extras)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw IllegalArgumentException("Stub!")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        throw IllegalArgumentException("Stub!")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw IllegalArgumentException("Stub!")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw IllegalArgumentException("Stub!")
    }

    override fun getType(uri: Uri): String? {
        throw IllegalArgumentException("Stub!")
    }

    override fun onCreate(): Boolean {
        return true
    }

    companion object {
        const val METHOD_CURRENT_PROFILE = "currentProfile"

        private const val CLASH_SERVICE_RUNNING_FILE = "service_running.lock"
        private const val TUN_ACTIVE_FILE = "tun_active.lock"

        var serviceRunning: Boolean = false
            set(value) {
                field = value
                if (value) {
                    shouldStartClashOnBoot = true
                    runningPid = Process.myPid().toLong()
                }
            }
        var shouldStartClashOnBoot: Boolean
            get() = Global.application.filesDir.resolve(CLASH_SERVICE_RUNNING_FILE).exists()
            set(value) {
                Global.application.filesDir.resolve(CLASH_SERVICE_RUNNING_FILE).apply {
                    if (value)
                        createNewFile()
                    else
                        delete()
                }
            }
        var currentProfile: String? = null
        var runningPid: Long = 0

        /**
         * Whether the VPN tunnel (tun interface) is currently established.
         * Persisted to a file so the main process can read it cross-process
         * (TunService runs in :background, MainActivity runs in the main proc).
         * This must reflect the REAL tunnel state, not just "is the process alive",
         * because the OS can revoke/freeze the tunnel while the process survives.
         */
        var tunActive: Boolean
            get() = Global.application.filesDir.resolve(TUN_ACTIVE_FILE).exists()
            set(value) {
                Global.application.filesDir.resolve(TUN_ACTIVE_FILE).apply {
                    if (value) createNewFile() else delete()
                }
            }

        /**
         * Check if any process we launched with [runningPid] is still alive.
         * Returns false if either no PID was recorded or the recorded process has died.
         */
        fun isProcessActive(): Boolean {
            if (runningPid == 0L) return false
            try {
                val am = Global.application.getSystemService(
                    android.content.Context.ACTIVITY_SERVICE
                ) as android.app.ActivityManager
                return am.runningAppProcesses?.any { it.pid == runningPid.toInt() } ?: false
            } catch (_: Exception) {
                return false
            }
        }
    }
}