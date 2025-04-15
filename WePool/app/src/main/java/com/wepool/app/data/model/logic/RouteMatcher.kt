package com.wepool.app.data.model.logic

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.users.User
import com.wepool.app.data.remote.IGoogleMapsService
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RouteMatcher {

    // מרחק במטרים
    private var maxDistanceMeters = 2000.0
    // מהירות במטרים לדקה
    private var averageSpeedMetersPerMinute = 600.0

    val maxAllowedDistance: Double
        get() = maxDistanceMeters

    val averageSpeed: Double
        get() = averageSpeedMetersPerMinute


    fun updateMaxDistanceMeters(newDistance: Double, user: User) {
        val isAdmin = user.roles.any { it == UserRole.ADMIN.name }

        if (!isAdmin) {
            throw SecurityException("Unauthorized: Only Admin can update max distance.")
        }

        if (newDistance < 100.0) {
            throw IllegalArgumentException("Distance must be at least 100 meters.")
        }

        maxDistanceMeters = newDistance
    }

    fun updateAverageSpeedMetersPerMinute(newSpeedInKilometerPerHour: Double, user: User) {
        val isAdmin = user.roles.any { it == UserRole.ADMIN.name }

        if (!isAdmin) {
            throw SecurityException("Unauthorized: Only Admin can update average speed.")
        }

       val newSpeedInMetersPerMinute = convertKmHtoMetersPerMinute(newSpeedInKilometerPerHour)

        if (newSpeedInMetersPerMinute < 40.0) {
            throw IllegalArgumentException("Speed must be at least 40 m/min.")
        }

        averageSpeedMetersPerMinute = newSpeedInMetersPerMinute
    }

    private fun convertKmHtoMetersPerMinute(speedKmH: Double): Double {
        return (speedKmH * 1000) / 60.0
    }

    /*
      בדיקה האם הסטייה מהמסלול לנקודת האיסוף חוקית מבחינת סטייה מקסימלית לפי הלשבים הבאים:
      1. פישוט המסלול לקוארדינטות סמוכות
      2. שמירה של קוארדינטות קרובות מבחינה גיאוגרפית לנקודת האיסוף לפי מרחק אווירי
      3. בדיקה לא מקוונת (לפי מרחק אווירי ומהירות ממוצעת) האם הסטייה חוקית
      4. אם הבדיקה הקודמת עברה אז אפשר לבצע בדיקה מקוונת לפי api האם הסטייה חוקית
     */
    suspend fun isPickupWithinDriverDetour(
        encodedPolyline: String,
        pickupPoint: LatLng,
        maxAllowedDetourMinutes: Double,
        arrivalTime: String,
        mapsService: IGoogleMapsService,
        averageSpeedMetersPerMinute: Double = this.averageSpeedMetersPerMinute
    ): Boolean {
        val simplifiedRoute = PolylineDecoder.decodeAndSimplify(encodedPolyline)
        val candidatePoints = getNearbyPoints(simplifiedRoute, pickupPoint)

        Log.d("RouteMatcher", "🛑 סטייה מותרת מקסימלית: $maxAllowedDetourMinutes דקות")
        Log.d("RouteMatcher", "🔍 נמצאו ${candidatePoints.size} נקודות פוטנציאליות לבדיקה")

        for (point in candidatePoints) {
            val estimatedDetourMinutes = estimateOfflineDetourMinutes(point, pickupPoint, averageSpeedMetersPerMinute)
            if (estimatedDetourMinutes <= maxAllowedDetourMinutes) {
                val actualDetourMinutes = getActualDetourDuration(
                    point,
                    pickupPoint,
                    mapsService,
                    arrivalTime
                )
                Log.d("RouteMatcher", "🚗 סטייה בפועל: $actualDetourMinutes דק, (אומדן: $estimatedDetourMinutes)")
                if (actualDetourMinutes <= maxAllowedDetourMinutes) {
                    return true
                }
            }
            else {
                Log.d("RouteMatcher", "⛔️ סטייה גאומטרית $estimatedDetourMinutes דק > מותרת $maxAllowedDetourMinutes")
            }
        }
        return false
    }

    // מחזיר את הקוארדינטות שקרובות לנקודת האיסוף
    private fun getNearbyPoints(route: List<LatLng>, pickupPoint: LatLng): List<LatLng> {
        return route.filter {
            SphericalUtil.computeDistanceBetween(it, pickupPoint) <= maxDistanceMeters
        }
    }

    // מחזיר את הסטייה בדקות מהקוארדינטה לנקודת האיסוף לפי מרחק אווירי ומהירות ממוצעת
    private fun estimateOfflineDetourMinutes(
        routePoint: LatLng,
        pickupPoint: LatLng,
        speedMetersPerMinute: Double
    ): Double {
        val toPickup = SphericalUtil.computeDistanceBetween(routePoint, pickupPoint)
        val roundTrip = toPickup * 2 // הלוך וחזור
        return roundTrip / speedMetersPerMinute
    }

    // מחזיר את הסטייה האמיתית בדקות מהמסלול לנקודת האיסוף
    private suspend fun getActualDetourDuration(
        routePoint: LatLng,
        pickupPoint: LatLng,
        mapsService: IGoogleMapsService,
        arrivalTime: String
    ): Int = withContext(Dispatchers.IO) {
        val origin = GeoPoint(routePoint.latitude, routePoint.longitude)
        val destination = GeoPoint(pickupPoint.latitude, pickupPoint.longitude)

        try {
            val result = mapsService.getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
            return@withContext result.durationMinutes
        } catch (e: Exception) {
            Log.e("RouteMatcher", "❌ שגיאה בחישוב דרך ה-API: ${e.message}", e)
            return@withContext Int.MAX_VALUE // מחזיר ערך גבוה כדי שייפסל
        }
    }
}