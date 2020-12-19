package com.romagmir.biketrack.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.romagmir.biketrack.R

class RemoveTrackDialog(val onConfirm: () -> Unit = {}, val onDeny: () -> Unit = {}) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.dialog_text)
                .setPositiveButton(R.string.dialog_confirm) {_, _ -> onConfirm() }
                .setNegativeButton(R.string.dialog_deny) { _, _ -> onDeny() }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}