package com.romagmir.biketrack.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import androidx.preference.PreferenceDialogFragmentCompat

/**
 * Dialog that contains a [NumberPickerPreference] and can be used to select a preference using a
 * number spinner.
 */
class NumberPickerPreferenceDialog : PreferenceDialogFragmentCompat() {
    /** View that will be shown inside the dialog */
    private lateinit var numberPicker: NumberPicker

    override fun onCreateDialogView(context: Context?): View {
        numberPicker = NumberPicker(context)
        return numberPicker
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        // Create and initialize the [NumberPickerPreference]
        val pref = preference as NumberPickerPreference
        numberPicker.minValue = pref.minValue
        numberPicker.maxValue = pref.maxValue
        numberPicker.value = pref.persistedInt
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            numberPicker.clearFocus()
            val newValue = numberPicker.value
            if (preference.callChangeListener(newValue)) {
                (preference as NumberPickerPreference).persistedInt = newValue
            }
        }
    }

    companion object {
        /**
         * Create a new instance of this class.
         *
         * @param key Preference key value
         * @return A configured [NumberPickerPreferenceDialog]
         */
        fun newInstance(key: String) : NumberPickerPreferenceDialog {
            val fragment = NumberPickerPreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}