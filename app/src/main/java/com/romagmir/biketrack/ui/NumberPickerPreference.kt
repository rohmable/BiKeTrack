package com.romagmir.biketrack.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.romagmir.biketrack.R

/**
 * Preference that allows to select a number in a given range [[minValue], [maxValue]].
 *
 * @constructor Creates a NumberPickerPreference with the given context and attributes.
 */
class NumberPickerPreference(context: Context, attrs: AttributeSet) :
DialogPreference(context, attrs) {
    /** Default value, if the user never changes the preference this value will be used */
    private val initialValue: Int
    /** Maximum possible value that the preference can assume */
    val maxValue: Int
    /** Minimum possible value that the preference can assume */
    val minValue: Int
    /** Contains the selected preference or the default value if it was never set by the user */
    var persistedInt: Int
    get() = super.getPersistedInt(initialValue)
    set(value) {
        super.persistInt(value)
        notifyChanged()
    }
    /** Suffix to show in the summary */
    private val suffix: String

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference)
        maxValue = attributes.getInt(R.styleable.NumberPickerPreference_max_value, Int.MAX_VALUE)
        minValue = attributes.getInt(R.styleable.NumberPickerPreference_min_value, Int.MIN_VALUE)
        suffix = attributes.getString(R.styleable.NumberPickerPreference_suffix) ?: ""
        val initValue = attributes.getInt(R.styleable.NumberPickerPreference_initial_value, 0)
        initialValue = if (initValue in minValue..maxValue) initValue else (minValue + maxValue) / 2
        attributes.recycle()
    }

    override fun getSummary(): CharSequence = "$persistedInt $suffix"
}