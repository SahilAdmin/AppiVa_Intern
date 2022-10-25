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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.appiva_project.Utility.isValidEmail
import com.sahil_admin.appiva_project.Utility.isValidPassword
import com.sahil_admin.appiva_project.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding
    val auth = Firebase.auth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val REQ_ONE_TAP = 2
    private var showOneTapUI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityLoginBinding.inflate(layoutInflater)
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

    override fun onStart() {
        super.onStart()

        binding.buttonSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.buttonLogin.setOnClickListener {
            loginClicked()
        }

        binding.imageViewGoogle.setOnClickListener {
            googleSignin()
        }
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
                                        checkSignUp()

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
                    // closed the dialog without signing in
                }
            }
        }
    }

    fun googleSignin() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0)

                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Sign in failure, please try later!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener(this) { e ->
                Toast.makeText(this, "Sign in failure, please try later", Toast.LENGTH_SHORT).show()
            }
    }

    fun loginClicked() {
        val email = binding.textViewEmailid.text.toString()
        val password = binding.textViewPassword.text.toString()

        if (!isValidEmail(email)) {
            binding.textViewWrongEmail.visibility = View.VISIBLE
            return

        } else {
            binding.textViewWrongEmail.visibility = View.GONE
        }

        if(!isValidPassword(password)) {
            binding.textViewWrongPassword.visibility = View.VISIBLE
            return

        } else {
            binding.textViewWrongPassword.visibility = View.GONE
        }

        // Validated everything

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    checkSignUp()

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkSignUp () = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser!!
        val querySnapshot = userCollectionRef.whereEqualTo("email", user.email).get().await()
        if(querySnapshot.documents.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "No user found, please SignUp first", Toast.LENGTH_SHORT).show()

            }
        } else {
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                finish()
            }
        }
    }
}