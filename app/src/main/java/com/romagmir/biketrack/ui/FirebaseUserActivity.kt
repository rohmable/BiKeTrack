package com.romagmir.biketrack.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.romagmir.biketrack.R
import java.util.*
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * Activity that exposes a [FirebaseUser][import com.google.firebase.auth.FirebaseUser] and
 * handles the log in/sign up operations for all the activities that inherit from this class.
 */
open class FirebaseUserActivity : AppCompatActivity() {
    /** Logged user */
    var user: FirebaseUser? by Delegates.observable(FirebaseAuth.getInstance().currentUser) { prop, old, new ->
        onUserChanged(prop, old, new)
    }

    /**
     * Called when the user has changed.
     *
     * The value of the user has already been changed when this callback is invoked.
     *
     * @param property Property that called the method.
     * @param oldValue Previous user.
     * @param newValue New user.
     */
    open fun onUserChanged(property: KProperty<*>, oldValue: FirebaseUser?, newValue: FirebaseUser?) {
        newValue?.let {
            Log.d(TAG, "User \"${it.displayName}\" logged in (uid: ${it.uid})")
        } ?: run {
            logUser()
        }
    }

    /**
     * Perform initialization of all fragments.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logUser(force = true)
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are *not* resumed.
     */
    override fun onResume() {
        super.onResume()
        logUser()
    }

    /**
     * If the user is already logged then the [user] observable is immediately assigned, otherwise
     * an [AuthUI] activity is started.
     *
     * @param force Force the update of the user, calling the [onUserChanged] method.
     */
    private fun logUser(force: Boolean = false) {
        // If the user is already logged in the the data is retrieved, otherwise the app tries to
        // log the user
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let {
            if (user != it || force) {
                user = it
            }
        } ?: run {
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setLogo(R.mipmap.ic_launcher_round)
                    .setTheme(R.style.Theme_BiKeTrack)
                    .build(),
                RC_SIGN_IN
            )
        }
    }

    /**
     * Logs out the user and tries to log in again.
     */
    fun logOut() {
        FirebaseAuth.getInstance().signOut()
        logUser()
    }

    /**
     * Called when a launched activity exits, returning the requestCode
     * which it started with, the resultCode it returned, and any additional
     * data from it.
     *
     * Handles the log in/sign up operations.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Handling login result
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                user = FirebaseAuth.getInstance().currentUser
            } else {
                Log.d(TAG, "Log-in failed, cause ${response?.error?.errorCode}")
            }
        }
    }

    companion object {
        /** List of providers that can be used to log in/up */
        private val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        /** Log tag */
        private val TAG = FirebaseUserActivity::class.java.simpleName
        private const val RC_SIGN_IN = 123
    }
}