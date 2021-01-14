package com.romagmir.biketrack.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
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
        setContentView(R.layout.splash_screen)
        // If the user is already logged in return successfully
        FirebaseAuth.getInstance().currentUser?.let {
            Log.d(TAG, "User \"${it.displayName}\" is already logged in")
            logged()
        } ?: run {
            setContentView(binding.root)
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
                    when(task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show()
                            Log.d(TAG, "Login failed!")
                            reset()
                        }
                        is FirebaseAuthInvalidUserException -> {
                            // Try to register the user
                            register(email, psw)
                        }
                    }
                }
        }
        // While the Firebase framework tries to authenticate the user show
        // a progress bar in place of the login button
        binding.btnLogin.visibility = View.GONE
        binding.prgLoading.visibility = View.VISIBLE
    }

    private fun register(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "User registered successfully!")
                        // Start activity to ask for the nickname
                        val nicknameIntent = Intent(this, ChooseNicknameActivity::class.java)
                        nicknameIntent.putExtra(MAIL_KEY, email)
                        startActivity(nicknameIntent)
                        reset()
                    } else {
                        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Login failed!", task.exception)
                        reset()
                    }
                }
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

        const val MAIL_KEY = "MAIL"
    }
}