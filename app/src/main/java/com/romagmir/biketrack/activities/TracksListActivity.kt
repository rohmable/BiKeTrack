package com.romagmir.biketrack.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.ActivityTracksListBinding
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.ui.TracksAdapter
import com.romagmir.biketrack.viewModels.TracksModel

/**
 * Shows a list of [tracks][Track] saved by an user.
 */
class TracksListActivity : AppCompatActivity() {
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
        trackAdapter = TracksAdapter(this,
            onOpen = { track -> adapterOnOpen(track)},
            onDelete = {track -> adapterOnDelete(track)}
        )
        binding.trackList.adapter = trackAdapter

        // If the user is already logged in the the data is retrieved, otherwise the app tries to
        // log the user
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let {
            tracksModel.user = it
            tracksModel.tracks.observe(this) {tracks ->
                trackAdapter.tracks = tracks
            }
        } ?: run {
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
            )
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setLogo(R.mipmap.ic_launcher_round)
                    .setTheme(R.style.Theme_BiKeTrack)
                    .build(),
                RC_SIGN_IN
            )
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Handling login result
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                tracksModel.user = FirebaseAuth.getInstance().currentUser
                tracksModel.tracks.observe(this) {tracks ->
                    trackAdapter.tracks = tracks
                }
            } else {
                Log.d(TAG, "Log-in failed, cause: ${response?.error?.errorCode}")
            }
        }
    }

    /**
     * Starts a record activity when the [FAB][com.google.android.material.floatingactionbutton.FloatingActionButton]
     * is clicked.
     *
     * @param src Event source
     */
    fun addRecordingClicked(src: View) {
        val recordIntent = Intent(this, RecordActivity::class.java)
        startActivity(recordIntent)
    }

    /**
     * Handles the onDelete signal of the [TracksAdapter].
     *
     * @param track Track associated with the card shown.
     */
    private fun adapterOnOpen(track: Track) {
        val openIntent = Intent(this, TrackDetailActivity::class.java)
        openIntent.putExtra(TRACK, track)
        startActivity(openIntent)
        Log.d(TAG, "Opening $track")
    }

    /**
     * Handles the onDelete signal of the [TracksAdapter].
     *
     * @param track Track associated with the card shown.
     */
    private fun adapterOnDelete(track: Track) {
        Log.d(TAG, "Deleting $track")
        TODO("Implement track deletion")
    }

    companion object {
        private val TAG = TracksListActivity::class.java.simpleName
        private const val RC_SIGN_IN = 123
        const val TRACK = "TRACK"
    }
}