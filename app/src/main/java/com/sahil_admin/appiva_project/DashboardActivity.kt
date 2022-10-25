package com.sahil_admin.appiva_project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.sahil_admin.appiva_project.Utility.decodeFromBase64
import com.sahil_admin.appiva_project.Utility.geoUri
import com.sahil_admin.appiva_project.databinding.ActivityDashboardBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


class DashboardActivity : AppCompatActivity() {

    lateinit var binding: ActivityDashboardBinding
    val auth = Firebase.auth
    val userCollectionRef = Firebase.firestore.collection("Users")
    lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
        }

        retriveUser()
    }

    private fun retriveUser () = CoroutineScope(Dispatchers.IO).launch {
        val email = auth.currentUser!!.email
        try {
            val querySnapshot = userCollectionRef.whereEqualTo("email", email).get().await()
            for(document in querySnapshot.documents) {
                val user = document.toObject<User>()!!
                val imageBitmap = decodeFromBase64(user.image)
                val geoUri = geoUri(user.location!!)

                withContext(Dispatchers.Main) {
                    binding.imageView2.setImageBitmap(imageBitmap)
                    binding.tvName.text = user.name
                    binding.tvDateandtime.text = "Date and Time of SignUP \n ${user.dateAndTime.format("yyyy-MM-dd-HH-mm")}"
                    binding.tvLocation.text = "Latitude: ${user.location!!.latitude} \n" +
                            "Longitude: ${user.location!!.longitude}"

                    binding.buttonLocation.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                        intent.setClassName(
                            "com.google.android.apps.maps",
                            "com.google.android.maps.MapsActivity"
                        )
                        startActivity(intent)
                    }
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}