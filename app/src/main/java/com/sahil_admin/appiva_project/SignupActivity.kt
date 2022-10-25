package com.sahil_admin.appiva_project

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.appiva_project.Utility.DATE_TIME_FORMAT
import com.sahil_admin.appiva_project.Utility.REQUEST_CODE_GALLERY
import com.sahil_admin.appiva_project.Utility.REQUEST_CODE_IMAGE_CAPTURE
import com.sahil_admin.appiva_project.Utility.REQUEST_CODE_LOCATION_PERMISSIONS
import com.sahil_admin.appiva_project.Utility.REQUEST_CODE_ONE_TAP
import com.sahil_admin.appiva_project.Utility.REQUEST_CODE_PERMISSIONS
import com.sahil_admin.appiva_project.Utility.REQUIRED_PERMISIONS_LOCATION
import com.sahil_admin.appiva_project.Utility.REQUIRED_PERMISSIONS_CAMERA
import com.sahil_admin.appiva_project.Utility.encodeBitmaptoBase64
import com.sahil_admin.appiva_project.databinding.ActivitySignupBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


class SignupActivity : AppCompatActivity() {

    val userCollectionRef = Firebase.firestore.collection("Users")
    val auth = Firebase.auth
    lateinit var binding: ActivitySignupBinding
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --------------------------- Google sign in ------------------------------------------- //
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
        // -------------------------------------------------------------------------------------- //
    }

    override fun onStart() {
        super.onStart()

        binding.buttonLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        binding.buttonRegister.setOnClickListener { registerClicked() }
        binding.imageViewGoogle.setOnClickListener { googleSignIn() }
        binding.clCamera.setOnClickListener { addImageClicked() }
        binding.clGallery.setOnClickListener { selectFromGalleryClicked() }

    }

    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GALLERY -> {
                if(data != null && data.data != null) {

                    val image_bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)

                    Glide.with(this)
                        .load(image_bitmap)
                        .fitCenter()
                        .circleCrop()
                        .into(binding.ivImage)
                }
            }

            REQUEST_CODE_IMAGE_CAPTURE -> {

                if(data != null && data.extras != null) {
                    val image_bitmap = data.extras!!.get("data") as Bitmap

                    Glide.with(this)
                        .load(image_bitmap)
                        .fitCenter()
                        .circleCrop()
                        .into(binding.ivImage)
                }
            }

            REQUEST_CODE_ONE_TAP -> {
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
                                        Toast.makeText(this, "SignUp failed!", Toast.LENGTH_SHORT).show()
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

    private fun googleSignIn () {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQUEST_CODE_ONE_TAP,
                        null, 0, 0, 0)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, "API error, please try again!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener(this) {
                Toast.makeText(this, "API error, please try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addImageClicked() {

        // Request camera permissions
        if (allPermissionsGranted(REQUIRED_PERMISSIONS_CAMERA)) {
            // start camera activity
            startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_IMAGE_CAPTURE)

        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS_CAMERA, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun selectFromGalleryClicked(){
        startActivityForResult(Intent(Intent.ACTION_PICK)
            .setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            REQUEST_CODE_GALLERY)
    }

    private fun registerClicked() {
        val email = binding.textViewEmailid.text.toString()
        val password = binding.textViewPassword.text.toString()
        val rePassword = binding.textViewPasswordReenter.text.toString()

        when {
            !Utility.isValidEmail(email) -> {
                binding.textViewWrongEmail.visibility = View.VISIBLE
                return
            }
            !Utility.isValidPassword(password) -> {
                binding.textViewWrongEmail.visibility = View.VISIBLE
                return
            }
            !rePassword.equals(password) -> {
                binding.textViewWrongRePassword.visibility = View.VISIBLE
                return
            }
            Utility.isValidEmail(email) -> binding.textViewWrongEmail.visibility = View.GONE
            Utility.isValidPassword(password) -> binding.textViewWrongEmail.visibility = View.GONE
            rePassword.equals(password) -> binding.textViewWrongRePassword.visibility = View.GONE

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

    private fun profileSetUp () {
        val user = auth.currentUser!!

        if (user.displayName == null) {
            val profileUpdates = userProfileChangeRequest {
                displayName = user.email
            }

            user.updateProfile(profileUpdates).addOnCompleteListener {
                getLocation()
            }

        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@SignupActivity)

        if(allPermissionsGranted(REQUIRED_PERMISIONS_LOCATION)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) return

            fusedLocationClient.lastLocation.addOnSuccessListener {
                addDetails(it)
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISIONS_LOCATION, REQUEST_CODE_LOCATION_PERMISSIONS)
        }
    }

    private fun addDetails(location: Location?) = CoroutineScope(Dispatchers.IO).launch {

        try {
            val user = auth.currentUser!!
            val querySnapshot = userCollectionRef.whereEqualTo("email", user.email).get().await()
            val imageBitmapCode = encodeBitmaptoBase64(binding.ivImage.drawable.toBitmap())

            if (querySnapshot.documents.isEmpty()) {
                // add to database

                userCollectionRef
                    .add(User(user.email.toString(),
                        user.displayName.toString(),
                        imageBitmapCode,
                        GeoPoint(location!!.latitude, location.longitude),
                        SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH)
                            .format(System.currentTimeMillis()))).await()

                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@SignupActivity, DashboardActivity::class.java))
                    finish()
                }
            } else {
                startActivity(Intent(this@SignupActivity, DashboardActivity::class.java))
                finish()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SignupActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted(permissions: Array<String>) =
        permissions.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

}