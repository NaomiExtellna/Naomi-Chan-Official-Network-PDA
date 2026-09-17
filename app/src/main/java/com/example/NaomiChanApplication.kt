package com.example

import android.app.Application
import com.example.network.GatewaySettings
import com.example.network.WirelessFlaskClient
import com.example.util.DiagnosticLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Installs diagnostics and lightweight network identity as soon as the process
 * starts. The heartbeat runs on IO and never blocks the SUNMI UI thread.
 */
class NaomiChanApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        GatewaySettings.initialize(this)
        DiagnosticLog.installCrashHandler(this)
        DiagnosticLog.log(
            this,
            "INFO",
            "Application",
            "Process started; SDK=${android.os.Build.VERSION.SDK_INT}, release=${android.os.Build.VERSION.RELEASE}"
        )

        appScope.launch {
            delay(5_000)
            val heartbeatClient = WirelessFlaskClient()
            while (isActive) {
                val configuredUrl = GatewaySettings.getBaseUrl()
                if (heartbeatClient.getBaseUrl() != configuredUrl) {
                    heartbeatClient.updateBaseUrl(configuredUrl)
                }
                heartbeatClient.syncDevicePresence()
                delay(60_000)
            }
        }
    }
}
