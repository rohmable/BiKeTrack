package com.romagmir.biketrack.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
 *
 * @property user  Used to access the track details, must match the user that recorded the track
 */
class TrackDetailModel(context: Application, val user: FirebaseUser) : AndroidViewModel(context)  {
    /** Database reference */
    private var database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("positions").child(user.uid)
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
            val posList = snapshot.children.mapNotNull { it.getValue<Position>() }
            track.value?.positions?.addAll(posList)
            track.notifyObserver()
        }

        override fun onCancelled(error: DatabaseError) { }
    }

    /**
     * Factory pattern used to instantiate a TrackDetailModel ViewModel
     *
     * @property app Context
     * @property user User that wants to connect to the database
     */
    class TrackDetailModelFactory(val app: Application, private val user: FirebaseUser) : ViewModelProvider.Factory {
        /**
         * Creates a new instance of the TrackDetailModel.
         *
         * @param modelClass a `Class` whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created TracksModel
        </T> */
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TrackDetailModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TrackDetailModel(app, user) as T
            }
            throw IllegalArgumentException("Unable to construct viewModel")
        }

    }
}