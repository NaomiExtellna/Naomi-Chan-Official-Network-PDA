package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class CatalogItem(
    val id: String,
    val name: String,
    val price: Double,
    val category: String,
    val description: String,
    val barcode: String,
    val itemType: String,
    val eventId: String,
    val active: Boolean
)

sealed class CatalogLookupResult {
    data class Found(val item: CatalogItem) : CatalogLookupResult()
    data class NotFound(val barcode: String) : CatalogLookupResult()
    data class Error(val message: String) : CatalogLookupResult()
}

object CatalogClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun lookupBarcode(baseUrl: String, barcode: String): CatalogLookupResult = withContext(Dispatchers.IO) {
        val cleanBase = normaliseBaseUrl(baseUrl)
            ?: return@withContext CatalogLookupResult.Error("Invalid POS gateway address")
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return@withContext CatalogLookupResult.NotFound(cleanBarcode)

        try {
            val encoded = URLEncoder.encode(cleanBarcode, "UTF-8").replace("+", "%20")
            val request = Request.Builder()
                .url("$cleanBase/api/catalog/barcode/$encoded")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext CatalogLookupResult.NotFound(cleanBarcode)
                if (!response.isSuccessful) {
                    return@withContext CatalogLookupResult.Error("Catalogue lookup failed (${response.code})")
                }

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val item = CatalogItem(
                    id = json.optString("id"),
                    name = json.optString("name", "Catalogue item"),
                    price = json.optDouble("price", 0.0),
                    category = json.optString("category", "Event Services"),
                    description = json.optString("description", json.optString("desc", "")),
                    barcode = json.optString("barcode", cleanBarcode),
                    itemType = json.optString("item_type", "SERVICE"),
                    eventId = json.optString("event_id", ""),
                    active = json.optBoolean("active", true)
                )
                if (item.id.isBlank() || !item.active) {
                    CatalogLookupResult.NotFound(cleanBarcode)
                } else {
                    CatalogLookupResult.Found(item)
                }
            }
        } catch (e: Exception) {
            Log.w("CatalogClient", "Barcode lookup failed: ${e.message}")
            CatalogLookupResult.Error(e.localizedMessage ?: "Unable to reach the POS gateway")
        }
    }

    private fun normaliseBaseUrl(value: String): String? {
        var clean = value.trim()
        if (!clean.startsWith("http://", true) && !clean.startsWith("https://", true)) clean = "http://$clean"
        clean = clean.trimEnd('/')
        return clean.toHttpUrlOrNull()?.toString()?.trimEnd('/')
    }
}
