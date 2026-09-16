package com.example.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLog {
    private const val FILE_NAME = "pda_diagnostics.log"
    private const val MAX_BYTES = 256 * 1024L
    @Volatile private var installed = false

    @Synchronized
    fun log(context: Context, level: String, source: String, message: String) {
        runCatching {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            if (file.exists() && file.length() > MAX_BYTES) {
                val keep = file.readLines().takeLast(300)
                file.writeText(keep.joinToString("\n", postfix = if (keep.isEmpty()) "" else "\n"))
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(Date())
            val safe = message.replace('\n', ' ').replace('\r', ' ').take(1200)
            file.appendText("$stamp | ${level.uppercase()} | $source | $safe\n")
        }
    }

    fun error(context: Context, source: String, throwable: Throwable) {
        log(context, "ERROR", source, "${throwable.javaClass.simpleName}: ${throwable.message ?: "No message"}")
    }

    fun readRecent(context: Context, limit: Int = 40): List<String> = runCatching {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) emptyList() else file.readLines().takeLast(limit.coerceIn(1, 200)).reversed()
    }.getOrDefault(emptyList())

    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE_NAME).delete() }
    }

    fun installCrashHandler(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            installed = true
            val appContext = context.applicationContext
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                error(appContext, "UNCAUGHT/${thread.name}", throwable)
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
