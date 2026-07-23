package com.github.kr328.clash.service

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.Process
import com.github.kr328.clash.common.compat.pendingIntentFlags
import com.github.kr328.clash.common.constants.Components
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.service.clash.clashRuntime
import com.github.kr328.clash.service.clash.module.*
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.cancelAndJoinBlocking
import com.github.kr328.clash.service.util.parseCIDR
import com.github.kr328.clash.service.util.sendClashStarted
import com.github.kr328.clash.service.util.sendClashStopped
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

class TunService : VpnService(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val self: TunService
        get() = this

    private var reason: String? = null

    private val runtime = clashRuntime {
        val store = ServiceStore(self)

        val close = install(CloseModule(self))
        val tun = install(TunModule(self))
        val config = install(ConfigurationModule(self))
        val network = install(NetworkObserveModule(self))

        if (store.dynamicNotification)
            install(DynamicNotificationModule(self))
        else
            install(StaticNotificationModule(self))

        install(AppListCacheModule(self))
        install(TimeZoneModule(self))
        install(SuspendModule(self))

        try {
            tun.open()

            while (isActive) {
                val quit = select<Boolean> {
                    close.onEvent {
                        true
                    }
                    config.onEvent {
                        reason = it.message

                        true
                    }
                    network.onEvent { n ->
                        if (Build.VERSION.SDK_INT in 22..28) @TargetApi(22) {
                            setUnderlyingNetworks(n?.let { arrayOf(it) })
                        }

                        false
                    }
                }

                if (quit) break
            }
        } catch (e: Exception) {
            Log.e("Create clash runtime: ${e.message}", e)

            reason = e.message
        } finally {
            withContext(NonCancellable) {
                tun.close()

                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Robust duplicate-instance guard:
        // - Normal path: serviceRunning=true means another instance already started
        // - Crash recovery (START_STICKY): if the old :background process was killed,
        //   onDestroy never fired so the lock file still exists, but the old PID is dead.
        //   We allow startup BUT clean up the stale lock if no older instance is active,
        //   because the old instance cannot resume anyway.
        if (StatusProvider.serviceRunning && StatusProvider.isProcessActive()) {
            // Duplicate — stop immediately
            return stopSelf()
        }

        val isNewInstance = !StatusProvider.isProcessActive()

        StatusProvider.serviceRunning = true
        StatusProvider.runningPid = Process.myPid().toLong()

        // NOTE: Do NOT clear shouldStartClashOnBoot here. It is the user's "expect
        // connected" intent (persisted lock) and must survive process recreation
        // (ColorOS freeze, START_STICKY restart, system VPN revoke). A process that
        // dies while the user still wanted it on MUST be auto-recovered; clearing the
        // lock here is what broke recovery. User-initiated stop already clears it in
        // ClashService (~line 84).

        // Tunnel is not built yet at this point. Start from false so a stale "true"
        // left behind by a hard crash (onDestroy never ran) is cleared on restart.
        StatusProvider.tunActive = false

        StaticNotificationModule.createNotificationChannel(this)
        StaticNotificationModule.notifyLoadingNotification(this)

        runtime.launch()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendClashStarted()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        TunModule.requestStop()

        StatusProvider.tunActive = false
        StatusProvider.serviceRunning = false

        sendClashStopped(reason)

        cancelAndJoinBlocking()

        Log.i("TunService destroyed: ${reason ?: "successfully"}")

        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        runtime.requestGc()
    }

    override fun onRevoke() {
        // The OS (or a vendor like ColorOS on background freeze) revoked the VPN.
        // The tun fd is already closed by the system, but by default VpnService does
        // NOT stop the service — so :background stays alive, clashRunning stays true,
        // and the main process wrongly thinks "VPN is connected" and skips recovery.
        // Mark the tunnel inactive and let the runtime stop cleanly (the finally block
        // calls stopSelf), which flips clashRunning to false so the main process can
        // re-establish the tunnel on next foreground via recoverIfKilledInBackground.
        Log.i("TunService revoked by system; tearing down cleanly")
        StatusProvider.tunActive = false
        TunModule.requestStop()
    }

    private fun TunModule.open() {
        val store = ServiceStore(self)

        val device = with(Builder()) {
            // Interface address
            addAddress(TUN_GATEWAY, TUN_SUBNET_PREFIX)
            if (store.allowIpv6) {
                addAddress(TUN_GATEWAY6, TUN_SUBNET_PREFIX6)
            }

            // Route
            if (store.bypassPrivateNetwork) {
                resources.getStringArray(R.array.bypass_private_route).map(::parseCIDR).forEach {
                    addRoute(it.ip, it.prefix)
                }
                if (store.allowIpv6) {
                    resources.getStringArray(R.array.bypass_private_route6).map(::parseCIDR).forEach {
                        addRoute(it.ip, it.prefix)
                    }
                }

                // Route of virtual DNS
                addRoute(TUN_DNS, 32)
                if (store.allowIpv6) {
                    addRoute(TUN_DNS6, 128)
                }
            } else {
                addRoute(NET_ANY, 0)
                if (store.allowIpv6) {
                    addRoute(NET_ANY6, 0)
                }
            }

            // Access Control
            when (store.accessControlMode) {
                AccessControlMode.AcceptAll -> Unit
                AccessControlMode.AcceptSelected -> {
                    (store.accessControlPackages + packageName).forEach {
                        runCatching { addAllowedApplication(it) }
                    }
                }
                AccessControlMode.DenySelected -> {
                    (store.accessControlPackages - packageName).forEach {
                        runCatching { addDisallowedApplication(it) }
                    }
                }
            }

            // Blocking
            setBlocking(false)

            // Mtu
            setMtu(TUN_MTU)

            // Session Name
            setSession("Clash")

            // Virtual Dns Server
            addDnsServer(TUN_DNS)
            if (store.allowIpv6) {
                addDnsServer(TUN_DNS6)
            }

            // Open MainActivity
            setConfigureIntent(
                PendingIntent.getActivity(
                    self,
                    R.id.nf_vpn_status,
                    Intent().setComponent(Components.MAIN_ACTIVITY),
                    pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
                )
            )

            // Metered
            if (Build.VERSION.SDK_INT >= 29) {
                setMetered(false)
            }

            // System Proxy
            if (Build.VERSION.SDK_INT >= 29 && store.systemProxy) {
                listenHttp()?.let {
                    setHttpProxy(
                        ProxyInfo.buildDirectProxy(
                            it.address.hostAddress,
                            it.port,
                            HTTP_PROXY_BLACK_LIST + if (store.bypassPrivateNetwork) HTTP_PROXY_LOCAL_LIST else emptyList()
                        )
                    )
                }
            }

            if (store.allowBypass) {
                allowBypass()
            }

            TunModule.TunDevice(
                fd = establish()?.detachFd()
                    ?: throw NullPointerException("Establish VPN rejected by system"),
                stack = store.tunStackMode,
                gateway = "$TUN_GATEWAY/$TUN_SUBNET_PREFIX" + if (store.allowIpv6) ",$TUN_GATEWAY6/$TUN_SUBNET_PREFIX6" else "",
                portal = TUN_PORTAL + if (store.allowIpv6) ",$TUN_PORTAL6" else "",
                dns = if (store.dnsHijacking) NET_ANY else (TUN_DNS + if (store.allowIpv6) ",$TUN_DNS6" else ""),
            )
        }

        attach(device)

        StatusProvider.tunActive = true
    }

    companion object {
        private const val TUN_MTU = 9000
        private const val TUN_SUBNET_PREFIX = 30
        private const val TUN_GATEWAY = "172.19.0.1"
        private const val TUN_SUBNET_PREFIX6 = 126
        private const val TUN_GATEWAY6 = "fdfe:dcba:9876::1"
        private const val TUN_PORTAL = "172.19.0.2"
        private const val TUN_PORTAL6 = "fdfe:dcba:9876::2"
        private const val TUN_DNS = TUN_PORTAL
        private const val TUN_DNS6 = TUN_PORTAL6
        private const val NET_ANY = "0.0.0.0"
        private const val NET_ANY6 = "::"

        private val HTTP_PROXY_LOCAL_LIST: List<String> = listOf(
            "localhost",
            "*.local",
            "127.*",
            "10.*",
            "172.16.*",
            "172.17.*",
            "172.18.*",
            "172.19.*",
            "172.2*",
            "172.30.*",
            "172.31.*",
            "192.168.*"
        )
        private val HTTP_PROXY_BLACK_LIST: List<String> = listOf(
            "*zhihu.com",
            "*zhimg.com",
            "*jd.com",
            "100ime-iat-api.xfyun.cn",
            "*360buyimg.com",
        )
    }
}
