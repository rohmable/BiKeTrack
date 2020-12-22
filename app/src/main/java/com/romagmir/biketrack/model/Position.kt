package com.romagmir.biketrack.model

import android.location.Location
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

/**
 * Dataclass that represent a position in space and time.
 *
 * @constructor Creates a position where all the values are zero.
 */
@Parcelize
data class Position (
        /** Instant in time in milliseconds. */
        var timestamp: Long = 0,
        /** Distance in meters of the position inside a route */
        var distance: Double = 0.0,
        /** Latitude coordinate. */
        var latitude: Double = 0.0,
        /** Longitude coordinate. */
        var longitude: Double = 0.0,
        /** Altitude in meters. */
        var altitude: Double = 0.0,
        /** Speed in Km/h */
        var speed: Double = 0.0
) : Parcelable {
    /**
     * Calculates the distance in meters from this position to the one given as parameter.
     *
     * @param to Position to calculate the distance to.
     * @return The distance from the given position in meters.
     */
    fun distance(to: Position): Double = distance(this, to)

    /**
     * Returns the position as a [LatLng] object with his latitude and longitude.
     */
    fun toLatLng() = LatLng(latitude, longitude)

    /** @suppress */
    companion object {
        /**
         * Calculates the distance in meters from the positions given as parameter.
         *
         * @param from Starting position.
         * @param to Arrival position.
         * @return Distance between the two positions in meters.
         */
        fun distance(from: Position, to: Position): Double {
            val results = FloatArray(1)
            Location.distanceBetween(
                    from.latitude,
                    from.longitude,
                    to.latitude,
                    to.longitude,
                    results)
            return results[0].toDouble()
        }
    }
}