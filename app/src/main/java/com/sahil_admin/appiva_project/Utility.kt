package com.sahil_admin.appiva_project

object Utility {
    fun isValidEmail (str: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(str).matches()
    }

    fun isValidPassword (str: String): Boolean {
        return str.length >= 10
    }
}
