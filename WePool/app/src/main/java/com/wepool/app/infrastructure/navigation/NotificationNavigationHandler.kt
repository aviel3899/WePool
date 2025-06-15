package com.wepool.app.infrastructure.navigation

import android.content.Context
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.notifications.NotificationHelper

suspend fun handleNotificationNavigation(
    context: Context,
    navController: NavController
): Boolean {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
    val uid = currentUser.uid
    val (screen, rideId) = NotificationHelper.getStoredNotificationData(context)
    if (screen.isNullOrEmpty() || rideId.isNullOrEmpty()) return false

    NotificationHelper.clearNotificationData(context)

    val user = RepositoryProvider.provideUserRepository().getUser(uid)
    val ride = RepositoryProvider.provideRideRepository().getRide(rideId)
    val isDriver = ride?.driverId == uid
    val isPassenger = ride?.passengers?.contains(uid) == true

    val route = when (screen) {
        "rideStarted", "pickup", "dropoff", "rideUpdated" -> when {
            isPassenger -> "passengerActiveRides/$uid?rideId=$rideId"
            isDriver -> "driverActiveRides/$uid?rideId=$rideId"
            else -> "passengerMenu/$uid"
        }

        "rideCancelled" -> "passengerPendingRequests/$uid?rideId=$rideId"

        "pendingRequests" -> if (user?.roles?.contains(UserRole.DRIVER) == true)
            "driverPendingRequests/$uid?rideId=$rideId"
        else
            "passengerPendingRequests/$uid"

        else -> if (user?.roles?.contains(UserRole.DRIVER) == true)
            "driverMenu/$uid"
        else
            "passengerMenu/$uid"
    }

    navController.navigate(route) {
        popUpTo("login") { inclusive = true }
        launchSingleTop = true
    }

    return true
}
