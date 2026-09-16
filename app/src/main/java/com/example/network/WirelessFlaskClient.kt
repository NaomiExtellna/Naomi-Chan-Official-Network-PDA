package com.example.network

import android.util.Log
import com.example.model.PaymentMethod
import com.example.model.ReceiptData
import com.example.model.ReceiptItem
import com.example.model.WirelessOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WirelessFlaskClient(
    private var baseUrl: String = "http://10.0.2.2:5000"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun updateBaseUrl(newUrl: String): Boolean {
        var cleanUrl = newUrl.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)
        ) {
            cleanUrl = "http://$cleanUrl"
        }
        cleanUrl = cleanUrl.trimEnd('/')

        val parsed = cleanUrl.toHttpUrlOrNull()
        if (parsed == null || parsed.host.isBlank()) {
            Log.w("WirelessFlaskClient", "Rejected invalid server URL: $newUrl")
            return false
        }

        baseUrl = parsed.toString().trimEnd('/')
        return true
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun checkServerStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/status")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("WirelessFlaskClient", "Server status check failed on $baseUrl: ${e.message}")
            false
        }
    }

    suspend fun fetchPendingOrders(): List<WirelessOrder> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/orders/pending")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(bodyString)
                val orders = mutableListOf<WirelessOrder>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                    val items = mutableListOf<ReceiptItem>()

                    for (j in 0 until itemsArray.length()) {
                        val itObj = itemsArray.optJSONObject(j) ?: continue
                        items.add(
                            ReceiptItem(
                                id = itObj.optString("id"),
                                name = itObj.optString("name", "Item"),
                                quantity = itObj.optInt("quantity", 1).coerceAtLeast(1),
                                unitPrice = itObj.optDouble("unitPrice", itObj.optDouble("unit_price", 0.0))
                            )
                        )
                    }

                    val payMethodStr = obj.optString("payment_method", "CARD")
                    val paymentMethod = when (payMethodStr.uppercase()) {
                        "FREE" -> PaymentMethod.FREE_PASS
                        "CASH" -> PaymentMethod.CASH
                        "QR" -> PaymentMethod.QR_PAY
                        "WIRE" -> PaymentMethod.BANK_TRANSFER
                        else -> PaymentMethod.CARD_TERMINAL
                    }

                    orders.add(
                        WirelessOrder(
                            id = obj.optString("id", "WPOS-0000"),
                            clientName = obj.optString("client_name", "Guest"),
                            clientContact = obj.optString("client_contact", ""),
                            venueName = obj.optString("venue_name", "Blackpool Bar"),
                            items = items,
                            subtotal = obj.optDouble("subtotal", 0.0),
                            taxPercent = obj.optDouble("tax_percent", 20.0),
                            taxAmount = obj.optDouble("tax_amount", 0.0),
                            grandTotal = obj.optDouble("grand_total", 0.0),
                            paymentMethod = paymentMethod,
                            notes = obj.optString("notes", ""),
                            status = obj.optString("status", "PENDING"),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                orders
            }
        } catch (e: Exception) {
            Log.e("WirelessFlaskClient", "Error fetching pending orders: ${e.message}")
            emptyList()
        }
    }

    suspend fun markOrderPrinted(orderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/orders/$orderId/printed")
                .post(emptyBody)
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("WirelessFlaskClient", "Error marking order $orderId printed: ${e.message}")
            false
        }
    }

    suspend fun broadcastReceiptToWeb(receipt: ReceiptData): String? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", receipt.id)
                put("clientName", receipt.clientName)
                put("clientContact", receipt.clientContact)
                put("venueName", receipt.venueName)
                put("gigDate", receipt.gigDate)
                put("subtotal", receipt.subtotal)
                put("taxPercent", receipt.taxPercent)
                put("taxAmount", receipt.taxAmount)
                put("grandTotal", receipt.grandTotal)
                put("paymentMethod", receipt.paymentMethod.label)
                put("footerNotes", receipt.footerNotes)
                put("processedBy", receipt.processedBy)
                put("shiftId", receipt.shiftId ?: JSONObject.NULL)
                put("receiptStatus", receipt.receiptStatus)
                put("voidReason", receipt.voidReason ?: JSONObject.NULL)
                put("voidedAt", receipt.voidedAt ?: JSONObject.NULL)
                put("voidedBy", receipt.voidedBy ?: JSONObject.NULL)
                put("replacesReceiptId", receipt.replacesReceiptId ?: JSONObject.NULL)

                val itemsArr = JSONArray()
                for (item in receipt.items) {
                    val itemObject = JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("total", item.total)
                    }
                    itemsArr.put(itemObject)
                }
                put("items", itemsArr)
            }

            val body = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/receipts")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val responseObject = JSONObject(response.body?.string() ?: "{}")
                responseObject.optString("url", "/receipt/${receipt.id}")
            }
        } catch (e: Exception) {
            Log.e("WirelessFlaskClient", "Error broadcasting receipt: ${e.message}")
            null
        }
    }
}
