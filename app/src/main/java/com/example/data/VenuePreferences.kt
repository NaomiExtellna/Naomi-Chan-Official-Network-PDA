package com.example.data

import android.content.Context
import com.example.model.BlackpoolBar
import org.json.JSONArray
import org.json.JSONObject

class VenuePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("naomi_venue_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_VENUES = "custom_venues"
        private const val KEY_FAVOURITES = "favourite_venues"

        fun venueKey(venue: BlackpoolBar): String = "${venue.name.trim()}|${venue.address.trim()}".lowercase()
    }

    fun getCustomVenues(): List<BlackpoolBar> {
        val raw = prefs.getString(KEY_CUSTOM_VENUES, "[]").orEmpty()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val name = obj.optString("name").trim()
                    val address = obj.optString("address").trim()
                    if (name.isBlank() || address.isBlank()) continue
                    add(
                        BlackpoolBar(
                            name = name,
                            address = address,
                            area = obj.optString("area", "Custom Venue"),
                            barType = obj.optString("barType", "Event Venue"),
                            contact = obj.optString("contact", ""),
                            nearestTramStop = obj.optString("nearestTramStop", ""),
                            notes = obj.optString("notes", "Staff-added venue")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomVenue(venue: BlackpoolBar) {
        val current = getCustomVenues().toMutableList()
        val key = venueKey(venue)
        val existing = current.indexOfFirst { venueKey(it) == key }
        if (existing >= 0) current[existing] = venue else current.add(venue)
        persistCustomVenues(current)
    }

    fun deleteCustomVenue(venue: BlackpoolBar) {
        val key = venueKey(venue)
        persistCustomVenues(getCustomVenues().filterNot { venueKey(it) == key })
        val favourites = favouriteKeys().toMutableSet()
        if (favourites.remove(key)) saveFavouriteKeys(favourites)
    }

    fun isFavourite(venue: BlackpoolBar): Boolean = venueKey(venue) in favouriteKeys()

    fun toggleFavourite(venue: BlackpoolBar): Boolean {
        val key = venueKey(venue)
        val favourites = favouriteKeys().toMutableSet()
        val nowFavourite = if (key in favourites) {
            favourites.remove(key)
            false
        } else {
            favourites.add(key)
            true
        }
        saveFavouriteKeys(favourites)
        return nowFavourite
    }

    private fun persistCustomVenues(venues: List<BlackpoolBar>) {
        val array = JSONArray()
        venues.forEach { venue ->
            array.put(JSONObject().apply {
                put("name", venue.name)
                put("address", venue.address)
                put("area", venue.area)
                put("barType", venue.barType)
                put("contact", venue.contact)
                put("nearestTramStop", venue.nearestTramStop)
                put("notes", venue.notes)
            })
        }
        prefs.edit().putString(KEY_CUSTOM_VENUES, array.toString()).apply()
    }

    private fun favouriteKeys(): Set<String> =
        prefs.getStringSet(KEY_FAVOURITES, emptySet()).orEmpty()

    private fun saveFavouriteKeys(keys: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVOURITES, keys.toSet()).apply()
    }
}
