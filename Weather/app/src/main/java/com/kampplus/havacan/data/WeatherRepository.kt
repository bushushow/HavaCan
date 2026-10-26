package com.kampplus.havacan.data

import com.kampplus.havacan.data.remote.ForecastApi
import com.kampplus.havacan.model.Forecast
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fetches forecasts and keeps them in memory for [cacheTtlMillis], so switching cities back and
 * forth, rotating the screen or toggling Koşucu Modu never triggers a new request.
 */
class WeatherRepository(
    private val api: ForecastApi,
    private val cacheTtlMillis: Long = 10 * 60 * 1000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Entry(val forecast: Forecast?, val fetchedAt: Long)

    private val cache = mutableMapOf<String, Entry>()
    private val mutex = Mutex()

    /** Returns null when the API answered but had no usable data. Throws on network/HTTP errors. */
    suspend fun forecast(latitude: Double, longitude: Double, forceRefresh: Boolean = false): Forecast? {
        val key = String.format(Locale.ROOT, "%.3f,%.3f", latitude, longitude)
        mutex.withLock {
            val cached = cache[key]
            if (!forceRefresh && cached != null && now() - cached.fetchedAt < cacheTtlMillis) {
                return cached.forecast
            }
        }
        val forecast = ForecastMapper.map(api.forecast(latitude, longitude))
        mutex.withLock { cache[key] = Entry(forecast, now()) }
        return forecast
    }
}
