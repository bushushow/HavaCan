package com.kampplus.havacan.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface Fix {
    data class Found(val latitude: Double, val longitude: Double) : Fix
    data object NoPermission : Fix
    data object ServicesOff : Fix
    data object NotFound : Fix
}

/** The phone's position and, for places abroad, a name for it. */
interface DeviceLocator {
    suspend fun locate(): Fix

    /** City and country for a point abroad; null when the phone can't name it (never made up). */
    suspend fun nameOf(latitude: Double, longitude: Double): Pair<String, String?>?
}

/**
 * Android's own LocationManager (no Play Services dependency), approximate accuracy is enough to
 * pick a district. Waits at most 15 s for a fresh fix, then falls back to a recent last-known one.
 */
class AndroidDeviceLocator(private val context: Context) : DeviceLocator {

    @SuppressLint("MissingPermission") // checked first
    override suspend fun locate(): Fix {
        val granted = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return Fix.NoPermission
        val manager = context.getSystemService(LocationManager::class.java) ?: return Fix.NotFound
        if (!LocationManagerCompat.isLocationEnabled(manager)) return Fix.ServicesOff
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        for (provider in providers.take(2)) {
            val fresh = withTimeoutOrNull(15_000) { current(manager, provider) }
            if (fresh != null) return Fix.Found(fresh.latitude, fresh.longitude)
        }
        val last = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 30 * 60_000 }
            .maxByOrNull { it.time }
        return last?.let { Fix.Found(it.latitude, it.longitude) } ?: Fix.NotFound
    }

    @SuppressLint("MissingPermission")
    private suspend fun current(manager: LocationManager, provider: String): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
            if (cont.isActive) cont.resume(location)
        }
    }

    override suspend fun nameOf(latitude: Double, longitude: Double): Pair<String, String?>? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.forLanguageTag("tr"))
        val address = if (Build.VERSION.SDK_INT >= 33) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
        }
        val found = address ?: return@withContext null
        val name = found.locality ?: found.subAdminArea ?: found.adminArea ?: return@withContext null
        name to found.countryName
    }
}
