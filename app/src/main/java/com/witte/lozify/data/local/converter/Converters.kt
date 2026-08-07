package com.witte.lozify.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room type converters for handling Instant (java.time) objects.
 *
 * Room does not natively support java.time.Instant, so we convert:
 * - Instant → Long (epoch milliseconds) for database storage
 * - Long → Instant for in-memory usage
 *
 * These converters are registered in the Room database @TypeConverters annotation.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
