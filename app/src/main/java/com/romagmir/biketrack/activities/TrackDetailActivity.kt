package com.romagmir.biketrack.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.ActivityTrackDetailBinding
import com.romagmir.biketrack.model.Position
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.viewModels.TrackDetailModel
import kotlin.math.ln

/**
 * Shows the details of a [Track].
 */
class TrackDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] used to interact with the children views */
    private lateinit var binding: ActivityTrackDetailBinding
    /** [ViewModel][androidx.lifecycle.ViewModel] used to store data during the whole app lifecycle */
    private val trackDetailModel: TrackDetailModel by viewModels()
    /** Map that is shown on the top of the activity */
    private lateinit var mMap: GoogleMap
    /** [Track] that is shown */
    private var track: Track? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        track = intent.extras?.getParcelable(TracksListActivity.TRACK)

        // If the user is already logged in then the data is retrieved via [updateTrackData]
        // otherwise the app tries to log the user
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let {currentUser ->
            trackDetailModel.user = currentUser
            updateTrackData()
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
    }

    /**
     * Called when the map is available.
     *
     * Here all the map operations are made.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        trackDetailModel.track.observe(this) {
            drawLine(it)
        }
    }

    /**
     * Update the activity UI with the given [Track].
     *
     * The only view that is not updated is the map, this is made inside the [onMapReady] method.
     */
    private fun updateTrackData() {
        track?.let {
            trackDetailModel.getDetails(it)
            binding.toolbar.title = it.name
            trackDetailModel.track.observe(this, { track ->
                binding.detailCard.track = track
            })
        }
    }

    /**
     *  Draws the given track route on the [mMap].
     *
     *  @param track The route that must be drawn on the map.
     */
    private fun drawLine(track: Track) {
        track.positions.let { positions ->
            if (positions.isEmpty()) return
            // Draw polyline using the coordinates included in the track in pairs
            val options = PolylineOptions().width(5f).color(Color.RED)
            val boundBuilder = LatLngBounds.builder()
            positions.zipWithNext().forEach { (start, end) ->
                val startCoord = LatLng(start.latitude, start.longitude)
                val endCoord = LatLng(end.latitude, end.longitude)
                options.add(startCoord, endCoord)
                boundBuilder.include(startCoord)
            }
            mMap.addPolyline(options)
            val bounds = boundBuilder.build()

            // Set the map view to display the whole track
            // The center position is calculated by boundBuilder
            // The formula below is used to calculate the zoom level that includes the whole track
            val nePos = Position(latitude = bounds.northeast.latitude, longitude = bounds.northeast.longitude)
            val swPos = Position(latitude = bounds.southwest.latitude, longitude = bounds.southwest.longitude)
            val scale = Position.distance(nePos, swPos) / 500
            val zoomLvl = ((16 - ln(scale) / ln(2.0))).toFloat()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, zoomLvl))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Handling login result
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                trackDetailModel.user = FirebaseAuth.getInstance().currentUser
                updateTrackData()
            } else {
                Log.d(TAG, "Log-in failed, cause: ${response?.error?.errorCode}")
            }
        }
    }

    companion object {
        private val TAG = TrackDetailActivity::class.java.simpleName
        private const val RC_SIGN_IN = 125
    }
}