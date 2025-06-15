package com.wepool.app.data.model.logic

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.users.User
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.ride.PickupStop
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
        val isAdmin = UserRole.ADMIN in user.roles

        if (!isAdmin) {
            throw SecurityException("Unauthorized: Only Admin can update max distance.")
        }

        if (newDistance < 100.0) {
            throw IllegalArgumentException("Distance must be at least 100 meters.")
        }

        maxDistanceMeters = newDistance
    }

    fun updateAverageSpeedMetersPerMinute(newSpeedInKilometerPerHour: Double, user: User) {
        val isAdmin = UserRole.ADMIN in user.roles

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
    suspend fun evaluatePickupDetour(
        encodedPolyline: String,
        pickupPoint: PickupStop,
        maxAllowedDetourMinutes: Int,
        currentDetourMinutes: Int,
        currentRouteTimeMinutes: Int,
        timeReference: String,
        date: String,
        mapsService: IGoogleMapsService,
        startLocation: GeoPoint,
        destination: GeoPoint,
        currentPickupStops: List<PickupStop>,
        rideRepository: IRideRepository,
        rideDirection: RideDirection,
        averageSpeedMetersPerMinute: Double = this.averageSpeedMetersPerMinute
    ): DetourEvaluationResult {
        val simplifiedRoute = PolylineDecoder.decodeAndSimplify(encodedPolyline)
        val candidatePoints = getNearbyPoints(simplifiedRoute, pickupPoint)

        Log.d("RouteMatcher", "🛑 סטייה מותרת מקסימלית: $maxAllowedDetourMinutes דקות")
        Log.d("RouteMatcher", "🔍 ${candidatePoints.size} נקודות סמוכות נבדקות")

        for (point in candidatePoints) {
            val estimated = estimateOfflineDetourMinutes(point, pickupPoint, averageSpeedMetersPerMinute)
            if (currentDetourMinutes + estimated <= maxAllowedDetourMinutes) {

                val routeAfter = try {
                    getActualDetourData(
                        start = startLocation,
                        destination = destination,
                        currentStops = currentPickupStops,
                        pickupLocation = pickupPoint,
                        timeReference = timeReference,
                        date = date,
                        direction = rideDirection,
                        mapsService = mapsService,
                    )
                } catch (e: Exception) {
                    Log.e("RouteMatcher", "❌ שגיאה בחישוב מסלול עם נקודת עצירה חדשה", e)
                    return DetourEvaluationResult(isAllowed = false)
                }

                val addedDetour = routeAfter.durationMinutes - currentRouteTimeMinutes
                Log.d("RouteMatcher", "🔍 זמן קודם: $currentRouteTimeMinutes, זמן חדש: ${routeAfter.durationMinutes}, תוספת: $addedDetour")

                val totalDetour = currentDetourMinutes + addedDetour
                Log.d("RouteMatcher", "🚗 זמן חדש: ${routeAfter.durationMinutes}, תוספת: $addedDetour, מצטבר: $totalDetour")

                if (totalDetour <= maxAllowedDetourMinutes) {
                    val updatedReferenceTime = rideRepository.adjustTimeAccordingToDirection(timeReference, routeAfter.durationMinutes, rideDirection)
                    return DetourEvaluationResult(
                        isAllowed = true,
                        pickupLocation = PickupStop(
                            location = pickupPoint.location,
                            passengerId = pickupPoint.passengerId,
                            pickupTime = routeAfter.pickupTimes[pickupPoint.passengerId],
                            dropoffTime = routeAfter.dropoffTimes[pickupPoint.passengerId]
                        ),
                        encodedPolyline = routeAfter.encodedPolyline,
                        addedDetourMinutes = addedDetour,
                        updatedReferenceTime = updatedReferenceTime,
                    )
                }
            }
        }
        return DetourEvaluationResult(isAllowed = false)
    }

    //  מחזיר את הקוארדינטות שקרובות לנקודת האיסוף לפי מרחק אווירי
    private fun getNearbyPoints(route: List<LatLng>, pickupStop: PickupStop): List<LatLng> {
        val pickupLatLng = LatLng(
            pickupStop.location.geoPoint.latitude,
            pickupStop.location.geoPoint.longitude
        )
        return route.filter {
            SphericalUtil.computeDistanceBetween(it, pickupLatLng) <= maxDistanceMeters
        }
    }

    // מחזיר את הסטייה בדקות מהקוארדינטה לנקודת האיסוף לפי מרחק אווירי ומהירות ממוצעת
    private fun estimateOfflineDetourMinutes(
        routePoint: LatLng,
        pickupStop: PickupStop,
        speedMetersPerMinute: Double
    ): Double {
        val pickupLatLng = LatLng(
            pickupStop.location.geoPoint.latitude,
            pickupStop.location.geoPoint.longitude
        )
        val toPickup = SphericalUtil.computeDistanceBetween(routePoint, pickupLatLng)
        val roundTrip = toPickup * 2 // הלוך וחזור
        return roundTrip / speedMetersPerMinute
    }


    // DurationAndRoute מחזיר גם את זמן הנסיעה החדש כולל הסטייה וגם את המסלול - מחזיר משתנה מסוג
    private suspend fun getActualDetourData(
        start: GeoPoint,
        destination: GeoPoint,
        currentStops: List<PickupStop>,
        pickupLocation: PickupStop,
        timeReference: String,
        date: String,
        direction: RideDirection,
        mapsService: IGoogleMapsService,
    ): DurationAndRoute = withContext(Dispatchers.IO) {
        return@withContext try {

            val updatedWaypoints = currentStops + pickupLocation
            val route = mapsService.getDurationAndRouteWithWaypoints(
                origin = start,
                waypoints = updatedWaypoints,
                destination = destination,
                timeReference = timeReference,
                date = date,
                direction = direction
            )
            Log.d("RouteMatcher", "🧭 מסלול חדש חושב בהצלחה - זמן כולל: ${route.durationMinutes} דקות")
            route

        } catch (e: Exception) {
            Log.e("RouteMatcher", "❌ שגיאה בחישוב סטייה עם Waypoints: ${e.message}", e)
            DurationAndRoute(Int.MAX_VALUE, "") // מחזיר ערך כושל לביטול התאמה
        }
    }

}