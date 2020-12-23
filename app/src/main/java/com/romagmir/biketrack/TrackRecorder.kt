package com.romagmir.biketrack

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
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
    /** Contains the last location returned by the [locationClient] */
    private var lastKnownLocation: Location? = null
    /** Handler used to keep track of the route time */
    private val timerHandler = Handler(Looper.getMainLooper())
    /** Stores the most recent position tracked by the recorder */
    val position: MutableLiveData<Position> by lazy { MutableLiveData<Position>() }
    /** Stores the total distance of the track */
    val distance: MutableLiveData<Double> by lazy { MutableLiveData<Double>(0.0) }
    /** Stores the total recording time of the track */
    val recordingTime: MutableLiveData<Long> by lazy { MutableLiveData<Long>(0) }

    var recorderListener: TrackRecorderListener? = null

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
        distance.postValue(0.0)
        recordingTime.postValue(0L)
        lastKnownLocation = null

        // Create new track and bind the locationCallback to the locationClient
        actualTrack = Track(name=trackName)
        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        timerHandler.postDelayed(timerRunnable, TIMER_PERIOD)
        running = true
        recorderListener?.onRecordingStart()
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
            timerHandler.removeCallbacks(timerRunnable)
            running = false
            recorderListener?.onRecordingEnd()
        }
        return actualTrack
    }

    /**
     * Increments the track length by a definite amount of time in milliseconds.
     */
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (lastKnownLocation != null) {
                actualTrack.length += TIMER_PERIOD
                recordingTime.postValue(actualTrack.length)
            }
            timerHandler.postDelayed(this, TIMER_PERIOD)
        }
    }

    /**
     * Callback used to gather the location data.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (!running) return
            // Location data can come in batches
            locationResult.locations.asSequence().filter { it.accuracy <= 20 }.forEach { location ->
                val speed = location.speed.toDouble() * 3.6

                // Create new position from the updated location
                val newPosition = Position(
                    timestamp=location.time,
                    latitude=location.latitude,
                    longitude=location.longitude,
                    altitude=location.altitude,
                    speed=speed
                )

                // Compute data based on the previous location (if exists)
                val newDistance = lastKnownLocation?.let {
                    location.distanceTo(it).toDouble()
                } ?: run {
                    recorderListener?.onDataReceived()
                    0.0
                }

                // Update the recorded track
                actualTrack.positions.add(newPosition)
                actualTrack.distance += newDistance
                newPosition.distance = actualTrack.distance
                lastKnownLocation = location

                // Publish track properties changes
                position.postValue(newPosition)
                distance.postValue(actualTrack.distance)
            }
        }
    }

    /**
     * Listener interface to handle the various events triggered by the recorder.
     */
    interface TrackRecorderListener {
        /**
         * Called when the recording has started.
         *
         * This does not imply that any data has been received from the [locationClient]
         */
        fun onRecordingStart()

        /**
         * Called when the recording has ended.
         *
         * This means that no new data will be added to the actual track.
         */
        fun onRecordingEnd()

        /**
         * Called when the recorder receives the first location data.
         *
         * From now on the track will be updated with new data.
         */
        fun onDataReceived()
    }

    /** @suppress */
    companion object {
        /** Timer period in milliseconds */
        private const val TIMER_PERIOD = 250L
    }
}