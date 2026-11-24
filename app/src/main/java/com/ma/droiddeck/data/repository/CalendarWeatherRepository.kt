package com.ma.droiddeck.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.CalendarContract
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.ma.droiddeck.data.model.CalendarEventSummary
import com.ma.droiddeck.data.model.CallDirection
import com.ma.droiddeck.data.model.QuickAppEntry
import com.ma.droiddeck.data.model.RecentCallEntry
import com.ma.droiddeck.data.model.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Provides on-demand queries for Calendar events, the 2 most recent CallLog entries,
 * installed launcher apps (for Quick App Dock), and privacy-friendly Open-Meteo weather
 * with a local Sensor.TYPE_AMBIENT_TEMPERATURE fallback.
 */
class CalendarWeatherRepository(private val context: Context) {

    private val weatherPrefs by lazy {
        context.getSharedPreferences("desk_weather_cache", Context.MODE_PRIVATE)
    }
    private var cachedWeather: WeatherState? = null

    fun fetchNextCalendarEvent(): CalendarEventSummary? {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return null

        val now = System.currentTimeMillis()
        val endWindow = now + 24L * 60L * 60L * 1000L
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, endWindow)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )

        return runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.BEGIN} >= ?",
                arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                CalendarEventSummary(
                    eventId = cursor.getLong(0),
                    title = cursor.getString(1).orEmpty().ifBlank { "Untitled Event" },
                    startTimeMs = cursor.getLong(2),
                    endTimeMs = cursor.getLong(3),
                    location = cursor.getString(4).orEmpty()
                )
            }
        }.getOrNull()
    }

    fun fetchRecentCalls(limit: Int = 4): List<RecentCallEntry> {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return emptyList()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )

        return runCatching {
            val list = mutableListOf<RecentCallEntry>()
            val seenNumbers = HashSet<String>()
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext() && list.size < limit) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1)?.trim().orEmpty()
                    val number = cursor.getString(2)?.trim().orEmpty()
                    if (number.isBlank()) continue
                    val normalized = number.replace(Regex("[^0-9+]"), "").ifBlank { number }
                    if (!seenNumbers.add(normalized)) continue
                    val typeInt = cursor.getInt(3)
                    val dateMs = cursor.getLong(4)
                    val direction = when (typeInt) {
                        CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                        else -> CallDirection.INCOMING
                    }
                    list.add(
                        RecentCallEntry(
                            id = id,
                            displayName = name.ifBlank { number },
                            number = number,
                            direction = direction,
                            timestampMs = dateMs
                        )
                    )
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    fun queryInstalledLauncherApps(): List<QuickAppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        }.getOrElse {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                QuickAppEntry(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun fetchWeather(city: String, forceRefresh: Boolean = false): WeatherState =
        withContext(Dispatchers.IO) {
            val trimmedCity = city.trim()
            val now = System.currentTimeMillis()
            val cached = cachedWeather ?: loadPersistedWeather(trimmedCity)
            if (!forceRefresh &&
                cached != null &&
                cached.city.equals(trimmedCity, ignoreCase = true) &&
                (now - cached.updatedAtMs) < 30L * 60L * 1000L
            ) {
                cachedWeather = cached
                return@withContext cached
            }

            if (trimmedCity.isNotBlank()) {
                val remote = runCatching { fetchOpenMeteo(trimmedCity, now) }.getOrNull()
                if (remote != null) {
                    val withRememberedCity = remote.copy(city = remote.city.ifBlank { trimmedCity })
                    cachedWeather = withRememberedCity
                    savePersistedWeather(trimmedCity, withRememberedCity)
                    return@withContext withRememberedCity
                }
                // Return stale persisted weather for this city if offline
                val persisted = loadPersistedWeather(trimmedCity)
                if (persisted != null) {
                    cachedWeather = persisted
                    return@withContext persisted
                }
            }

            // Fallback to device ambient temperature sensor if present
            val ambient = readAmbientTemperatureOnce()
            WeatherState(
                city = trimmedCity.ifBlank { "Ambient" },
                temperatureCelsius = ambient,
                conditionLabel = when {
                    ambient != null -> "Ambient Sensor"
                    trimmedCity.isNotBlank() -> "Offline"
                    else -> "Set city in Settings"
                },
                isAmbientSensorFallback = true,
                updatedAtMs = now
            )
        }

    private fun loadPersistedWeather(requestedCity: String): WeatherState? {
        if (requestedCity.isBlank()) return null
        val savedQuery = weatherPrefs.getString("query_city", null) ?: return null
        if (!savedQuery.equals(requestedCity, ignoreCase = true)) return null
        if (!weatherPrefs.contains("temp_c")) return null
        return WeatherState(
            city = weatherPrefs.getString("resolved_city", requestedCity) ?: requestedCity,
            temperatureCelsius = weatherPrefs.getFloat("temp_c", 0f),
            conditionLabel = weatherPrefs.getString("condition", "Cached") ?: "Cached",
            isAmbientSensorFallback = false,
            updatedAtMs = weatherPrefs.getLong("updated_at_ms", 0L)
        )
    }

    private fun savePersistedWeather(requestedCity: String, state: WeatherState) {
        val temp = state.temperatureCelsius ?: return
        weatherPrefs.edit()
            .putString("query_city", requestedCity)
            .putString("resolved_city", state.city)
            .putFloat("temp_c", temp)
            .putString("condition", state.conditionLabel)
            .putLong("updated_at_ms", state.updatedAtMs)
            .apply()
    }

    private fun fetchOpenMeteo(city: String, nowMs: Long): WeatherState? {
        val encoded = URLEncoder.encode(city, "UTF-8")
        val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&format=json")
        val geoJson = httpGet(geoUrl) ?: return null
        val results = JSONObject(geoJson).optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.getJSONObject(0)
        val lat = first.getDouble("latitude")
        val lon = first.getDouble("longitude")
        val resolvedName = first.optString("name", city)

        val weatherUrl = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        )
        val weatherJson = httpGet(weatherUrl) ?: return null
        val current = JSONObject(weatherJson).optJSONObject("current") ?: return null
        val temp = current.optDouble("temperature_2m", Double.NaN)
        val code = current.optInt("weather_code", -1)
        if (temp.isNaN()) return null

        return WeatherState(
            city = resolvedName,
            temperatureCelsius = temp.toFloat(),
            conditionLabel = mapWmoWeatherCode(code),
            isAmbientSensorFallback = false,
            updatedAtMs = nowMs
        )
    }

    private fun httpGet(url: URL): String? {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun readAmbientTemperatureOnce(): Float? {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) ?: return null
        return suspendCoroutine { cont ->
            var resumed = false
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (resumed) return
                    resumed = true
                    sm.unregisterListener(this)
                    cont.resume(event?.values?.firstOrNull())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            val registered = sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            if (!registered && !resumed) {
                resumed = true
                cont.resume(null)
            }
        }
    }

    private fun mapWmoWeatherCode(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Cloudy"
    }
}
