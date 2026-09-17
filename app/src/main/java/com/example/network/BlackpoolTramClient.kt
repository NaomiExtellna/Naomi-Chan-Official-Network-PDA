package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class BlackpoolTramStop(
    val name: String,
    val southboundStopId: String,
    val northboundStopId: String
)

data class TramStopInfo(
    val name: String,
    val area: String,
    val note: String = "",
    val isBranchStop: Boolean = false
)

data class TramDeparture(
    val stopName: String,
    val destination: String,
    val departureTime: String,
    val isLive: Boolean,
    val directionLabel: String
)

data class TramServiceAlert(
    val id: String,
    val title: String,
    val detail: String,
    val affectedStops: List<String>,
    val startDate: String,
    val status: String
)

object BlackpoolTramAlerts {
    val CURRENT = listOf(
        TramServiceAlert(
            id = "lord-street-2026-09",
            title = "Lord Street, Fleetwood reopened",
            detail = "Full-route trams resumed to Fleetwood Ferry after the 11-13 September 2026 Lord Street issue. London Street northbound remained closed because scaffolding obstructed the platform; use Victoria Street or Fishermans Walk. Southbound operates normally. Check live departures for the latest position.",
            affectedStops = listOf("London Street", "Victoria Street", "Fishermans Walk", "Fleetwood Ferry"),
            startDate = "2026-09-11",
            status = "MONITORING"
        )
    )
}

object BlackpoolTramStops {
    val FEATURED = listOf(
        BlackpoolTramStop("Pleasure Beach", "9400ZZBPPLB1", "9400ZZBPPLB2"),
        BlackpoolTramStop("South Pier", "9400ZZBPSHP1", "9400ZZBPSHP2"),
        BlackpoolTramStop("Central Pier", "9400ZZBPCLP1", "9400ZZBPCLP2"),
        BlackpoolTramStop("Tower", "9400ZZBPTOW1", "9400ZZBPTOW2"),
        BlackpoolTramStop("North Station", "9400ZZBPNRS1", "9400ZZBPNRS2"),
        BlackpoolTramStop("Bispham", "9400ZZBPBSH1", "9400ZZBPBSH2")
    )

    val ALL_STOPS = listOf(
        TramStopInfo("Starr Gate", "South Shore", "Southern terminus"),
        TramStopInfo("Harrow Place", "South Shore"),
        TramStopInfo("Burlington Road West", "South Shore"),
        TramStopInfo("Pleasure Beach", "South Shore", "Blackpool Pleasure Beach"),
        TramStopInfo("South Pier", "South Shore"),
        TramStopInfo("Waterloo Road", "South Shore", "Interchange for Blackpool South area"),
        TramStopInfo("St Chad's Road", "South Shore"),
        TramStopInfo("Manchester Square", "Central Blackpool"),
        TramStopInfo("Central Pier", "Central Blackpool"),
        TramStopInfo("Tower", "Central Blackpool", "Blackpool Tower / town centre"),
        TramStopInfo("North Pier", "Central Blackpool", "Northbound and southbound platforms"),
        TramStopInfo("Pleasant Street", "North Shore"),
        TramStopInfo("Wilton Parade", "North Shore"),
        TramStopInfo("Gynn Square", "North Shore"),
        TramStopInfo("Cliffs Hotel", "North Shore"),
        TramStopInfo("Cabin", "North Shore"),
        TramStopInfo("Lowther Avenue", "North Shore"),
        TramStopInfo("Cavendish Road", "North Shore"),
        TramStopInfo("Bispham", "Bispham"),
        TramStopInfo("Sandhurst Avenue", "Bispham"),
        TramStopInfo("Norbreck", "Bispham / Norbreck"),
        TramStopInfo("Norbreck North", "Bispham / Norbreck"),
        TramStopInfo("Little Bispham", "Bispham"),
        TramStopInfo("Anchorsholme Lane", "Anchorsholme"),
        TramStopInfo("Cleveleys", "Cleveleys", "Town centre / bus connections"),
        TramStopInfo("West Drive", "Cleveleys"),
        TramStopInfo("Thornton Gate", "Thornton-Cleveleys"),
        TramStopInfo("Rossall Beach", "Rossall"),
        TramStopInfo("Rossall School", "Rossall"),
        TramStopInfo("Rossall Square", "Rossall / Broadwater"),
        TramStopInfo("Broadwater", "Broadwater"),
        TramStopInfo("Heathfield Road", "Fleetwood"),
        TramStopInfo("Lindel Road", "Fleetwood"),
        TramStopInfo("Stanley Road", "Fleetwood"),
        TramStopInfo("Fishermans Walk", "Fleetwood", "Alternative stop during London Street northbound closure"),
        TramStopInfo(
            "London Street",
            "Fleetwood",
            "RECENT LORD STREET ISSUE (11-13 Sep 2026): Lord Street reopened and full-route trams resumed to Fleetwood Ferry. London Street northbound remained closed because scaffolding obstructed the platform; use Victoria Street or Fishermans Walk. Southbound operates normally. Check Live Trams for the latest status."
        ),
        TramStopInfo("Victoria Street", "Fleetwood", "Alternative stop during London Street northbound closure; Fleetwood Market area"),
        TramStopInfo("Marine Hall and Gardens", "Fleetwood", "Marine Hall / seafront"),
        TramStopInfo("Fleetwood Ferry", "Fleetwood", "Northern terminus")
    )

    val NORTH_STATION_BRANCH = listOf(
        TramStopInfo(
            name = "Talbot Square",
            area = "Central Blackpool",
            note = "Branch stop towards North Station only",
            isBranchStop = true
        ),
        TramStopInfo(
            name = "North Station",
            area = "Central Blackpool",
            note = "Blackpool North rail interchange",
            isBranchStop = true
        )
    )

    val COMPLETE_DIRECTORY = ALL_STOPS + NORTH_STATION_BRANCH
}

class BlackpoolTramClient {
    private data class CachedDepartures(val departures: List<TramDeparture>, val savedAt: Long)

    companion object {
        private val memoryCache = ConcurrentHashMap<String, CachedDepartures>()

        // Calls from Home and the dedicated tram board share the same fresh cache. The UI may
        // ask once per minute, but the radio will normally only wake for this stop every ~2 min.
        private const val FRESH_CACHE_AGE_MS = 90_000L
        private const val MAX_CACHE_AGE_MS = 6 * 60 * 60 * 1000L

        // A single OkHttp pool is shared by every tram screen. Reusing sockets avoids repeated
        // DNS/TLS/TCP setup and keeps network-active time shorter on the battery-powered PDA.
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // Parsing regexes are compiled once instead of being rebuilt for every platform response.
        private val SCRIPT_REGEX = Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
        private val STYLE_REGEX = Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
        private val TAG_REGEX = Regex("<[^>]+>")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val DEPARTURE_REGEX = Regex(
            "Service\\s*-\\s*Tram\\.\\s*Destination\\s*-\\s*(.*?)\\.\\s*Departure time\\s*-\\s*(.*?)\\.\\s*Departure\\s+\\d+\\s+of\\s+\\d+\\.\\s*(Live|Scheduled)\\.",
            RegexOption.IGNORE_CASE
        )
    }

    suspend fun fetchDepartures(stop: BlackpoolTramStop): List<TramDeparture> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = memoryCache[stop.name]

        // Fast path: if another screen has just fetched this stop, do not wake the network again.
        if (cached != null && now - cached.savedAt <= FRESH_CACHE_AGE_MS) {
            return@withContext cached.departures
        }

        // Fetch both directions together so the radio/network tail is shorter than two fully
        // sequential requests while keeping all blocking work on Dispatchers.IO.
        val fresh = coroutineScope {
            val southbound = async { fetchPlatform(stop, stop.southboundStopId, "Southbound") }
            val northbound = async { fetchPlatform(stop, stop.northboundStopId, "Northbound") }
            (southbound.await() + northbound.await()).take(12)
        }

        if (fresh.isNotEmpty()) {
            memoryCache[stop.name] = CachedDepartures(fresh, System.currentTimeMillis())
            return@withContext fresh
        }

        if (cached != null && now - cached.savedAt <= MAX_CACHE_AGE_MS) {
            return@withContext cached.departures.map { departure ->
                departure.copy(
                    isLive = false,
                    directionLabel = "${departure.directionLabel} • CACHED"
                )
            }
        }
        emptyList()
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
            sharedClient.newCall(request).execute().use { response ->
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
            .replace(SCRIPT_REGEX, " ")
            .replace(STYLE_REGEX, " ")
            .replace(TAG_REGEX, " ")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace(WHITESPACE_REGEX, " ")

        return DEPARTURE_REGEX.findAll(normalised)
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
