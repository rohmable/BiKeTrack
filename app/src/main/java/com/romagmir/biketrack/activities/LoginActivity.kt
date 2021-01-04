package com.romagmir.biketrack.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.romagmir.biketrack.R
import com.romagmir.biketrack.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] used to interact with the children views */
    private lateinit var binding: ActivityLoginBinding
    /** Flag that signals if the inserted username is valid */
    var isUsernameValid = false
    /** Flag that signals if the inserted password is valid */
    var isPasswordValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add input validators
        binding.txtUsername.addTextChangedListener {
            isUsernameValid = android.util.Patterns.EMAIL_ADDRESS.matcher(it.toString()).matches()
            binding.btnLogin.isEnabled = isUsernameValid && isPasswordValid
        }
        binding.txtPassword.addTextChangedListener {
            isPasswordValid = it.toString().length > 5
            binding.btnLogin.isEnabled = isUsernameValid && isPasswordValid
        }
    }

    override fun onStart() {
        super.onStart()
        // If the user is already logged in return successfully
        FirebaseAuth.getInstance().currentUser?.let {
            Log.d(TAG, "User \"${it.displayName}\" is already logged in")
            logged()
        }
    }

    fun onLogPressed(src: View) {
        val email = binding.txtUsername.text.toString()
        val psw = binding.txtPassword.text.toString()
        // Log the user
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, psw)
            .addOnCompleteListener(this) {task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Login successful!")
                    logged()
                } else {
                    Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Login failed!")
                    reset()
                }
        }
        // While the Firebase framework tries to authenticate the user show
        // a progress bar in place of the login button
        binding.btnLogin.visibility = View.GONE
        binding.prgLoading.visibility = View.VISIBLE
    }

    private fun reset() {
        // Reset the UI to allow another login
        binding.txtPassword.text.clear()
        binding.btnLogin.visibility = View.VISIBLE
        binding.prgLoading.visibility = View.GONE
    }

    /**
     * Called to start the main activity after a successful login
     */
    private fun logged() {
        val tracksListIntent = Intent(this, TracksListActivity::class.java)
        startActivity(tracksListIntent)
        reset()
    }

    companion object {
        /** Logging tag */
        private val TAG = LoginActivity::class.java.simpleName
    }
}