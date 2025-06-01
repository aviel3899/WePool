package com.wepool.app.data.model.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.enums.user.UserRole

object PolylineDecoder {

    // טולרנס גלובלי לפישוט מסלולים (ברירת מחדל: 50 מטר)
    private var toleranceMeters = 50.0

    // Getter ציבורי לקריאה בלבד
    val tolerance: Double
        get() = toleranceMeters

    // מאפשר לעדכן את הטולרנס רק למשתמש עם תפקיד ADMIN
    fun updateToleranceMeters(newTolerance: Double, user: User) {
        val isAdmin = UserRole.ADMIN in user.roles

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
        toleranceMeters: Double? = null
    ): List<LatLng> {
        val effectiveTolerance = toleranceMeters ?: this.toleranceMeters
        return PolyUtil.simplify(route, effectiveTolerance)
    }

    // מפשט מסלול מקודד ישירות לפי טולרנס במטרים
    fun decodeAndSimplify(
        encodedPolyline: String,
        toleranceMeters: Double? = null
    ): List<LatLng> {
        val effectiveTolerance = toleranceMeters ?: this.toleranceMeters
        val decoded = decode(encodedPolyline)
        return simplifyRoute(decoded, effectiveTolerance)
    }
}