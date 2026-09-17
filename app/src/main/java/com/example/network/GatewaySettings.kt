package com.example.network

import android.content.Context
import android.os.Build
import java.util.UUID

/**
 * Process-wide gateway configuration for the PDA.
 *
 * The configured Flask URL must survive app restarts on the physical SUNMI;
 * otherwise the app would fall back to the Android-emulator-only 10.0.2.2
 * address. Device identity is also stable so the Flask portal can show one
 * persistent terminal rather than a new row for every heartbeat.
 */
object GatewaySettings {
    private const val PREFS_NAME = "naomi_gateway"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_DEVICE_ID = "device_id"
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:5000"

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        ensureDeviceId()
    }

    fun getBaseUrl(): String {
        val context = appContext ?: return DEFAULT_BASE_URL
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_BASE_URL)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        val context = appContext ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, url.trim().trimEnd('/'))
            .apply()
    }

    fun getDeviceId(): String = ensureDeviceId()

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Naomi-Chan PDA" }
    }

    private fun ensureDeviceId(): String {
        val context = appContext
        if (context == null) {
            val fallback = Build.FINGERPRINT.orEmpty().hashCode().toUInt().toString(16).uppercase()
            return "PDA-$fallback"
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }

        val id = "PDA-" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }
}
