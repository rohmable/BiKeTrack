package com.romagmir.biketrack.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.PointsGraphSeries
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.ActivityTrackDetailBinding
import com.romagmir.biketrack.model.Position
import com.romagmir.biketrack.model.Track
import com.romagmir.biketrack.ui.RemoveTrackDialog
import com.romagmir.biketrack.viewModels.TrackDetailModel
import com.romagmir.biketrack.viewModels.TracksModel
import kotlin.math.ln
import kotlin.math.max

/**
 * Shows the details of a [Track].
 */
class TrackDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] used to interact with the children views */
    private lateinit var binding: ActivityTrackDetailBinding
    /** Logged user */
    private lateinit var user: FirebaseUser
    /** [ViewModel][androidx.lifecycle.ViewModel] used to store data during the whole app lifecycle */
    private lateinit var trackDetailModel: TrackDetailModel
    /** Map that is shown on the top of the activity */
    private lateinit var mMap: GoogleMap
    /** Displayed track */
    private var track = Track()
    /** Shows on the graph where the user has set his seekbar */
    private val cursorSeries = LineGraphSeries<DataPoint>()
    /** Shows on the graph the altitude value at the specified distance */
    private val cursorAltitudeSeries = PointsGraphSeries<DataPoint>()
    /** Shows on the graph the speed value at the specified distance */
    private val cursorSpeedSeries = PointsGraphSeries<DataPoint>()
    /** Shows the altitude variation on the graph */
    private val altitudeSeries = LineGraphSeries<DataPoint>()
    /** Shows the speed variation on the graph */
    private val speedSeries = LineGraphSeries<DataPoint>()
    /** Maximum value between the altitude and the speed */
    private var maxY = 0.0
    /** Shows on the map the position selected by the user with the seekbar */
    private lateinit var mapMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)
        binding = ActivityTrackDetailBinding.inflate(layoutInflater)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        FirebaseAuth.getInstance().currentUser?.let {
            user = it
        } ?: run {
            finish()
        }
        trackDetailModel = ViewModelProviders.of(this, TrackDetailModel.TrackDetailModelFactory(application, user))
            .get(TrackDetailModel::class.java)

        // Enable the "back arrow" on the toolbar to go back to the previous activity
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Start retrieving the details of the given track (if any)
        track = intent.extras?.getParcelable(TracksListActivity.TRACK_KEY) ?: Track()
        binding.detailToolbar?.title = track.name
        trackDetailModel.getDetails(track)

        // Setup the various graph views
        binding.detailGraph.gridLabelRenderer.labelFormatter = graphLabelFormatter
        cursorSeries.color = Color.WHITE
        cursorAltitudeSeries.shape = PointsGraphSeries.Shape.POINT
        cursorAltitudeSeries.color = getColor(R.color.altitude_color)
        cursorSpeedSeries.color = getColor(R.color.speed_color)
        cursorSpeedSeries.shape = PointsGraphSeries.Shape.POINT

        altitudeSeries.isDrawBackground = true
        altitudeSeries.color = getColor(R.color.altitude_color)
        altitudeSeries.backgroundColor = getColor(R.color.altitude_background_color)
        altitudeSeries.title = getString(R.string.altitude)

        speedSeries.title = getString(R.string.speed)
        speedSeries.color = getColor(R.color.speed_color)

        with(binding.detailGraph) {
            addSeries(altitudeSeries)
            secondScale.addSeries(speedSeries)
            addSeries(cursorSeries)
            addSeries(cursorAltitudeSeries)
            secondScale.addSeries(cursorSpeedSeries)
        }

        // Setup listeners
        binding.detailSelector.setOnSeekBarChangeListener(detailSeekbarChangeListener)

        // Settings menu
        setSupportActionBar(binding.detailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.detailToolbar?.setOnMenuItemClickListener(menuItemListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Called when the map is available.
     *
     * Here all the map operations are made.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        trackDetailModel.track.observe(this) {
            updateTrackData(it)
            drawLine(it)
            if (it.positions.isNotEmpty()) {
                setContentView(binding.root)
            }
        }
    }

    /**
     * Update the activity UI with the given [Track].
     *
     * The only view that is not updated is the map, this is made inside the [onMapReady] method.
     */
    private fun updateTrackData(track: Track) {
        Log.d(TAG, "Drawing track graphs")
        this.track = track

        binding.detailCard.track = track

        // Draw altitude graph
        if (track.positions.isNotEmpty()) {
            val altitudePoints = ArrayList<DataPoint>()
            val speedPoints = ArrayList<DataPoint>()

            track.positions.forEach {
                altitudePoints.add(DataPoint(it.distance, it.altitude))
                speedPoints.add(DataPoint(it.distance, it.speed))
            }
            altitudeSeries.resetData(altitudePoints.toTypedArray())
            speedSeries.resetData(speedPoints.toTypedArray())
            maxY = max(altitudeSeries.highestValueY, speedSeries.highestValueY)
            binding.detailSelector.max = altitudeSeries.highestValueX.toInt()

            with(binding.detailGraph) {
                viewport.setMaxX(altitudeSeries.highestValueX)
                viewport.isScalable = true
                secondScale.setMinY(0.0)
                secondScale.setMaxY(speedSeries.highestValueY)
            }
        }
    }

    /**
     *  Draws the given track route on the [mMap].
     *
     *  @param track The route that must be drawn on the map.
     */
    private fun drawLine(track: Track) {
        Log.d(TAG, "Drawing track route on map")
        if (track.positions.isEmpty()) return
        val options = PolylineOptions().width(5f).color(getColor(R.color.polyline_color))
        val boundBuilder = LatLngBounds.builder()
        track.positions.zipWithNext().forEach { (start, end) ->
            options.add(start.toLatLng(), end.toLatLng())
            boundBuilder.include(start.toLatLng())
        }
        mMap.addPolyline(options)
        val bounds = boundBuilder.build()

        // Set the map view to display the whole track
        // The center position is calculated by boundBuilder
        // The formula below is used to calculate the zoom level that includes the whole track
        val nePos = Position(latitude = bounds.northeast.latitude, longitude = bounds.northeast.longitude)
        val swPos = Position(latitude = bounds.southwest.latitude, longitude = bounds.southwest.longitude)
        val scale = Position.distance(nePos, swPos) / 300
        val zoomLvl = ((16 - ln(scale) / ln(2.0))).toFloat()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, zoomLvl))
    }

    /**
     * Formatter used for the primary axis by the graph view.
     */
    private val graphLabelFormatter = object : DefaultLabelFormatter() {
        override fun formatLabel(value: Double, isValueX: Boolean): String {
            return if (isValueX) {
                "${(value / 1000).toInt()}"
            } else {
                "${value.toInt()} m"
            }
        }
    }

    /**
     * Listens for the changes on the seekbar and updates the UI accordingly.
     *
     * Updates:
     * * The map view with a marker that shows the location
     * * The values of altitude and longitude
     * * The graph view with a line that runs over the selected distance and the altitude and speed values at that distance
     */
    private val detailSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range min..max where min
         * and max were set by [ProgressBar.setMin] and
         * [ProgressBar.setMax], respectively. (The default values for
         * min is 0 and max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val cursorDataPoints = arrayOf(
                DataPoint(progress.toDouble(), 0.0),
                DataPoint(progress.toDouble(), maxY)
            )
            lateinit var cursorAltitudePoints: Array<DataPoint>
            lateinit var cursorSpeedPoints: Array<DataPoint>
            try {
                // Get the first position with at least the given distance
                val pos = track.positions.first { it.distance >= progress }

                // Set UI updated values
                binding.txtAltitude.text = getString(R.string.detail_altitude, pos.altitude)
                binding.txtSpeed.text = getString(R.string.detail_speed, pos.speed)
                cursorAltitudePoints = arrayOf(
                    DataPoint(progress.toDouble(), pos.altitude)
                )
                cursorSpeedPoints = arrayOf(
                    DataPoint(progress.toDouble(), pos.speed)
                )

                // Remove the previous marker (if any) and show the new one
                if (this@TrackDetailActivity::mapMarker.isInitialized) {
                    mapMarker.remove()
                }
                mapMarker = mMap.addMarker(MarkerOptions().position(pos.toLatLng()))
            } catch (e: NoSuchElementException) {
                // If there are no positions with at least this distance a bunch of placeholder
                // values are set
                binding.txtAltitude.text = getString(R.string.detail_altitude, 0.0)
                binding.txtSpeed.text = getString(R.string.detail_speed, 0.0)
                cursorAltitudePoints = arrayOf(DataPoint(progress.toDouble(), 0.0))
                cursorSpeedPoints = arrayOf(DataPoint(progress.toDouble(), 0.0))
            } finally {
                cursorSeries.resetData(cursorDataPoints)
                cursorAltitudeSeries.resetData(cursorAltitudePoints)
                cursorSpeedSeries.resetData(cursorSpeedPoints)
            }
        }

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        override fun onStartTrackingTouch(seekBar: SeekBar?) { }

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        override fun onStopTrackingTouch(seekBar: SeekBar?) { }
    }

    private val menuItemListener = androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {
        when(it.itemId) {
            R.id.delete -> {
                val tracksModel = ViewModelProviders.of(this, TracksModel.TracksModelFactory(application, user))
                        .get(TracksModel::class.java)
                RemoveTrackDialog(onConfirm = {tracksModel.removeTrack(track); finish()})
                        .show(supportFragmentManager, CONFIRM_DIAG_TAG)
            }
        }
        false
    }

    companion object {
        /** Log tag */
        private val TAG = TrackDetailActivity::class.java.simpleName
        /** Tag used to show the [RemoveTrackDialog] dialog */
        private const val CONFIRM_DIAG_TAG = "CONFIRM_DIAG"
    }
}