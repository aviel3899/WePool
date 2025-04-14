package com.wepool.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Driver
//import com.wepool.app.data.model.users.PassengerRideInfo
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DriverRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mapsService: IGoogleMapsService
) : IDriverRepository {

    // יוצרת או מעדכנת את נתוני הנהג במסמך info
    override suspend fun saveDriver(driver: Driver) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .set(driver)
            .await()
    }

    // מחזירה את אובייקט הנהג לפי uid
    override suspend fun getDriver(uid: String): Driver? {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .get()
            .await()
        return snapshot.toObject(Driver::class.java)
    }

    // מוחקת את מסמך הנהג
    override suspend fun deleteDriver(uid: String) {
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .delete()
            .await()
    }

    // עדכון כללי - תשתית לעדכון שדות נפרדים
    private suspend fun updateDriverFields(uid: String, updates: Map<String, Any?>)  {
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .update(updates)
            .await()
    }

    // פונקציות לעדכון שדה אחד בודד
    override suspend fun updateAvailableSeats(uid: String, seats: Int) {
        updateDriverFields(uid, mapOf("availableSeats" to seats))
    }

    override suspend fun updateVehicleDetails(uid: String, vehicleDetails: String) {
        updateDriverFields(uid, mapOf("vehicleDetails" to vehicleDetails))
    }

    override suspend fun updateMaxDetourMinutes(uid: String, maxDetour: Int) {
        updateDriverFields(uid, mapOf("maxDetourMinutes" to maxDetour))
    }

    override suspend fun updatePreferredArrivalTime(uid: String, time: String) {
        updateDriverFields(uid, mapOf("preferredArrivalTime" to time))
    }

    override suspend fun updateCalculatedDepartureTime(uid: String, time: String) {
        updateDriverFields(uid, mapOf("calculatedDepartureTime" to time))
    }

    override suspend fun updateDirection(uid: String, direction: String) {
        updateDriverFields(uid, mapOf("direction" to direction))
    }

    override suspend fun updateDestination(uid: String, destination: GeoPoint) {
        updateDriverFields(uid, mapOf("destination" to destination))
    }

    override suspend fun updateActiveRideId(uid: String, rideId: String?) {
        updateDriverFields(uid, mapOf("activeRideId" to rideId))
    }

    // מחזיר את השעה הנוכחית בפורמט טקסטואלי
    private fun getCurrentTimestamp(): String {
        return LocalTime.now().toString()
    }

    override suspend fun calculateDepartureTimeFromArrival(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DepartureCalculationResult {
        val result = getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
        val totalMinutesBefore = result.durationMinutes + 10
        val departureTime = subtractMinutesFromTime(arrivalTime, totalMinutesBefore)
        return DepartureCalculationResult(departureTime, result.encodedPolyline)
    }

    private suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DurationAndRoute {
        return mapsService.getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
    }

    private fun subtractMinutesFromTime(time: String, minutes: Int): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // הגדרת פורמט זמן
        val localTime = LocalTime.parse(time, formatter) // הופך את המחרוזת של time לאובייקט מסוג localTime
        val adjustedTime = localTime.minusMinutes(minutes.toLong()) // מפחית את כמות הדקות מהשעה המקורית
        return adjustedTime.format(formatter) // מחזיר את השעה החדשה כמחרוזת בפורמט "HH:mm"
    }

    // עדכון נסיעה קיימת עם נוסע חדש תוך בדיקות
    /*suspend fun updateRideWithPassenger(rideId: String, passengerInfo: PassengerRideInfo): Boolean {
        val rideRef = firestore.collection("rides").document(rideId)

        return firestore.runTransaction { tx ->
            val snapshot = tx.get(rideRef)
            val ride = snapshot.toObject(Ride::class.java) ?: return@runTransaction false

            if (ride.availableSeats <= ride.passengers.size) return@runTransaction false
            if (passengerInfo.detourMinutes > ride.maxDetourMinutes) return@runTransaction false

            val updatedPassengers = ride.passengers + passengerInfo
            val updatedRide = ride.copy(passengers = updatedPassengers)

            tx.set(rideRef, updatedRide)
            true
        }.await()
    }*/
}