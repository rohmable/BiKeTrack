package com.romagmir.biketrack.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
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
 */
class TracksModel(context: Application) : AndroidViewModel(context) {
    /** Database reference */
    private var database: DatabaseReference
    /** Database user used to access the track list */
    var user: FirebaseUser? = null
    set(value) {
        // Remove old event listeners in case of subsequent calls
        database.removeEventListener(trackListener)
        field = value
        field?.let {
            // Set the database reference to the new user and start to listen for the values
            database = FirebaseDatabase.getInstance().reference.child("tracks").child(it.uid)
            database.addValueEventListener(trackListener)
        }?: run {
            database = FirebaseDatabase.getInstance().reference.child("tracks")
        }
    }

    /** Stores the users track list */
    val tracks: MutableLiveData<ArrayList<Track>> by lazy {
        MutableLiveData<ArrayList<Track>>()
    }

    init {
        // The Firebase database persistence option must be enabled before any usage of the database
        // functionalities, for this reason the [database] reference is assigned in the default constructor
        Firebase.database.setPersistenceEnabled(true)
        database = FirebaseDatabase.getInstance().reference.child("tracks")
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
            if (user != null && it.contains(track)) {
                val trackRef = database.child(track.key)
                val posRef = FirebaseDatabase.getInstance().reference.child("positions").child(user!!.uid).child(track.key)
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
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG, "New track list loaded")
            val newTracks = ArrayList<Track>()
            snapshot.children.forEach {
                it.getValue<Track>()?.let { track ->
                    track.key = it.key ?: ""
                    newTracks.add(track)
                }
            }
            tracks.clear()
            tracks.addAll(newTracks)
        }

        override fun onCancelled(error: DatabaseError) { }
    }

    companion object {
        /** Log tag */
        private val TAG = TracksModel::class.java.simpleName
    }
}