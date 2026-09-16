package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class BlackpoolTramStop(
    val name: String,
    val southboundStopId: String,
    val northboundStopId: String
)

data class TramDeparture(
    val stopName: String,
    val destination: String,
    val departureTime: String,
    val isLive: Boolean,
    val directionLabel: String
)

object BlackpoolTramStops {
    val FEATURED = listOf(
        BlackpoolTramStop("Pleasure Beach", "9400ZZBPPLB1", "9400ZZBPPLB2"),
        BlackpoolTramStop("South Pier", "9400ZZBPSHP1", "9400ZZBPSHP2"),
        BlackpoolTramStop("Central Pier", "9400ZZBPCLP1", "9400ZZBPCLP2"),
        BlackpoolTramStop("Tower", "9400ZZBPTOW1", "9400ZZBPTOW2"),
        BlackpoolTramStop("North Station", "9400ZZBPNRS1", "9400ZZBPNRS2"),
        BlackpoolTramStop("Bispham", "9400ZZBPBSH1", "9400ZZBPBSH2")
    )
}

class BlackpoolTramClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchDepartures(stop: BlackpoolTramStop): List<TramDeparture> = withContext(Dispatchers.IO) {
        val southbound = fetchPlatform(stop, stop.southboundStopId, "Southbound")
        val northbound = fetchPlatform(stop, stop.northboundStopId, "Northbound")
        (southbound + northbound).take(12)
    }

    private fun fetchPlatform(
        stop: BlackpoolTramStop,
        stopId: String,
        directionLabel: String
    ): List<TramDeparture> {
        val request = Request.Builder()
            .url("https://www.blackpooltransport.com/stops/$stopId")
            .header("User-Agent", "Naomi-Chan-PDA/1.0 Android Staff Utility")
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string().orEmpty()
                parseDepartures(body, stop.name, directionLabel)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDepartures(
        html: String,
        stopName: String,
        directionLabel: String
    ): List<TramDeparture> {
        val normalised = html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")

        val pattern = Regex(
            "Service\\s*-\\s*Tram\\.\\s*Destination\\s*-\\s*(.*?)\\.\\s*Departure time\\s*-\\s*(.*?)\\.\\s*Departure\\s+\\d+\\s+of\\s+\\d+\\.\\s*(Live|Scheduled)\\.",
            RegexOption.IGNORE_CASE
        )

        return pattern.findAll(normalised)
            .map { match ->
                TramDeparture(
                    stopName = stopName,
                    destination = match.groupValues[1].trim(),
                    departureTime = match.groupValues[2].trim(),
                    isLive = match.groupValues[3].equals("Live", ignoreCase = true),
                    directionLabel = directionLabel
                )
            }
            .distinctBy { "${it.destination}|${it.departureTime}|${it.directionLabel}" }
            .take(6)
            .toList()
    }
}
