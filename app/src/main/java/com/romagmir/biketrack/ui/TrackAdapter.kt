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
class TracksAdapter(context: Context, trackList: List<Track> = ArrayList(), val onOpen: (Track) -> Unit, val onDelete: (Track) -> Unit) :
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
        binding.txtRowLength.text = context.getString(R.string.length_format, (track.length/(3600 * 1000)).toFloat())
        binding.btnOpen.setOnClickListener { onOpen(track) }
        binding.btnDelete.setOnClickListener { onDelete(track) }
        return binding.root
    }

}
