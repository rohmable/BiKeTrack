package com.romagmir.biketrack.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.romagmir.biketrack.R
import com.romagmir.biketrack.TrackRecorder
import com.romagmir.biketrack.databinding.ActivityRecordBinding
import com.romagmir.biketrack.ui.FirebaseUserActivity
import com.romagmir.biketrack.viewModels.RecordingModel
import kotlin.reflect.KProperty

/**
 * Used to record a [Track][com.romagmir.biketrack.model.Track]
 */
class RecordActivity : FirebaseUserActivity() {
    /** Flag used to check if the location permission was granted by the user */
    private var locationPermissionGranted = false
    /** [ViewBinding][androidx.viewbinding.ViewBinding] used to interact with the children views */
    private lateinit var binding: ActivityRecordBinding
    /** Recording [ViewModel][androidx.lifecycle.ViewModel] used to store data during the whole app lifecycle */
    private val recordingModel: RecordingModel by viewModels()

    /**
     * Perform initialization of all fragments.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Binding activity with his layout
        binding = ActivityRecordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Enable the "back arrow" on the toolbar to go back to the previous activity
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup activity default data
        binding.txtTime.text = getString(R.string.time_format, 0, 0, 0)
        binding.txtSpeed.text = getString(R.string.speed_format, 0f)
        binding.txtAltitude.text = getString(R.string.altitude_format, 0.0)
        binding.txtDistance.text = getString(R.string.distance_format, 0.0)


        // Register observers for UI update
        recordingModel.position.observe(this) { position ->
            binding.txtSpeed.text = getString(R.string.speed_format, position.speed)
            binding.txtAltitude.text = getString(R.string.altitude_format, position.altitude)
        }
        recordingModel.distance.observe(this) { distance ->
            binding.txtDistance.text = getString(R.string.distance_format, distance / 1000)
        }
        recordingModel.recordingTime.observe(this) { time ->
            val timeSec = time / 1000
            val hours = timeSec / 3600
            val minutes = (timeSec - (hours * 3600)) / 60
            val seconds = (timeSec - (hours * 3600) - (minutes * 60))
            binding.txtTime.text = getString(R.string.time_format, hours, minutes, seconds)
        }

        // Set correct icon on the FAB button
        val resource = if(recordingModel.running) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24
        binding.fabRecord.setImageResource(resource)
    }

    override fun onResume() {
        super.onResume()
        recordingModel.addTrackRecorderListener(recorderListener)
    }

    override fun onPause() {
        super.onPause()
        recordingModel.removeTrackRecorderListener()
    }

    /**
     * Called when the user has changed.
     *
     * The value of the user has already been changed when this callback is invoked.
     *
     * @param property Property that called the method.
     * @param oldValue Previous user.
     * @param newValue New user.
     */
    override fun onUserChanged(
        property: KProperty<*>,
        oldValue: FirebaseUser?,
        newValue: FirebaseUser?
    ) {
        super.onUserChanged(property, oldValue, newValue)

        newValue?.let {
            recordingModel.user = it
        }
    }

    /**
     * onClick event handler for the FAB fabRecord.
     *
     * This handler is connected only to the fabRecord button.
     *
     * @param src View that generated the onClick event
     */
    fun onRecordClick(src: View) {
        // Relay call to specific handler
        if (recordingModel.running) stopTrack()
        else startTrack()
    }

    /**
     * Handles the start of a new recording and updates the UI accordingly.
     *
     * Checks if the location permission is granted before trying to start the recording, this is
     * the only check that is made.
     *
     * @param src FAB that generated the event
     */
    private fun startTrack() {
        Log.v(TAG, "Start recording")

        if (!locationPermissionGranted) {
            getLocationPermission()
        }
        recordingModel.startRecording()
    }

    /**
     * Handles the stop of a new recording and updates the UI accordingly.
     *
     * @param src FAB that generated the event
     */
    private fun stopTrack() {
        Log.v(TAG, "Stop recording")
        recordingModel.stopRecording()
    }

    private val recorderListener = object : TrackRecorder.TrackRecorderListener {
        /**
         * Called when the recording has started.
         *
         * This does not imply that any data has been received from the [locationClient]
         */
        override fun onRecordingStart() {
            // Set recording preferences
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@RecordActivity)
            val deviceAwake = sharedPreferences.getBoolean(getString(R.string.setting_awake), true)
            binding.recordRootLayout.keepScreenOn = deviceAwake
            // Show progress bar
            binding.progressBar.visibility = View.VISIBLE
        }

        /**
         * Called when the recording has ended.
         *
         * This means that no new data will be added to the actual track.
         */
        override fun onRecordingEnd() {
            // UI adjustments
            binding.recordRootLayout.keepScreenOn = false
            binding.fabRecord.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            binding.fabRecord.contentDescription = getString(R.string.start_recording)
            Toast.makeText(this@RecordActivity, R.string.recording_saved, Toast.LENGTH_SHORT).show()
        }

        /**
         * Called when the recorder receives the first location data.
         *
         * From now on the track will be updated with new data.
         */
        override fun onDataReceived() {
            // Hide progress bar
            binding.progressBar.visibility = View.INVISIBLE
            // UI adjustments
            binding.fabRecord.setImageResource(R.drawable.ic_baseline_stop_24)
            binding.fabRecord.contentDescription = getString(R.string.stop_recording)
            Toast.makeText(this@RecordActivity, R.string.recording_started, Toast.LENGTH_SHORT).show()
        }

    }

    /**
     * Tries to obtain the location permission.
     * If the permission was not already granted then it asks it to the user.
     */
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [requestPermissions].
     *
     * @param requestCode The request code passed in [requestPermissions].
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either [android.content.pm.PackageManager.PERMISSION_GRANTED]
     *     or [android.content.pm.PackageManager.PERMISSION_GRANTED].
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        // Handling location permission request
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION ->
                locationPermissionGranted = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    companion object {
        /** Log tag */
        private val TAG = RecordActivity::class.java.simpleName
        /** Request code for login activity */
        private const val RC_SIGN_IN = 124
        /** Request code for location permissions */
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }
}