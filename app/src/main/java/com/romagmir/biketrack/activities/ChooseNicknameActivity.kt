package com.romagmir.biketrack.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.romagmir.biketrack.databinding.ActivityChooseNicknameBinding

class ChooseNicknameActivity : AppCompatActivity() {
    /** [ViewBinding][androidx.viewbinding.ViewBinding] used to interact with the children views */
    private lateinit var binding: ActivityChooseNicknameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseNicknameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edit text default value
        val user = intent.extras?.getString(LoginActivity.MAIL_KEY) ?: ""
        binding.txtNickname.addTextChangedListener { nickname ->
            binding.btnNext.isEnabled = nickname?.isNotEmpty() ?: false
        }
        binding.txtNickname.setText(user)

        // Set default displayName
        binding.btnNext.isEnabled = false
        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(user).build()
        FirebaseAuth.getInstance().currentUser?.let { it ->
            it.updateProfile(profileUpdates).addOnCompleteListener(this) {
                binding.btnNext.isEnabled = true
            }
        }
    }

    /**
     * Handler for onClick event of the btnNext
     */
    fun onNextPressed(src: View) {
        val nickname = binding.txtNickname.text.toString()

        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(nickname).build()
        FirebaseAuth.getInstance().currentUser?.let { user ->
            user.updateProfile(profileUpdates).addOnCompleteListener(this) {
                val tracksListIntent = Intent(this, TracksListActivity::class.java)
                startActivity(tracksListIntent)
                finish()
            }
        }
    }
}