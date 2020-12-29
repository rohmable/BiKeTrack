package com.romagmir.biketrack.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.utils.addAll
import com.romagmir.biketrack.utils.clear

/**
 * Handles the saved track list for a given user.
 *
 */
class TracksModel(context: Application, user: FirebaseUser) : AndroidViewModel(context) {
    /** Database reference */
    private var database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("tracks").child(user.uid)
    /** Database user used to access the track list */
    var user: FirebaseUser = user
    set(value) {
        field = value
        Log.d(TAG, "Now observing user: ${user.displayName}")
        database.removeEventListener(trackListener)
        database = FirebaseDatabase.getInstance().reference.child("tracks").child(user.uid)
        database.addValueEventListener(trackListener)
    }

    /** Stores the users track list */
    val tracks: MutableLiveData<ArrayList<Track>> by lazy {
        MutableLiveData<ArrayList<Track>>()
    }

    init {
        // The Firebase database persistence option must be enabled before any usage of the database
        // functionalities, for this reason the [database] reference is assigned in the default constructor
        // after checking if this is the first time that we enable the persistence
        if (FirebaseApp.getApps(getApplication()).isEmpty()) {
            Firebase.database.setPersistenceEnabled(true)
        }
    }

    /**
     * Removes the given track from the remote database.
     *
     * The track and his route are removed.
     *
     * Two checks are made before the deletion:
     * * The user is not null.
     * * The track is contained in the [tracks] list.
     *
     * @param track Track to remove.
     */
    fun removeTrack(track: Track) {
        tracks.value?.let {
            if (it.contains(track)) {
                val trackRef = database.child(track.key)
                val posRef = FirebaseDatabase.getInstance().reference.child("positions").child(user.uid).child(track.key)
                Log.d(TAG, "Removing track: $track")
                trackRef.removeValue()
                posRef.removeValue()
            } else {
                Log.d(TAG, "Couldn't remove track path: ${track.key}")
            }
        }
    }

    /**
     *  Database listener, reads the tracks for the given user.
     */
    private val trackListener = object : ValueEventListener {
        init {
            database.addValueEventListener(this)
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG, "New track list loaded")
            val newTracks = snapshot.children.mapNotNull {
                val track = it.getValue<Track>()
                track?.key = it.key ?: ""
                track
            }
            tracks.clear()
            tracks.addAll(newTracks)
        }

        override fun onCancelled(error: DatabaseError) { }
    }

    /**
     * Factory pattern used to instantiate a TracksModel ViewModel
     *
     * @property app Context
     * @property user User that wants to connect to the database
     */
    class TracksModelFactory(val app: Application, private val user: FirebaseUser) : ViewModelProvider.Factory {
        /**
         * Creates a new instance of the TracksModel.
         *
         * @param modelClass a `Class` whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created TracksModel
        </T> */
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TracksModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TracksModel(app, user) as T
            }
            throw IllegalArgumentException("Unable to construct viewModel")
        }

    }

    companion object {
        /** Log tag */
        private val TAG = TracksModel::class.java.simpleName
    }
}