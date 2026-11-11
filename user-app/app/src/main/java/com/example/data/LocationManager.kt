package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.core.content.edit
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.*

data class Emirate(val name: String, val lat: Double, val lon: Double)

val uaeEmirates = listOf(
    Emirate("Dubai", 25.2048, 55.2708),
    Emirate("Abu Dhabi", 24.4539, 54.3773),
    Emirate("Sharjah", 25.3463, 55.4209),
    Emirate("Ajman", 25.4052, 55.4390),
    Emirate("Ras Al Khaimah", 25.7895, 55.9432),
    Emirate("Fujairah", 25.1288, 56.3265),
    Emirate("Umm Al Quwain", 25.5647, 55.5552)
)

class LocationManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _selectedEmirate = MutableStateFlow<String>(prefs.getString("selected_emirate", "Dubai") ?: "Dubai")
    val selectedEmirate: StateFlow<String> = _selectedEmirate.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): String {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) && 
                !locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                return "GPS is disabled"
            }

            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    cont.resume(it)
                }.addOnFailureListener {
                    cont.resume(null)
                }
            }
            if (location != null) {
                _userLocation.value = location.latitude to location.longitude
                val nearest = findNearestEmirate(location.latitude, location.longitude)
                setEmirate(nearest.name)
                return "Location found"
            } else {
                return "Could not determine location"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error finding location"
        }
    }

    fun setEmirate(name: String) {
        prefs.edit(commit = true) {
            putString("selected_emirate", name)
        }
        _selectedEmirate.value = name
    }

    private fun findNearestEmirate(lat: Double, lon: Double): Emirate {
        return uaeEmirates.minByOrNull { distance(lat, lon, it.lat, it.lon) } ?: uaeEmirates.first()
    }

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // km
        val p = Math.PI / 180
        val a = 0.5 - cos((lat2 - lat1) * p) / 2 +
                cos(lat1 * p) * cos(lat2 * p) *
                (1 - cos((lon2 - lon1) * p)) / 2
        return 2 * r * asin(sqrt(a))
    }
}
