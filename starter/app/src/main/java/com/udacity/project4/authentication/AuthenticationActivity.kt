package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding

    private val user = FirebaseAuth.getInstance().currentUser

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (user != null) {
            navigateToRemindersActivity()
        }

        binding.button.setOnClickListener {
            createSignInIntent()
        }

        /**
         * TODO: a bonus is to customize the sign in flow to look nice using :
         * https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
         * */
    }

    private fun navigateToRemindersActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createSignInIntent() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.map)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(authenticationResult: FirebaseAuthUIAuthenticationResult) {
        val idpResponse = authenticationResult.idpResponse

        if (authenticationResult.resultCode == RESULT_OK) {
            navigateToRemindersActivity()
        } else {
            val errorMessage = if (idpResponse != null) {
                idpResponse.error?.localizedMessage ?: getString(R.string.default_error_message)
            } else {
                getString(R.string.login_cancelled_message)
            }
            Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
        }
    }
}