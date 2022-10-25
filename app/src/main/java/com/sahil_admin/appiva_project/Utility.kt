package com.sahil_admin.appiva_project

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import com.google.firebase.firestore.GeoPoint
import java.io.ByteArrayOutputStream

object Utility {
    fun isValidEmail (str: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(str).matches()
    }

    fun isValidPassword (str: String): Boolean {
        return str.length >= 10
    }

    fun decodeFromBase64(image: String?): Bitmap? {
        val decodedByteArray = Base64.decode(image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
    }

    fun encodeBitmaptoBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return  Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    fun geoUri(geoPoint: GeoPoint) =
        "http://maps.google.com/maps?q=loc:" + geoPoint.latitude.toString() + "," + geoPoint.longitude.toString()


    val REQUIRED_PERMISSIONS_CAMERA =
        mutableListOf (
            Manifest.permission.CAMERA,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

    val REQUIRED_PERMISIONS_LOCATION =
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).toTypedArray()


    const val DATE_TIME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 101
    const val REQUEST_CODE_IMAGE_CAPTURE = 102
    const val REQUEST_CODE_GALLERY = 103
    const val REQUEST_CODE_LOCATION_PERMISSIONS = 104
    const val REQUEST_CODE_ONE_TAP = 3
}
