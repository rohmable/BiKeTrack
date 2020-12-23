package com.romagmir.biketrack.ui

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.TrackRowItemBinding
import com.romagmir.biketrack.model.Track

/**
 * Container for tracks data that can be shown on a [android.widget.ListView].
 *
 * This adapter creates views with the layout specified in res/layout/track_row_item.xml
 *
 * @property trackList Track list to initially show.
 * @property onOpen Lambda function that executes when the user presses the "Open" button.
 * @property onDelete Lambda function that executes when the user presses the "Delete" button.
 */
class TracksAdapter(context: Context, trackList: List<Track> = ArrayList()) :
    ArrayAdapter<Track>(context, 0, trackList) {
    /** Localized date format used to show the date inside the views */
    private val dateFormat = DateFormat.getMediumDateFormat(context)
    /** Localized time format used to show the time inside the views */
    private val timeFormat = DateFormat.getTimeFormat(context)
    /** Track list to show */
    var tracks: List<Track> = trackList
    set(value) {
        field = value
        clear()
        addAll(field)
    }
    /** Listener for events */
    var trackAdapterListener: TrackAdapterListener? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        lateinit var binding: TrackRowItemBinding

        convertView?.let {
            // Recycling an existing view
            binding = TrackRowItemBinding.bind(convertView)
        } ?: run {
            // Creating a new one
            binding = TrackRowItemBinding.inflate(LayoutInflater.from(context))
        }

        val track = tracks[position]

        // Data binding
        binding.txtRowDate.text = context.getString(
            R.string.date_format,
            dateFormat.format(track.date),
            timeFormat.format(track.date)
        )
        binding.txtRowName.text = track.name
        binding.txtRowDistance.text = context.getString(R.string.distance_format, track.distance / 1000)
        binding.txtRowLength.text = context.getString(R.string.length_format, track.length.toFloat()/(3600 * 1000))
        binding.txtRowCalories.text = context.getString(R.string.kcal_format, track.calories)
        binding.cardView.setOnClickListener { trackAdapterListener?.onOpen(track) }
        binding.imgDelete.setOnClickListener { trackAdapterListener?.onDelete(track) }
        return binding.root
    }

    /**
     * Listener interface to handle the various events triggered by the adapter.
     */
    interface TrackAdapterListener {
        /**
         * Handles the onDelete signal of the [TracksAdapter].
         *
         * @param track Track associated with the card shown.
         */
        fun onOpen(track: Track)

        /**
         * Handles the onDelete signal of the [TracksAdapter].
         *
         * @param track Track associated with the card shown.
         */
        fun onDelete(track: Track)
    }

}
