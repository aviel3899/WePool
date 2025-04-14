package com.wepool.app.data.remote

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.remote.DirectionsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.*
import android.util.Log

class GoogleMapsService(
    private val apiKey: String // מוזן מתוך BuildConfig.MAPS_API_KEY
) : IGoogleMapsService {

    private val client = OkHttpClient()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * מחשב את משך זמן הנסיעה (בדקות) בין origin ל-destination
     * לפי זמן הגעה רצוי (arrivalTime בפורמט HH:mm)
     */
    override suspend fun getDurationFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): Int = withContext(Dispatchers.IO) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        val arrivalEpochSeconds = convertTimeToEpoch(arrivalTime)

        val url = buildString {
            append("https://maps.googleapis.com/maps/api/directions/json?")
            append("origin=$originStr&")
            append("destination=$destinationStr&")
            append("arrival_time=$arrivalEpochSeconds&")
            append("mode=driving&")
            append("key=$apiKey")
        }

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Google API")

        val directions = jsonParser.decodeFromString<DirectionsResponse>(body)
        val durationSeconds = directions.routes.firstOrNull()
            ?.legs?.firstOrNull()
            ?.duration?.value
            ?: throw Exception("Duration not found in API response")

        return@withContext durationSeconds / 60
    }

    /**
     * ממיר מחרוזת זמן בפורמט "HH:mm" ל־Epoch Time (שניות)
     */
    private fun convertTimeToEpoch(time: String): Long {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val localTime = LocalTime.parse(time, formatter)
        val today = LocalDate.now()
        val dateTime = LocalDateTime.of(today, localTime)
        val zone = ZoneId.systemDefault()
        return dateTime.atZone(zone).toEpochSecond()
    }
}


