package com.example.data

import android.util.Log

object CrashReporter {
    fun initialize() {
        Log.i("CrashReporter", "Crash reporting initialized.")
    }

    fun logException(e: Exception) {
        Log.e("CrashReporter", "Non-fatal exception logged: \${e.message}", e)
        // In a real app, this would send to Firebase Crashlytics or similar
    }

    fun log(message: String) {
        Log.d("CrashReporter", "Log: $message")
    }
}
