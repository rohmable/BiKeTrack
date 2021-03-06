package com.romagmir.biketrack.ui

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.TrackDetailCardBinding
import com.romagmir.biketrack.model.Track

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
        binding.txtDetailDistance.text = context.getString(
            R.string.distance_format,
            track.distance / 1000
        )
        binding.txtDetailTime.text = context.getString(
            R.string.length_format,
            track.hoursLength
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
        binding.txtDetailAvgWatts.text = context.getString(
            R.string.watts_format,
            track.watts.toInt()
        )
        binding.txtDetailCaloriesBurned.text = context.getString(R.string.kcal_format, track.calories)
    }

    companion object {
        /** Log tag */
        private val TAG = TrackDetailCard::class.java.simpleName
    }
}