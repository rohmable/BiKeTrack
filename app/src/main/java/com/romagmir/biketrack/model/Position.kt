package com.romagmir.biketrack.model

import android.location.Location
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Dataclass that represent a position in space and time.
 *
 * @constructor Creates a position where all the values are zero.
 */
@Parcelize
data class Position (
        /** Instant in time in milliseconds. */
        val timestamp: Long = 0,
        /** Latitude coordinate. */
        val latitude: Double = 0.0,
        /** Longitude coordinate. */
        val longitude: Double = 0.0,
        /** Altitude in meters. */
        val altitude: Double = 0.0,
        /** Speed in Km/h */
        val speed: Float = 0f
) : Parcelable {
    /**
     * Calculates the distance in meters from this position to the one given as parameter.
     *
     * @param to Position to calculate the distance to.
     * @return The distance from the given position in meters.
     */
    fun distance(to: Position): Float {
        return distance(this, to)
    }

    /** @suppress */
    companion object {
        /**
         * Calculates the distance in meters from the positions given as parameter.
         *
         * @param from Starting position.
         * @param to Arrival position.
         * @return Distance between the two positions in meters.
         */
        fun distance(from: Position, to: Position): Float {
            val results = FloatArray(1)
            Location.distanceBetween(
                    from.latitude,
                    from.longitude,
                    to.latitude,
                    to.longitude,
                    results)
            return results[0]
        }
    }
}