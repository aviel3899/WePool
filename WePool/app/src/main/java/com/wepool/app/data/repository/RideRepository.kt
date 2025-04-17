package com.wepool.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RideRepository(
    private val firestore: FirebaseFirestore,
    private val mapsService: IGoogleMapsService
) : IRideRepository {

    // מחזיר את הנתיב לcollection של נסיעות של נהג מסוים
    private fun driverRidesCollection(driverId: String) =
        firestore.collection("users")
            .document(driverId)
            .collection("rides")

    // יוצר נסיעה חדשה עבור נהג
    override suspend fun createRide(driverId: String, ride: Ride)  {
        driverRidesCollection(driverId)
            .document(ride.rideId)
            .set(ride)
            .await()
    }

    // מחזיר נסיעה מסוימת לפי rideId
    override suspend fun getRide(driverId: String, rideId: String): Ride? = withContext(Dispatchers.IO) {
        val snapshot = driverRidesCollection(driverId)
            .document(rideId)
            .get()
            .await()
        return@withContext snapshot.toObject(Ride::class.java)
    }

    // מחזיר את כל הנסיעות של נהג
    override suspend fun getRidesForDriver(driverId: String): List<Ride> = withContext(Dispatchers.IO) {
        val snapshot = driverRidesCollection(driverId).get().await()
        return@withContext snapshot.toObjects(Ride::class.java)
    }

    // מעדכן נסיעה קיימת
    override suspend fun updateRide(driverId: String, ride: Ride)  {
        driverRidesCollection(driverId)
            .document(ride.rideId)
            .set(ride)
            .await()
    }

    // מוחק נסיעה
    override suspend fun deleteRide(driverId: String, rideId: String)  {
        driverRidesCollection(driverId)
            .document(rideId)
            .delete()
            .await()
    }

    override suspend fun updateAvailableSeats(driverId: String, rideId: String, seats: Int) {
        driverRidesCollection(driverId)
            .document(rideId)
            .update("availableSeats", seats)
            .await()
    }

    override suspend fun updateMaxDetourMinutes(driverId: String, rideId: String, maxDetour: Int)  {
        driverRidesCollection(driverId)
            .document(rideId)
            .update("maxDetourMinutes", maxDetour)
            .await()
    }

    override suspend fun updatePreferredArrivalTime(driverId: String, rideId: String, time: String)  {
        driverRidesCollection(driverId)
            .document(rideId)
            .update("departureTime", time)
            .await()
    }

    override suspend fun updateDestination(driverId: String, rideId: String, destination: GeoPoint)  {
        driverRidesCollection(driverId)
            .document(rideId)
            .update("destination", destination)
            .await()
    }

    override suspend fun calculateRideDepartureTime(
        ride: Ride,
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DepartureCalculationResult {
        val result = getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
        val totalMinutesBefore = result.durationMinutes + ride.maxDetourMinutes
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


}
