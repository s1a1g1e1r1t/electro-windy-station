package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val regionLabel: String
)

class LocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoarseLocation(): DetectedLocation? = withContext(Dispatchers.IO) {
        val fusedResult = tryFusedLocation()
        val location = fusedResult ?: tryPlatformLocationManager() ?: return@withContext null

        val resolved = resolveCityName(location.latitude, location.longitude)
        DetectedLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            displayName = resolved.first,
            regionLabel = resolved.second
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryFusedLocation(): Location? = suspendCancellableCoroutine { cont ->
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (cont.isActive) cont.resume(loc)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryPlatformLocationManager(): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            providers.firstNotNullOfOrNull { provider ->
                if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(lat: Double, lon: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale("uk", "UA"))
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            val region = addr?.adminArea ?: addr?.countryName
            if (!city.isNullOrBlank()) {
                city to (region ?: String.format(Locale.US, "%.2f°, %.2f°", lat, lon))
            } else {
                "Ваша локація" to String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
            }
        } catch (_: Exception) {
            "GPS Локація" to String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
        }
    }
}
