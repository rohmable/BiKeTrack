package com.romagmir.biketrack.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import com.romagmir.biketrack.R

class RangeEditNumberPreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs) {
    /** Minimum possible value that the preference can assume */
    private val minValue: Float
    /** Maximum possible value that the preference can assume */
    private val maxValue: Float
    /** Suffix to show in the summary */
    private val suffix: String

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.RangeEditNumberPreference)
        minValue = attributes.getFloat(R.styleable.RangeEditNumberPreference_android_min, Float.MIN_VALUE)
        maxValue = attributes.getFloat(R.styleable.RangeEditNumberPreference_android_max, Float.MAX_VALUE)
        suffix =  attributes.getString(R.styleable.RangeEditNumberPreference_suffix) ?: ""
        attributes.recycle()
    }

    override fun setText(text: String?) {
        text?.let {
            try {
                val newVal = it.toFloat()
                if (newVal in minValue..maxValue) {
                    super.setText(it)
                }
                it
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid format: $text")
            }
        }
    }

    override fun getSummary(): CharSequence = "$text $suffix"

    companion object {
        /** Debug tag */
        private val TAG = RangeEditNumberPreference::class.java.simpleName
    }
}