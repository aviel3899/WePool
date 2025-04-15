package com.wepool.app.data.model.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.enums.UserRole

object PolylineDecoder {

    // טולרנס גלובלי לפישוט מסלולים (ברירת מחדל: 50 מטר)
    private var toleranceMeters = 50.0

    // Getter ציבורי לקריאה בלבד
    val tolerance: Double
        get() = toleranceMeters

    // מאפשר לעדכן את הטולרנס רק למשתמש עם תפקיד ADMIN
    fun updateToleranceMeters(newTolerance: Double, user: User) {
        val isAdmin = user.roles.any { it == UserRole.ADMIN.name }

        if (!isAdmin) {
            throw SecurityException("Unauthorized: Only Admin can update tolerance.")
        }

        toleranceMeters = newTolerance
    }

     // מפענח מחרוזת Polyline מקודדת לרשימת נקודות LatLng
    fun decode(encodedPolyline: String): List<LatLng> {
        return PolyUtil.decode(encodedPolyline)
    }


     // מפשט רשימת נקודות לפי טולרנס במטרים (פחות נקודות, אותה צורה)
    private fun simplifyRoute(
        route: List<LatLng>,
        toleranceMeters: Double = this.toleranceMeters
    ): List<LatLng> {
        return PolyUtil.simplify(route, toleranceMeters)
    }


     // מפשט מסלול מקודד ישירות לפי טולרנס במטרים
    fun decodeAndSimplify(
        encodedPolyline: String,
        toleranceMeters: Double = this.toleranceMeters
    ): List<LatLng> {
        val decoded = decode(encodedPolyline)
        return simplifyRoute(decoded, toleranceMeters)
    }


     // בודק אם מיקום מסוים (currentLocation) נמצא במרחק סביר מהמסלול
    fun isOffRoute(
        currentLocation: LatLng,
        route: List<LatLng>,
        maxAllowedDeviationMeters: Double
    ): Boolean {
        val closestDistance = route.minOf { point ->
            com.google.maps.android.SphericalUtil.computeDistanceBetween(currentLocation, point)
        }
        return closestDistance > maxAllowedDeviationMeters
    }
}
