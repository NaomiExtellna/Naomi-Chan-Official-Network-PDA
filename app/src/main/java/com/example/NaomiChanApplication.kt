package com.example

import android.app.Application
import com.example.util.DiagnosticLog

/**
 * Process-level setup for the Naomi-Chan™ BFC Warehouse terminal.
 *
 * The handheld keeps authentication and warehouse audit events locally while
 * retaining the existing SUNMI-safe startup path. Startup installs only the
 * local diagnostics handler before Compose and Room initialize.
 */
class NaomiChanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.installCrashHandler(this)
        DiagnosticLog.log(
            this,
            "INFO",
            "Application",
            "BFC Warehouse started; SDK=${android.os.Build.VERSION.SDK_INT}, release=${android.os.Build.VERSION.RELEASE}"
        )
    }
}
