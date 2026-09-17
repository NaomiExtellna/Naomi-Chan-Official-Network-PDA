package com.example

import android.app.Application
import com.example.util.DiagnosticLog

/**
 * Installs diagnostics as soon as the application process starts.
 * This is intentionally tiny and Android 7.1.1-safe so crashes that occur
 * while MainActivity is being loaded or verified are still recorded.
 */
class NaomiChanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.installCrashHandler(this)
        DiagnosticLog.log(
            this,
            "INFO",
            "Application",
            "Process started; SDK=${android.os.Build.VERSION.SDK_INT}, release=${android.os.Build.VERSION.RELEASE}"
        )
    }
}
