package com.romagmir.biketrack.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import com.google.firebase.auth.FirebaseUser
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.ActivityTracksListBinding
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.ui.FirebaseUserActivity
import com.romagmir.biketrack.ui.RemoveTrackDialog
import com.romagmir.biketrack.ui.TracksAdapter
import com.romagmir.biketrack.viewModels.TracksModel
import kotlin.reflect.KProperty

/**
 * Shows a list of [tracks][Track] saved by an user.
 */
class TracksListActivity : FirebaseUserActivity() {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] used to interact with the children views */
    private lateinit var binding: ActivityTracksListBinding
    /** For showing each track inside a view */
    private lateinit var trackAdapter: TracksAdapter
    /** [ViewModel][androidx.lifecycle.ViewModel] used to store data during the whole app lifecycle */
    private val tracksModel: TracksModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTracksListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Creating adapter
        trackAdapter = TracksAdapter(this)
        trackAdapter.trackAdapterListener = trackAdapterListener
        binding.trackList.adapter = trackAdapter
        tracksModel.tracks.observe(this) { tracks ->
            trackAdapter.tracks = tracks
        }

        // Settings menu
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.settings) {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            false
        }
    }

    override fun onUserChanged(
        property: KProperty<*>,
        oldValue: FirebaseUser?,
        newValue: FirebaseUser?
    ) {
        super.onUserChanged(property, oldValue, newValue)

        newValue.let {
            tracksModel.user = it
        }
    }

    /**
     * Starts a record activity when the [FAB][com.google.android.material.floatingactionbutton.FloatingActionButton]
     * is clicked.
     *
     * @param src Event source
     *
     */
    fun addRecordingClicked(src: View) {
        val recordIntent = Intent(this, RecordActivity::class.java)
        startActivity(recordIntent)
    }

    private val trackAdapterListener = object : TracksAdapter.TrackAdapterListener {
        override fun onOpen(track: Track) {
            val openIntent = Intent(this@TracksListActivity, TrackDetailActivity::class.java)
            openIntent.putExtra(TRACK_KEY, track)
            startActivity(openIntent)
            Log.d(TAG, "Opening $track")
        }

        override fun onDelete(track: Track) {
            Log.d(TAG, "Deleting $track")
            RemoveTrackDialog(onConfirm = {tracksModel.removeTrack(track)})
                .show(supportFragmentManager, CONFIRM_DIAG_TAG)
        }
    }

    companion object {
        /** Log tag */
        private val TAG = TracksListActivity::class.java.simpleName
        /** Key to retrieve the track contained in an intent */
        const val TRACK_KEY = "TRACK"
        /** Tag used to show the [RemoveTrackDialog] dialog */
        private const val CONFIRM_DIAG_TAG = "CONFIRM_DIAG"
    }
}