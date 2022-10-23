package com.sahil_admin.appiva_project

import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.appiva_project.DashboardActivity
import com.sahil_admin.appiva_project.LoginActivity
import com.sahil_admin.appiva_project.R
import com.sahil_admin.appiva_project.Utility
import com.sahil_admin.appiva_project.databinding.ActivitySignupBinding
import kotlinx.coroutines.GlobalScope

class SignupActivity : AppCompatActivity() {

    val userCollectionRef = Firebase.firestore.collection("Users")
    val auth = Firebase.auth
    lateinit var binding: ActivitySignupBinding
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val REQ_ONE_TAP = 3
    private var showOneTapUI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    when {
                        idToken != null -> {
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        profileSetUp()

                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Toast.makeText(this, "SignIn failed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        else -> {
                            // Shouldn't happen.
                            Toast.makeText(this, "API error, please try again!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: ApiException) {
                    // closed the dialog without signin in
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.buttonRegister.setOnClickListener {
            registerClicked()
        }

        binding.imageViewGoogle.setOnClickListener {
            googleSignIn()
        }
    }

    fun googleSignIn () {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "API error, please try again!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener(this) { e ->
                Toast.makeText(this, "API error, please try again!", Toast.LENGTH_SHORT).show()
            }
    }

    fun registerClicked() {
        val email = binding.textViewEmailid.text.toString()
        val password = binding.textViewPassword.text.toString()
        val rePassword = binding.textViewPasswordReenter.text.toString()

        if (!Utility.isValidEmail(email)) {
            binding.textViewWrongEmail.visibility = View.VISIBLE
            return

        } else {
            binding.textViewWrongEmail.visibility = View.GONE
        }

        if(!Utility.isValidPassword(password)) {
            binding.textViewWrongPassword.visibility = View.VISIBLE
            return

        } else {
            binding.textViewWrongPassword.visibility = View.GONE
        }

        if(!rePassword.equals(password)) {
            binding.textViewWrongRePassword.visibility = View.VISIBLE
            return

        } else {
            binding.textViewWrongRePassword.visibility = View.GONE
        }

        // Validated everything
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    profileSetUp()

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "User already exists, please try again",
                        Toast.LENGTH_SHORT).show()

                }
            }
    }

    fun profileSetUp () {
        val user = auth.currentUser!!

        //todo: Check if the email already exists...

        if(user.displayName == null) {
            val profileUpdates = userProfileChangeRequest {
                displayName = user.email
            }

            user.updateProfile(profileUpdates).addOnCompleteListener {
                startActivity(Intent(this, DashboardActivity::class.java))
                addDetails()
                finish()
            }

        } else {
            startActivity(Intent(this, DashboardActivity::class.java))
            addDetails()
            finish()
        }
    }

    fun addDetails() {
        val user = auth.currentUser!!

        userCollectionRef.add(User(user.email.toString(), user.displayName.toString(), 0, 0))
    }
}