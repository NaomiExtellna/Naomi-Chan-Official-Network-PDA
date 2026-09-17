package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TramCacheDao {
    @Query("SELECT * FROM tram_departure_cache WHERE stopName = :stopName LIMIT 1")
    suspend fun getByStop(stopName: String): TramDepartureCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TramDepartureCacheEntity)

    @Query("DELETE FROM tram_departure_cache WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
