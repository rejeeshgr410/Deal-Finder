package com.example.data

import android.util.Log

object AnalyticsManager {
    fun initialize() {
        Log.i("AnalyticsManager", "Analytics initialized.")
    }

    fun logEvent(eventName: String, params: Map<String, String>? = null) {
        Log.i("AnalyticsManager", "Event: $eventName, Params: $params")
        // In a real app, this would send to Google Analytics or similar
    }

    fun logScreenView(screenName: String) {
        Log.i("AnalyticsManager", "Screen View: $screenName")
    }
}
