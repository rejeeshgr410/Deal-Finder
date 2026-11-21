package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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

enum class LocationStatusKind {
    NONE,
    LOADING,
    SUCCESS,
    WARNING,
    ERROR
}

data class LocationResultState(
    val kind: LocationStatusKind = LocationStatusKind.NONE,
    val message: String = ""
)

class LocationManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val _selectedEmirate = MutableStateFlow(prefs.getString("selected_emirate", "Dubai") ?: "Dubai")
    val selectedEmirate: StateFlow<String> = _selectedEmirate.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _statusState = MutableStateFlow(LocationResultState())
    val statusState: StateFlow<LocationResultState> = _statusState.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun tryAutoDetectIfPermitted() {
        if (_userLocation.value != null || !hasLocationPermission()) return
        fetchCurrentLocation(updateEmirateAutomatically = false)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(updateEmirateAutomatically: Boolean = true): LocationResultState {
        if (!hasLocationPermission()) {
            val state = LocationResultState(LocationStatusKind.ERROR, "Location permission is required")
            _statusState.value = state
            return state
        }
        _statusState.value = LocationResultState(LocationStatusKind.LOADING, "Detecting GPS location...")

        try {
            val sysLoc = appContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val gpsOn = sysLoc.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            val netOn = sysLoc.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            if (!gpsOn && !netOn) {
                val state = LocationResultState(LocationStatusKind.WARNING, "GPS is disabled on device")
                _statusState.value = state
                return state
            }

            var location: Location? = suspendCancellableCoroutine { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }

            if (location == null) {
                val cts = CancellationTokenSource()
                location = suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation { cts.cancel() }
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }

            return if (location != null) {
                _userLocation.value = location.latitude to location.longitude
                val nearest = findNearestEmirate(location.latitude, location.longitude)
                if (updateEmirateAutomatically) {
                    setEmirate(nearest.name)
                }
                val state = LocationResultState(LocationStatusKind.SUCCESS, "Detected near ${nearest.name}")
                _statusState.value = state
                state
            } else {
                val state = LocationResultState(LocationStatusKind.WARNING, "Could not determine location")
                _statusState.value = state
                state
            }
        } catch (e: Exception) {
            val state = LocationResultState(LocationStatusKind.ERROR, e.message ?: "Error finding location")
            _statusState.value = state
            return state
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
        val r = 6371.0
        val p = Math.PI / 180.0
        val a = 0.5 - cos((lat2 - lat1) * p) / 2.0 +
            cos(lat1 * p) * cos(lat2 * p) * (1.0 - cos((lon2 - lon1) * p)) / 2.0
        return 2.0 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    companion object {
        fun localizedEmirateName(context: Context, canonicalName: String): String {
            return when (canonicalName) {
                "Dubai" -> context.getString(R.string.emirate_dubai)
                "Abu Dhabi" -> context.getString(R.string.emirate_abu_dhabi)
                "Sharjah" -> context.getString(R.string.emirate_sharjah)
                "Ajman" -> context.getString(R.string.emirate_ajman)
                "Ras Al Khaimah" -> context.getString(R.string.emirate_rak)
                "Fujairah" -> context.getString(R.string.emirate_fujairah)
                "Umm Al Quwain" -> context.getString(R.string.emirate_uaq)
                else -> canonicalName
            }
        }
    }
}
