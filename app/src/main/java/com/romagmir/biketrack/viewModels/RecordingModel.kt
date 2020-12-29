package com.romagmir.biketrack.viewModels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.romagmir.biketrack.R
import com.romagmir.biketrack.TrackRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/**
 * Handles the recording and the database writing of the tracks.
 *
 * @property user Used to memorize the track recordings.
 */
class RecordingModel(context: Application, var user: FirebaseUser) : AndroidViewModel(context) {
    /** Used to record the tracks. */
    private val trackRecorder = TrackRecorder(getApplication<Application>().applicationContext)
    /** @see TrackRecorder.position */
    val position = trackRecorder.position
    /** @see TrackRecorder.distance */
    val distance = trackRecorder.distance
    /** @see TrackRecorder.recordingTime */
    val recordingTime = trackRecorder.recordingTime
    /** @see TrackRecorder.running */
    val running
    get() = trackRecorder.running

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val recordingResolution = sharedPreferences.getInt(
            context.getString(R.string.setting_resolution),
            6
        )
        trackRecorder.recordingResolution = recordingResolution
    }

    override fun onCleared() {
        super.onCleared()
        // Stop the actual recording (if any)
        stopRecording()
    }

    /**
     *  Starts a new recording with a default name based on the time of day.
     *
     *  If there is already a track being recorded then the operation fails.
     *
     *  @return True if the recording was started, False otherwise
     *  @see genTrackName
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (running) return false
        trackRecorder.start(genTrackName())
        return true
    }

    /**
     * Stops a recording.
     *
     * If there is no track being recorded then nothing is done.
     */
    fun stopRecording() {
        if (!running) return
        val track = trackRecorder.stop()
        // Retrieve user info for watts calculation
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
        val height = sharedPreferences.getInt(
            getApplication<Application>().getString(R.string.setting_height),
            180
        ).toDouble()
        val weight = sharedPreferences.getInt(
            getApplication<Application>().getString(R.string.setting_weight),
            80
        ).toDouble()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val tracksDb = FirebaseDatabase.getInstance().reference.child("tracks").child(user.uid)
                val newVal = tracksDb.push()

                // Calculate watts
                track.watts = track.calcWatts(weight, height)

                // Write data to database
                track.write(newVal)
                newVal.key?.let {key ->
                    // Write positions in a separate location
                    val positionsDb = FirebaseDatabase.getInstance().reference.child("positions").child(user.uid).child(key)
                    val positionsMap = track.positions.mapIndexed {idx, pos -> idx.toString() to pos}.toMap()
                    positionsDb.updateChildren(positionsMap)
                }
            }
        }
    }

    fun addTrackRecorderListener(recorder: TrackRecorder.TrackRecorderListener) {
        trackRecorder.recorderListener = recorder
    }

    fun removeTrackRecorderListener() {
        trackRecorder.recorderListener = null
    }

    /**
     * Generates a new track name based on the time of day.
     *
     * The new name is localized.
     *
     * @return A new localized track name
     */
    private fun genTrackName() : String {
        val application: Application = getApplication()
        val currentTime = ZonedDateTime.now()
        when (currentTime.hour) {
            in 0..7 -> {
                return application.getString(R.string.night_track)
            }
            in 8..11 -> {
                return application.getString(R.string.morning_track)
            }
            in 12..17 -> {
                return application.getString(R.string.afternoon_track)
            }
            in 18..23 -> {
                return application.getString(R.string.evening_track)
            }
            else -> {
                return ""
            }
        }
    }

    /**
     * Factory pattern used to instantiate a RecordingModel ViewModel
     *
     * @property app Context
     * @property user User that wants to connect to the database
     */
    class RecordingModelFactory(val app: Application, private val user: FirebaseUser) : ViewModelProvider.Factory {
        /**
         * Creates a new instance of the RecordingModel.
         *
         * @param modelClass a `Class` whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created TracksModel
        </T> */
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecordingModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RecordingModel(app, user) as T
            }
            throw IllegalArgumentException("Unable to construct viewModel")
        }

    }

    companion object {
        /** Log tag */
        private val TAG = RecordingModel::class.java.simpleName
    }
}