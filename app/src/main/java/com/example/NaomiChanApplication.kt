package com.example

import android.app.Application
import com.example.util.DiagnosticLog

/**
 * Process-level setup for the standalone Naomi-Chan™ POS.
 *
 * The terminal now stores transactions locally and exports them to CSV instead
 * of maintaining a Flask/API heartbeat. Startup therefore only installs the
 * local diagnostics handler.
 */
class NaomiChanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.installCrashHandler(this)
        DiagnosticLog.log(
            this,
            "INFO",
            "Application",
            "Standalone POS started; SDK=${android.os.Build.VERSION.SDK_INT}, release=${android.os.Build.VERSION.RELEASE}"
        )
    }
}
