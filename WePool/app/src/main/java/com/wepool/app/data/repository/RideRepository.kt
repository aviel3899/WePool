package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.notifications.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class RideRepository(
    private val firestore: FirebaseFirestore,
    private val mapsService: IGoogleMapsService,
    private val rideRequestRepository: IRideRequestRepository
) : IRideRepository {

    private val rideCollection = firestore.collection("rides")
    companion object {
        private const val SAFE_ARRIVAL_MARGIN_MINUTES = 10L
    }

    override suspend fun createRide(ride: Ride) {
        rideCollection.document(ride.rideId).set(ride).await()
    }

    override suspend fun getRide(rideId: String): Ride? = withContext(Dispatchers.IO) {
        val snapshot = rideCollection.document(rideId).get().await()
        return@withContext snapshot.toObject(Ride::class.java)
    }

    override suspend fun getAllRides(): List<Ride> = withContext(Dispatchers.IO) {
        val snapshot = rideCollection.get().await()  // שליפת כל המסמכים מתוך האוסף "rides"
        return@withContext snapshot.documents.mapNotNull { it.toObject(Ride::class.java) }  // המרת המסמכים למודל Ride
    }

    override suspend fun getRidesByDriver(driverId: String): List<Ride> = withContext(Dispatchers.IO) {
        val snapshot = rideCollection
            .whereEqualTo("driverId", driverId)
            .get()
            .await()
        return@withContext snapshot.toObjects(Ride::class.java)
    }

    override suspend fun getRidesByCompanyAndDirection(companyId: String, direction: RideDirection): List<Ride> {
        return rideCollection
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("direction", direction)
            .get()
            .await()
            .toObjects(Ride::class.java)
    }

    override fun getPickupTimeForPassenger(ride: Ride, passengerId: String): String? {
        return ride.pickupStops
            .firstOrNull { it.passengerId == passengerId }
            ?.pickupTime
    }

    override fun getDropoffTimeForPassenger(ride: Ride, passengerId: String): String? {
        return ride.pickupStops
            .firstOrNull { it.passengerId == passengerId }
            ?.dropoffTime
    }

    override suspend fun deactivateExpiredRides(): Unit = withContext(Dispatchers.IO) {
        try {
            val activeRidesSnapshot = firestore.collection("rides")
                .whereEqualTo("active", true)
                .get()
                .await()

            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val today = LocalDate.now()
            val now = LocalTime.now()

            for (doc in activeRidesSnapshot.documents) {
                val ride = doc.toObject(Ride::class.java) ?: continue
                val rideDate = LocalDate.parse(ride.date, dateFormatter)
                val arrivalTimeStr = ride.arrivalTime ?: continue
                val arrivalTime = LocalTime.parse(arrivalTimeStr, formatter)

                val safeArrivalTime = arrivalTime.plusMinutes(SAFE_ARRIVAL_MARGIN_MINUTES)

                val isExpired = rideDate.isBefore(today) ||
                        (rideDate.isEqual(today) && safeArrivalTime.isBefore(now))

                if (isExpired) {
                    firestore.collection("rides")
                        .document(ride.rideId)
                        .update("active", false)
                        .await()

                    RepositoryProvider.provideDriverRepository()
                        .removeActiveRideFromDriver(ride.driverId, ride.rideId)

                    val passengerRepo = RepositoryProvider.providePassengerRepository()
                    for (passengerId in ride.passengers) {
                        passengerRepo.removeActiveRideFromPassenger(passengerId, ride.rideId)
                    }

                    Log.d("RideRepo", "🛑 נסיעה ${ride.rideId} כובתה והוסרה מכל משתמש")
                }
            }
        } catch (e: Exception) {
            Log.e("RideRepo", "❌ שגיאה בבדיקת תוקף נסיעות: ${e.message}", e)
        }
    }

    override suspend fun approvePassengerRequest(
        candidate: RideCandidate,
        requestId: String,
        passengerId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val ride = candidate.ride
        val detour = candidate.detourEvaluationResult

        val rideRef = rideCollection.document(ride.rideId)
        val requestRef = rideRef.collection("requests").document(requestId)

        try {
            val success = firestore.runTransaction { tx ->
                val rideSnapshot = tx.get(rideRef)
                val currentRide = rideSnapshot.toObject(Ride::class.java)
                    ?: return@runTransaction false

                val requestSnapshot = tx.get(requestRef)
                val request = requestSnapshot.toObject(RideRequest::class.java)
                    ?: return@runTransaction false

                val pickupLocation = request.pickupLocation

                val detourEvaluationResult = request.detourEvaluationResult

                val pickupStop = PickupStop(
                    location = pickupLocation,
                    passengerId = passengerId,
                    pickupTime = if (ride.direction == RideDirection.TO_WORK)
                        detourEvaluationResult.pickupLocation?.pickupTime else null,
                    dropoffTime = if (ride.direction == RideDirection.TO_HOME)
                        detourEvaluationResult.pickupLocation?.dropoffTime else null
                )

                val updatedRide = currentRide.copy(
                    passengers = currentRide.passengers + passengerId,
                    occupiedSeats = currentRide.occupiedSeats + 1,
                    currentDetourMinutes = currentRide.currentDetourMinutes + detour.addedDetourMinutes,
                    encodedPolyline = detour.encodedPolyline ?: currentRide.encodedPolyline,
                    pickupStops = currentRide.pickupStops + pickupStop,
                    departureTime = if (ride.direction == RideDirection.TO_WORK)
                        detour.updatedReferenceTime ?: currentRide.departureTime
                    else currentRide.departureTime,
                    arrivalTime = if (ride.direction == RideDirection.TO_HOME)
                        detour.updatedReferenceTime ?: currentRide.arrivalTime
                    else currentRide.arrivalTime,
                    isActive = true
                )

                tx.set(rideRef, updatedRide)
                true
            }.await()

            if (success) {
                rideRequestRepository.updateRequestStatus(
                    ride.rideId,
                    requestId,
                    RequestStatus.ACCEPTED
                )
                Log.d("RideRequest", "📥 סטטוס הבקשה עודכן ל-ACCEPTED (requestId=$requestId)")

                RepositoryProvider.providePassengerRepository()
                    .addActiveRideToPassenger(passengerId, ride.rideId) // הוספה של הנסיעה לרשימת הנסיעות הפעילות של הנוסע
            }

            return@withContext success

        } catch (e: Exception) {
            Log.e("RideJoin", "❌ שגיאה באישור הבקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun declineAndDeleteRideRequest(
        rideId: String,
        requestId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            //  עדכון סטטוס הבקשה ל-DECLINED
            val statusUpdated = rideRequestRepository.updateRequestStatus(
                rideId = rideId,
                requestId = requestId,
                newStatus = RequestStatus.DECLINED
            )

            if (!statusUpdated) {
                Log.w("RideRequest", "⚠ לא ניתן לעדכן סטטוס ל-DECLINED (requestId=$requestId)")
                return@withContext false
            }

            //  מחיקת הבקשה ע"י הפונקציה מ-RideRequestRepository
            //rideRequestRepository.deleteRequest(rideId, requestId)

            Log.d("RideRequest", "🗑 בקשה נדחתה ונמחקה בהצלחה (requestId=$requestId)")
            return@withContext true

        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בדחיית/מחיקת בקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun cancelAndDeleteRideRequest(
        rideId: String,
        requestId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val statusUpdated = rideRequestRepository.updateRequestStatus(
                rideId = rideId,
                requestId = requestId,
                newStatus = RequestStatus.CANCELLED
            )

            if (!statusUpdated) {
                Log.w("RideRequest", "⚠ לא ניתן לעדכן סטטוס ל-CANCELLED (requestId=$requestId)")
                return@withContext false
            }

            //rideRequestRepository.deleteRequest(rideId, requestId)

            Log.d("RideRequest", "🚫 הבקשה בוטלה ונמחקה בהצלחה (requestId=$requestId)")
            return@withContext true

        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בביטול/מחיקת בקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun removePassengerFromRide(
        rideId: String,
        passengerId: String
    ): Unit = withContext(Dispatchers.IO) {
        val docRef = rideCollection.document(rideId)

        val originalRide = firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val ride = snapshot.toObject(Ride::class.java)

            if (ride == null || !ride.passengers.contains(passengerId)) {
                Log.w("RideLeave", "⚠ הנוסע $passengerId לא נמצא בנסיעה (rideId: $rideId)")
                return@runTransaction null
            }

            return@runTransaction ride
        }.await()

        if (originalRide == null) return@withContext

        try {
            val updatedRide = buildRideAfterPassengerRemoved(originalRide, passengerId)
            rideCollection.document(rideId).set(updatedRide).await()

            //deletePassengerRequestsForRide(rideId, passengerId)

            RepositoryProvider.providePassengerRepository()
                .removeActiveRideFromPassenger(passengerId, rideId) // מחיקה של הנסיעה מרשימת הנסיעות הפעילות של הנוסע

            Log.d("RideLeave", "✅ הנוסע $passengerId הוסר והמסלול עודכן (rideId=$rideId)")

            NotificationService.notifyRideUpdated(rideId)

        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה בהסרת נוסע: ${e.message}", e)
        }
    }

    private suspend fun buildRideAfterPassengerRemoved(
        ride: Ride,
        passengerId: String
    ): Ride {
        val updatedPassengers = ride.passengers - passengerId
        val updatedOccupiedSeats = maxOf(ride.occupiedSeats - 1, 0)

        val updatedPickupStops = ride.pickupStops.filterNot { it.passengerId == passengerId }

        val timeFlag: Boolean
        val timeReference = if (ride.direction == RideDirection.TO_WORK) {
            timeFlag = true
            ride.arrivalTime
        } else {
            timeFlag = false
            ride.departureTime
        }

        val route = if (updatedPickupStops.isNotEmpty()) {
            mapsService.getDurationAndRouteWithWaypoints(
                origin = ride.startLocation.geoPoint,
                waypoints = updatedPickupStops,
                destination = ride.destination.geoPoint,
                timeReference = timeReference!!,
                date = ride.date,
                direction = ride.direction!!,
                passengerStop = null
            )
        } else {
            mapsService.getDurationAndRouteFromGoogleApi(
                origin = ride.startLocation.geoPoint,
                destination = ride.destination.geoPoint,
                timeReference = timeReference!!,
                date = ride.date,
                direction = ride.direction!!
            )
        }

        val updatedTimeReference = adjustTimeAccordingToDirection(
            time = timeReference,
            minutes = route.durationMinutes,
            direction = ride.direction
        )

        val previousDetourMinutes = calculateTimeDifferenceInMinutes(
            ride.arrivalTime!!,
            ride.departureTime!!
        )
        Log.d("RideUpdate", "📏 סטייה קודמת: $previousDetourMinutes דקות")

        val newDetourMinutes = if (timeFlag) {
            calculateTimeDifferenceInMinutes(ride.departureTime, updatedTimeReference)
        } else {
            calculateTimeDifferenceInMinutes(ride.arrivalTime, updatedTimeReference)
        }
        Log.d("RideUpdate", "📏 סטייה חדשה: $newDetourMinutes דקות")

        val updatedDetourMinutes = ride.currentDetourMinutes - newDetourMinutes
        Log.d("RideUpdate", "🔁 עדכון סטייה מצטברת: $updatedDetourMinutes דקות")

        val baseUpdatedRide = ride.copy(
            passengers = updatedPassengers,
            occupiedSeats = updatedOccupiedSeats,
            pickupStops = updatedPickupStops,
            encodedPolyline = route.encodedPolyline,
            currentDetourMinutes = updatedDetourMinutes
        )

        return if (ride.direction == RideDirection.TO_WORK) {
            baseUpdatedRide.copy(departureTime = updatedTimeReference)
        } else {
            baseUpdatedRide.copy(arrivalTime = updatedTimeReference)
        }
    }

    private suspend fun getPickupLocationFromRequest(
        rideId: String,
        passengerId: String
    ): LocationData? {
        return try {
            val snapshot = rideCollection.document(rideId)
                .collection("requests")
                .whereEqualTo("passengerId", passengerId)
                .get()
                .await()

            snapshot.documents.firstOrNull()
                ?.toObject(RideRequest::class.java)
                ?.pickupLocation
        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה בשליפת נקודת איסוף: ${e.message}", e)
            null
        }
    }

    private suspend fun deletePassengerRequestsForRide(rideId: String, passengerId: String) {
        try {
            val snapshot = firestore.collection("rides")
                .document(rideId)
                .collection("requests")
                .whereEqualTo("passengerId", passengerId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val requestId = doc.id
                firestore.collection("rides")
                    .document(rideId)
                    .collection("requests")
                    .document(requestId)
                    .delete()
                    .await()

                Log.d("RideLeave", "🗑 בקשה $requestId נמחקה")
            }

            Log.d("RideLeave", "✅ כל בקשות ההצטרפות של $passengerId לנסיעה $rideId נמחקו")
        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה במחיקת בקשות נוסע: ${e.message}", e)
        }
    }

    // מעדכן נסיעה קיימת
    override suspend fun updateRide(ride: Ride)  {
        rideCollection.document(ride.rideId).set(ride).await()
    }

    // מוחק נסיעה
    override suspend fun deleteRide(rideId: String) = withContext(Dispatchers.IO) {
        try {
            val rideSnapshot = firestore.collection("rides").document(rideId).get().await()
            val ride = rideSnapshot.toObject(Ride::class.java)

            if (ride == null) {
                Log.w("RideDelete", "⚠ לא נמצאה נסיעה למחיקה (rideId=$rideId)")
                return@withContext
            }

            if (ride.passengers.isNotEmpty()) {
                Log.d("RideDelete", "🔁 התחלת הסרת ${ride.passengers.size} נוסעים מהנסיעה")

                for (passengerId in ride.passengers.toList()) {
                    try {
                        removePassengerFromRide(rideId, passengerId)
                    } catch (e: Exception) {
                        Log.e("RideDelete", "❌ שגיאה בהסרת נוסע $passengerId: ${e.message}", e)
                    }
                }

                Log.d("RideDelete", "✅ כל הנוסעים הוסרו מהנסיעה (rideId=$rideId)")
            }

            firestore.collection("rides").document(rideId).delete().await()
            Log.d("RideDelete", "🗑 הנסיעה נמחקה בהצלחה (rideId=$rideId)")

            val driverId = ride.driverId
            val driverRepository = RepositoryProvider.provideDriverRepository()
            driverRepository.removeActiveRideFromDriver(driverId, rideId)

        } catch (e: Exception) {
            Log.e("RideDelete", "❌ שגיאה במחיקת נסיעה: ${e.message}", e)
        }
    }


override suspend fun updateAvailableSeats(rideId: String, seats: Int) {
        firestore.collection("rides")
            .document(rideId)
            .update("availableSeats", seats)
            .await()
    }

    override suspend fun updateMaxDetourMinutes(rideId: String, maxDetour: Int) {
        firestore.collection("rides")
            .document(rideId)
            .update("maxDetourMinutes", maxDetour)
            .await()
    }

    override suspend fun updateArrivalTime(rideId: String, time: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("arrivalTime", time)
            .await()
    }

    override suspend fun updateDestination(rideId: String, destination: GeoPoint) {
        firestore.collection("rides")
            .document(rideId)
            .update("destination", destination)
            .await()
    }

    override suspend fun updateDepartureTime(rideId: String, departureTime: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("departureTime", departureTime)
            .await()
    }

    override suspend fun updateRideDate(rideId: String, date: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("date", date)
            .await()
    }

    override suspend fun updateEncodedPolyline(rideId: String, encodedPolyline: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("encodedPolyline", encodedPolyline)
            .await()
    }

    override suspend fun updateRideIsActive(rideId: String, isActive: Boolean) {
        firestore.collection("rides")
            .document(rideId)
            .update("isActive", isActive)
            .await()
    }

    override suspend fun planRideFromUserInput(
        driverId: String,
        companyId: String,
        startAddress: LocationData,
        destinationAddress: LocationData,
        arrivalTime: String,
        departureTime: String,
        date: String,
        direction: RideDirection,
        availableSeats: Int,
        occupiedSeats: Int,
        maxDetourMinutes: Int,
        notes: String
    ): Boolean = withContext(Dispatchers.IO) {
        val existingRides = getRidesByDriver(driverId)
        val alreadyExists = existingRides.any {
            it.direction == direction &&
                    it.arrivalTime == arrivalTime &&
                    it.date == date
        }

        if (alreadyExists) {
            Log.d("RideLogic", "⚠ נסיעה כזו כבר קיימת — לא נוצרת חדשה")
            return@withContext false
        }

        val startLocation = startAddress.geoPoint
        val destination = destinationAddress.geoPoint

        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        var adjustedArrivalTimeStr = arrivalTime // ברירת מחדל – לא משתנה אם הכיוון אינו TO_WORK

        if (direction == RideDirection.TO_WORK) {
            val adjustedArrivalTime = LocalTime
                .parse(arrivalTime, formatter)
                .minusMinutes(maxDetourMinutes.toLong())

            adjustedArrivalTimeStr = adjustedArrivalTime.format(formatter)
        }

        val rideId = firestore.collection("rides").document().id

        val baseRide = Ride(
            rideId = rideId,
            driverId = driverId,
            companyId = companyId,
            startLocation = startAddress,
            destination = destinationAddress,
            direction = direction,
            arrivalTime = adjustedArrivalTimeStr,
            departureTime = departureTime.format(formatter),
            date = date,
            availableSeats = availableSeats,
            occupiedSeats = occupiedSeats,
            passengers = emptyList(),
            pickupStops = emptyList(),
            maxDetourMinutes = maxDetourMinutes,
            currentDetourMinutes = 0,
            encodedPolyline = "",
            isActive = true,
            notes = notes
        )

        val timeReference = if (direction == RideDirection.TO_WORK) {
            adjustedArrivalTimeStr
        } else {
            departureTime
        }

        val result = try {
            calculateRideDepartureTime(
                ride = baseRide,
                origin = startLocation,
                destination = destination,
                timeReference = timeReference,
                date = date
            )
        } catch (e: Exception) {
            if(direction == RideDirection.TO_WORK)
                Log.e("RideLogic", "❌ שגיאה בחישוב זמן היציאה: ${e.message}", e)
            else
                Log.e("RideLogic", "❌ שגיאה בחישוב זמן הההגעה: ${e.message}", e)
            return@withContext false
        }

        val finalizedRide = baseRide.copy(
            arrivalTime = result.arrivalTime,
            departureTime = result.departureTime,
            encodedPolyline = result.encodedPolyline
        )

        createRide(finalizedRide)

        val driverRepository = RepositoryProvider.provideDriverRepository()
        driverRepository.addActiveRideToDriver(driverId, finalizedRide.rideId)

        Log.d("RideLogic", "✅ נסיעה נוצרה בהצלחה: ${finalizedRide.rideId}")
        return@withContext true
    }

    override suspend fun calculateRideDepartureTime(
        ride: Ride,
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String
    ): DepartureCalculationResult {
        val result = getDurationAndRouteFromGoogleApi(origin, destination, timeReference, date,  ride.direction!!)
        val timeInHours = adjustTimeAccordingToDirection(
            time = timeReference,
            minutes = result.durationMinutes,
            direction = ride.direction
        )
        if(ride.direction == RideDirection.TO_WORK)
            return DepartureCalculationResult(timeInHours, timeReference ,result.encodedPolyline)
        else
            return DepartureCalculationResult(timeReference, timeInHours ,result.encodedPolyline)
    }

    private suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String,
        direction: RideDirection
    ): DurationAndRoute {
        return mapsService.getDurationAndRouteFromGoogleApi(origin, destination, timeReference, date, direction)
    }

    override suspend fun adjustTimeAccordingToDirection(time: String, minutes: Int, direction: RideDirection): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // הגדרת פורמט זמן
        val localTime = LocalTime.parse(time, formatter) // הופך את המחרוזת של time לאובייקט מסוג localTime
        val adjustedTime = when (direction) {
            RideDirection.TO_WORK -> localTime.minusMinutes(minutes.toLong())
            RideDirection.TO_HOME -> localTime.plusMinutes(minutes.toLong())
        }
        return adjustedTime.format(formatter)
    }

    private suspend fun calculateTimeDifferenceInMinutes(startTime: String, endTime: String): Int {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val start = LocalTime.parse(startTime, formatter)
        val end = LocalTime.parse(endTime, formatter)

        return abs(Duration.between(start, end).toMinutes().toInt())
    }
}