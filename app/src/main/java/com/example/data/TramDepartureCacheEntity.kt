package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tram_departure_cache",
    indices = [Index(value = ["fetchedAt"])]
)
data class TramDepartureCacheEntity(
    @PrimaryKey val stopName: String,
    val payloadJson: String,
    val fetchedAt: Long
)
