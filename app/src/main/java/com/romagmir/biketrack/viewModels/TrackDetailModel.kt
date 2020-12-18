package com.romagmir.biketrack.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.romagmir.biketrack.model.Position
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.utils.notifyObserver

/**
 * Handles the details of a given track.
 *
 * Gathering position data for every track from the database is useless if just an overview of the
 * track is needed, for this reason the data is split between [tracks][Track] and his [positions][Position].
 * A [Track] record maintains just an overview of the data, the details relative to his route are kept
 * separate.
 * When an activity needs the details of the track these are gathered by this [ViewModel][androidx.lifecycle.ViewModel]
 */
class TrackDetailModel(context: Application) : AndroidViewModel(context)  {
    /** Database reference */
    private var database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("positions")
    /** Database user used to access the track details, must match the user that recorded the track */
    var user: FirebaseUser? = null
    set(value) {
        // Remove old event listeners in case of subsequent calls
        database.removeEventListener(trackDetailListener)
        field = value
        field?.let {
            // Sets the database reference on the new user
            database = FirebaseDatabase.getInstance().reference.child("positions").child(it.uid)
        } ?: run {
            database = FirebaseDatabase.getInstance().reference.child("positions")
        }
    }

    /** Stores the track selected with [getDetails] */
    val track: MutableLiveData<Track> by lazy {
        MutableLiveData<Track>()
    }

    /**
     * Retrieves the details for the given track.
     *
     * The track will be set as the new [track] and the details will be put inside it, the observers
     * will be notified each time new data will be available.
     *
     * @param track Track to retrieve the details from
     */
    fun getDetails(track: Track) {
        // Remove previous event listeners
        database.removeEventListener(trackDetailListener)
        // Point the database reference to the selected track
        this.track.postValue(track)
        database = database.child(track.key)
        // Read values
        database.addValueEventListener(trackDetailListener)
    }

    /**
     * Database listener, reads the positions array.
     */
    private val trackDetailListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            snapshot.children.forEach {
                it.getValue<Position>()?.let { pos -> track.value?.positions?.add(pos) }
            }
            track.notifyObserver()
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }
}