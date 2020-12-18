package com.romagmir.biketrack

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.romagmir.biketrack.model.Position
import com.romagmir.biketrack.model.Track

/**
 * Class used to record a new track.
 *
 * It handles the location data gathering and signals the update of some useful variable
 * that the [RecordActivity][com.romagmir.biketrack.activities.RecordActivity] should show to the user.
 *
 * The gathering is made by registering a [LocationCallback][com.google.android.gms.location.LocationCallback]
 * to the [FusedLocationProviderClient][com.google.android.gms.location.FusedLocationProviderClient]
 * this callback will then receive updated location data every time this is available.
 *
 * @property recordingResolution Number of location data points gathered per second
 */
class TrackRecorder(context: Context, var recordingResolution: Int = 1) {
    /** Used to access the device geolocation capabilities */
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    /** Stores quality of service preferences for the [locationClient] */
    private val locationRequest = LocationRequest()
    /** Signals if the data gathering is running or not (true if running) */
    var running = false
    /** Contains the recorded [Track] */
    private var actualTrack = Track()
    /** Contains the last position returned by the [locationClient] */
    private var lastKnownPosition: Position? = null

    /** Stores the most recent position tracked by the recorder */
    val position: MutableLiveData<Position> by lazy {
        MutableLiveData<Position>()
    }

    /** Stores the total distance of the track */
    val distance: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>(0f)
    }

    /** Stores the total recording time of the track */
    val recordingTime: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>(0)
    }

    init {
        // Setting up the quality of service parameters
        locationRequest.interval = (1000 / recordingResolution).toLong()
        locationRequest.maxWaitTime = 500L
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Starts the recording of a new track.
     *
     * @param trackName The name of the new track
     * @return True if the new recording started, false otherwise
     */
    @SuppressLint("MissingPermission")
    fun start(trackName: String): Boolean {
        if (running) return false
        // Resetting previous data
        position.postValue(Position())
        distance.postValue(0f)
        recordingTime.postValue(0L)
        lastKnownPosition = null

        // Create new track and bind the locationCallback to the locationClient
        actualTrack = Track(name=trackName)
        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        running = true
        return true
    }

    /**
     * Stops the current recording.
     *
     * @return The recorded track
     */
    fun stop() : Track {
        if (running) {
            locationClient.removeLocationUpdates(locationCallback)
            running = false
        }
        return actualTrack
    }


    /**
     * Callback used to gather the location data.
     */
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (!running) return
            // Location data can come in batches
            for (location in locationResult.locations) {
                // Create new position from the updated location
                val newPosition = Position(
                    latitude=location.latitude,
                    longitude=location.longitude,
                    timestamp=location.time,
                    altitude=location.altitude,
                    speed=location.speed
                )

                // Compute data based on the previous location (if exists)
                var newDistance = 0f
                var timePassed = 0L
                lastKnownPosition?.let {
                    newDistance = it.distance(newPosition)
                    timePassed = newPosition.timestamp - it.timestamp
                }

                // Update the recorded track
                actualTrack.positions.add(newPosition)
                actualTrack.distance += newDistance
                actualTrack.length += timePassed
                lastKnownPosition = newPosition

                // Publish track properties changes
                position.postValue(newPosition)
                distance.postValue(actualTrack.distance)
                recordingTime.postValue(actualTrack.length)
            }
        }
    }
}