package com.romagmir.biketrack.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Utility class used to keep the local preferences synchronized between devices.
 */
class PreferencesSynchronizer(val context: Context, user: FirebaseUser, private var preferences: SharedPreferences) {
    /** Database reference */
    private var database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("settings").child(user.uid)
    /** User that must be synchronized */
    var user: FirebaseUser = user

    /**
     * Write data to the database.
     *
     * This operation is asynchronous.
     */
    fun upload() {
        Log.d(TAG, "Uploading settings for user ${user.uid}")
        preferences.write(database)
    }

    /**
     * Start downloading preferences data.
     *
     * This operation is asynchronous due to the use of the [android.content.SharedPreferences.Editor.apply()]
     * method.
     */
    fun download() {
        Log.d(TAG, "Downloading settings for user ${user.uid}")
        database.addListenerForSingleValueEvent(preferencesListener)
    }

    /**
     * Listens to the synchronization of Firebase Realtime Database values.
     */
    private val preferencesListener = object : ValueEventListener {
        /**
         * This method will be called with a snapshot of the data at this location. It will also be called
         * each time that data changes.
         *
         * @param snapshot The current data at the location
         */
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG, "Received settings data, now reading")
            preferences.read(snapshot)
        }

        /**
         * This method will be triggered in the event that this listener either failed at the server, or
         * is removed as a result of the security and Firebase Database rules. For more information on
         * securing your data, see: [Security Quickstart](https://firebase.google.com/docs/database/security/quickstart)
         *
         * @param error A description of the error that occurred
         */
        override fun onCancelled(error: DatabaseError) { }

    }

    /**
     * Writes the preference data to a DatabaseReference
     *
     * @param ref Database reference to write to
     */
    private fun SharedPreferences.write(ref: DatabaseReference) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                ref.updateChildren(all)
                Log.d(TAG, "Settings written")
            }
        }

    }

    /**
     * Reads the preference data from a DataSnapshot.
     *
     * String set type is not supported.
     *
     * @param data Database data to read
     */
    private fun SharedPreferences.read(data: DataSnapshot) {
        with(edit()) {
            all.forEach { (key, pref) ->
                if (data.hasChild(key)) {
                    val value = data.child(key)
                    when(pref) {
                        is String -> putString(key, value.getValue<String>() ?: "")
                        is Int -> putInt(key, value.getValue<Int>() ?: 0)
                        is Boolean -> putBoolean(key, value.getValue<Boolean>() ?: false)
                        is Float -> putFloat(key, value.getValue<Float>() ?: 0f)
                        is Long -> putLong(key, value.getValue<Long>() ?: 0L)
                    }
                }
            }
            apply()
        }
        Log.d(TAG, "Settings data read")
    }

    companion object {
        /** Log tag */
        private val TAG = PreferencesSynchronizer::class.simpleName
    }
}
