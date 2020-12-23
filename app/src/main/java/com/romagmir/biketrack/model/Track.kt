package com.romagmir.biketrack.model

import android.os.Parcelable
import com.google.firebase.database.DatabaseReference
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Dataclass that represents a recorded track.
 *
 * @constructor Creates an empty track.
 */
@Parcelize
data class Track (
    /** Unique identifier used in Firebase database. */
    var key: String = "",
    /** Track name chosen by the user. */
    var name: String = "",
    /** Track distance in meters. */
    var distance: Double = 0.0,
    /** Track date. */
    var date: Date = Date(),
    /** Track length in milliseconds. */
    var length: Long = 0L,
    /** Estimated power produced with the values of body mass and height given when the track was recorded */
    var watts: Double = 0.0,
    /** Track readings. */
    var positions: ArrayList<Position> = ArrayList()
) : Parcelable {
    /** Average speed in Km/h. */
    val avgSpeed: Double
    get() = (distance / length) * 3600
    /** Track minimum altitude in meters. */
    val minAltitude: Double
    get() = positions.minByOrNull { it.altitude }?.altitude ?: 0.0
    /** Track maximum altitude in meters. */
    val maxAltitude: Double
    get() = positions.maxByOrNull { it.altitude }?.altitude ?: 0.0
    /** Meters uphill during the track. */
    val elevationGain: Double
    get() = calcElevationGain()
    /** Track length in hours */
    val hoursLength
    get() = length.toDouble() / (3600 * 1000)
    /** Calories burned on the track, estimated from the average watts */
    val calories
    get() = (watts * hoursLength * 3.6).roundToInt()

    /**
     *  Calculates the meters of the track spent uphill
     *
     *  @return meters of track spent uphill
     */
    private fun calcElevationGain(): Double {
        var gain = 0.0
        for ((start, end) in positions.zipWithNext()) {
            if (start.altitude < end.altitude) gain += end.altitude - start.altitude
        }
        return gain
    }

    /**
     * Calculates the estimated power that the cyclist has produced riding the track
     *
     * The formula is taken from the [Strava website](https://support.strava.com/hc/en-us/articles/216917107-How-Strava-Calculates-Power).
     *
     * The total power is made up of several components:
     *  * Power needed to overcome the rolling resistance
     *  * Power needed to overcome wind resistance
     *  * Power needed to overcome gravity (when climbing)
     *  * Power needed to accelerate
     *
     *
     * Weight and height parameters are used to give an estimate of the frontal area of the rider.
     *
     *
     * @param weight Weight of the rider in kilograms
     * @param height Height of the rider in centimeters
     * @return The power produced by the rider in Watts
     */
    fun calcWatts(weight: Double, height: Double): Double {
        // From Heil DP. (European Journal of Applied Physiology, 85:358-366)
        val frontalArea = 0.00433 * SEAT_TUBE_ANGLE.pow(0.183f) * TORSO_ANGLE.pow(0.099f) * weight.pow(0.493) * (height / 100).pow(1.163)

        // Separate components
        var rolling = 0.0
        var wind = 0.0
        var gravity = 0.0
        var acceleration = 0.0

        positions.zipWithNext().forEach { (start, end) ->
            val speedAvg = (start.speed + end.speed) / 2
            var slope = (end.altitude - start.altitude) / (end.distance(start))
            slope = if (!slope.isNaN()) slope else 0.0

            rolling += ROLLING_COEFFICIENT * weight * speedAvg

            wind += 0.5 * AIR_DENSITY * speedAvg.pow(3) * DRAG_COEFFICIENT * frontalArea

            gravity += weight * 9.8 * sin(atan(slope))

            acceleration += weight * ((end.speed - start.speed) / (end.timestamp - start.timestamp))
        }

        val totWatts = rolling + wind + gravity + acceleration

        return totWatts / (length / 1000)
    }

    /**
     * Convenience method used to save the significant values on a [DatabaseReference](https://firebase.google.com/docs/reference/android/com/google/firebase/database/DatabaseReference)
     *
     * @param ref [DatabaseReference](https://firebase.google.com/docs/reference/android/com/google/firebase/database/DatabaseReference) to store the track.
     */
    fun write(ref: DatabaseReference) {
        val values = mapOf(
            "name" to name,
            "distance" to distance,
            "date" to date,
            "length" to length,
            "watts" to watts
        )
        ref.updateChildren(values)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "$name - $date"
    }

    /**
     * Indicates whether some other track is "equal to" this one.
     *
     * For two objects to be equal the following requirements must be met:
     * * The other object must be a Track
     * * The following properties must be equal:
     *      * Name
     *      * Distance
     *      * Date
     *      * Length
     *
     * @param other The other object to compare.
     * @return True if the two objects are equal, false otherwise.
     */
    override fun equals(other: Any?): Boolean
        =   (other is Track)
            && name == other.name
            && distance == other.distance
            && date == other.date
            && length == other.length

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + length.hashCode()
        return result
    }

    /** @suppress */
    companion object {
        /** Rolling coefficient taken from [Engineering Toolbox](https://www.engineeringtoolbox.com/rolling-friction-resistance-d_1303.html#Rolling). */
        private const val ROLLING_COEFFICIENT = 0.002
        /** Drag coefficient taken from [Engineering Toolbox](https://www.engineeringtoolbox.com/drag-coefficient-d_627.html). */
        private const val DRAG_COEFFICIENT = 0.9
        /** Dry air density at 15°C taken from [Wikipedia](https://en.wikipedia.org/wiki/Density_of_air#Dry_air). */
        private const val AIR_DENSITY = 1.2250
        /** Bike seat tube tilt in degrees */
        private const val SEAT_TUBE_ANGLE = 75f
        /** Cyclist torso angle in degrees */
        private const val TORSO_ANGLE = 15f
    }
}