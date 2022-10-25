package com.sahil_admin.appiva_project

import com.google.firebase.firestore.GeoPoint

data class User (
    var email: String = "",
    var name: String = "",
    var image: String = "",
    var location: GeoPoint? = null,
    var dateAndTime: String = ""
)