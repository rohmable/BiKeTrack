package com.romagmir.biketrack.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.TrackDetailCardBinding
import com.romagmir.biketrack.model.Track
import kotlin.math.roundToInt

/**
 * Shows data for a given track on a [CardView].
 */
class TrackDetailCard(context: Context, attrs: AttributeSet) :
    CardView(context, attrs) {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] to interact with the children views*/
    private var binding: TrackDetailCardBinding = TrackDetailCardBinding.bind(
        inflate(context, R.layout.track_detail_card, this)
    )
    /** Selected track to show, automatically updates the data */
    var track: Track = Track()
    set(value) {
        field = value
        updateTrack()
    }

    /**
     * Updates the children views with the [track] data.
     */
    private fun updateTrack() {
        // Retrieve user info for watts calculation.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val height = sharedPreferences.getInt(
            context.getString(R.string.setting_height),
            180
        )
        val weight = sharedPreferences.getInt(
            context.getString(R.string.setting_weight),
            80
        )

        binding.txtDetailDistance.text = context.getString(
            R.string.distance_format,
            track.distance / 1000
        )
        binding.txtDetailTime.text = context.getString(
            R.string.length_format,
            track.length.toFloat() / (3600 * 1000)
        )
        binding.txtMinAltitude.text = context.getString(
            R.string.altitude_format,
            track.minAltitude
        )
        binding.txtMaxAltitude.text = context.getString(
            R.string.altitude_format,
            track.maxAltitude
        )
        binding.txtElevationGain.text = context.getString(R.string.altitude_format, track.elevationGain)
        binding.txtDetailAvgSpeed.text = context.getString(
            R.string.speed_format,
            track.avgSpeed
        )

        val watts = track.calcWatts(weight.toDouble(), height.toDouble())
        binding.txtDetailAvgWatts.text = context.getString(
            R.string.watts_format,
            watts.roundToInt()
        )
        Log.d(TAG, "Track key is ${track.key}")
    }

    companion object {
        /** Log tag */
        private val TAG = TrackDetailCard::class.java.simpleName
    }
}