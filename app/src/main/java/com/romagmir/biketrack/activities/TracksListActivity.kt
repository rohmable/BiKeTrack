package com.romagmir.biketrack.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseUser
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.ActivityTracksListBinding
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.ui.FirebaseUserActivity
import com.romagmir.biketrack.ui.RemoveTrackDialog
import com.romagmir.biketrack.ui.TracksAdapter
import com.romagmir.biketrack.utils.PreferencesSynchronizer
import com.romagmir.biketrack.viewModels.TracksModel
import java.util.*
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
    private lateinit var tracksModel: TracksModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTracksListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Creating adapter
        trackAdapter = TracksAdapter(this)
        trackAdapter.trackAdapterListener = trackAdapterListener
        binding.trackList.adapter = trackAdapter

        // Set up welcome card
        user?.let {
            binding.welcomeCard.userName = it.displayName.toString()
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val prefSynchronizer = PreferencesSynchronizer(this, it, preferences)
            prefSynchronizer.download()
            binding.welcomeCard.caloriesObjective = preferences.getString(getString(R.string.setting_weekly_calories), "0f")?.toFloat() ?: 0f
            binding.welcomeCard.hoursObjective = preferences.getString(getString(R.string.setting_weekly_hours), "0f")?.toFloat() ?: 0f
            binding.welcomeCard.showWeekly = preferences.getBoolean(getString(R.string.setting_enable_weekly), false)
        }

        // Settings menu
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener(menuItemListener)
    }

    override fun onUserChanged(
        property: KProperty<*>,
        oldValue: FirebaseUser?,
        newValue: FirebaseUser?
    ) {
        super.onUserChanged(property, oldValue, newValue)

        newValue?.let {
            if (this::tracksModel.isInitialized) {
                tracksModel.user = it
            } else {
                tracksModel = ViewModelProviders.of(this, TracksModel.TracksModelFactory(application, it))
                    .get(TracksModel::class.java)

                Log.d(TAG, "TracksModel created, now observing tracks")
                tracksModel.tracks.observe(this) { tracks -> updateTracks(tracks) }
            }
        }
    }

    /**
     * Updates the UI using the given track list.
     *
     * @param tracks Tracks to display
     */
    private fun updateTracks(tracks: List<Track>) {
        trackAdapter.tracks = tracks
        if (binding.welcomeCard.showWeekly) {
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            var hours = 0.0
            var calories = 0.0
            tracks.asSequence().filter { calendar.time = it.date; calendar.get(Calendar.WEEK_OF_YEAR) == week }.forEach { track ->
                hours += track.hoursLength
                calories += track.calories
            }
            binding.welcomeCard.hoursDone = hours.toFloat()
            binding.welcomeCard.caloriesDone = calories.toFloat()
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
            Log.d(TAG, "Opening $track")
            startActivity(openIntent)
        }

        override fun onDelete(track: Track) {
            Log.d(TAG, "Deleting $track")
            RemoveTrackDialog(onConfirm = {tracksModel.removeTrack(track)})
                .show(supportFragmentManager, CONFIRM_DIAG_TAG)
        }
    }

    private val menuItemListener = androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {
        when(it.itemId) {
            R.id.settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.id.logout -> {
                Log.d(TAG, "Logging out")
                tracksModel.tracks.postValue(arrayListOf())
                logOut()
            }
        }
        false
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