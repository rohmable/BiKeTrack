package com.romagmir.biketrack.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.cardview.widget.CardView
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.WelcomeCardBinding

/**
 * Shows a welcome message displaying the user name and his weekly objectives (if enabled)
 */
class WelcomeCard(context: Context, attrs: AttributeSet) :
    CardView(context, attrs)  {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] to interact with the children views*/
    private var binding = WelcomeCardBinding.bind(inflate(context, R.layout.welcome_card, this))

    /** Username to display */
    var userName = ""
    set(value) {
        field = value
        updateUser()
    }

    /** Display the weekly objectives or not */
    var showWeekly = false
    set(value) {
        field = value
        updateWeekly()
    }

    /** Hours of training done in the current week */
    var hoursDone = 0f
    set(value) {
        field = value
        if (showWeekly) {
            updateHours()
        }
    }

    /** Hours of training to do in the current week */
    var hoursObjective = 0f
    set(value) {
        field = value
        if (showWeekly) {
            updateHours()
        }
    }

    /** Calories burned in the current week */
    var caloriesDone = 0f
    set(value) {
        field = value
        if (showWeekly) {
            updateCalories()
        }
    }

    /** Calories to burn in the current week */
    var caloriesObjective = 0f
    set (value) {
        field = value
        if (showWeekly) {
            updateCalories()
        }
    }

    /**
     * Updates the welcome message with the given user name
     */
    private fun updateUser() {
        binding.txtWelcome.text = context.getString(R.string.welcome_format, userName)
    }

    /**
     * Shows or hides the weekly objectives
     */
    private fun updateWeekly() {
        if (showWeekly) {
            binding.lytWeekly.visibility = View.VISIBLE
            updateHours()
            updateCalories()
        } else {
            binding.lytWeekly.visibility = View.GONE
        }
    }

    /**
     * Updates the hours objective
     */
    private fun updateHours() {
        binding.txtHours.text = context.getString(R.string.welcome_weekly_format, hoursDone, hoursObjective)
        binding.prgHours.max = hoursObjective.toInt()
        binding.prgHours.progress = hoursDone.toInt()
    }

    /**
     * Updates the calories objective
     */
    private fun updateCalories() {
        binding.txtCalories.text = context.getString(R.string.welcome_weekly_format, caloriesDone, caloriesObjective)
        binding.prgCalories.max = caloriesObjective.toInt()
        binding.prgCalories.progress = caloriesDone.toInt()
    }
}