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
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.logic.RouteMatcher
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
import java.time.LocalDateTime
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
        val snapshot = rideCollection.get().await()
        return@withContext snapshot.documents.mapNotNull { it.toObject(Ride::class.java) }
    }

    override suspend fun getRidesByDriver(driverId: String): List<Ride> =
        withContext(Dispatchers.IO) {
            val snapshot = rideCollection
                .whereEqualTo("driverId", driverId)
                .get()
                .await()
            return@withContext snapshot.toObjects(Ride::class.java)
        }

    override suspend fun getRidesByCompanyAndDirection(
        companyCode: String,
        direction: RideDirection
    ): List<Ride> {
        return rideCollection
            .whereEqualTo("companyCode", companyCode)
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

    override suspend fun getPastRidesAsDriver(uid: String): List<Ride> {
        return try {
            firestore.collection("rides")
                .whereEqualTo("driverId", uid)
                .whereEqualTo("active", false)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Ride::class.java)?.copy(rideId = it.id) }
        } catch (e: Exception) {
            Log.e("RideRepository", "❌ שגיאה ב־getPastRidesAsDriver: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getPastRidesAsPassenger(uid: String): List<Ride> {
        return try {
            val allRides = firestore.collection("rides")
                .whereEqualTo("active", false)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Ride::class.java)?.copy(rideId = it.id) }

            allRides.filter { ride ->
                ride.passengers.contains(uid)
            }
        } catch (e: Exception) {
            Log.e("RideRepository", "❌ שגיאה ב־getPastRidesAsPassenger: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun deactivateExpiredRides(): Unit = withContext(Dispatchers.IO) {
        try {
            val activeRidesSnapshot = firestore.collection("rides")
                .whereEqualTo("active", true)
                .get()
                .await()

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val nowDateTime = LocalDateTime.now()

            for (doc in activeRidesSnapshot.documents) {
                val ride = doc.toObject(Ride::class.java) ?: continue
                val rideDate = LocalDate.parse(ride.date, dateFormatter)

                val arrivalTimeStr = ride.arrivalTime ?: continue
                val arrivalTime = LocalTime.parse(arrivalTimeStr, timeFormatter)
                val arrivalDateTime = LocalDateTime.of(rideDate, arrivalTime)
                    .plusMinutes(SAFE_ARRIVAL_MARGIN_MINUTES)

                val isExpired = arrivalDateTime.isBefore(nowDateTime)

                if (isExpired) {
                    firestore.collection("rides").document(ride.rideId)
                        .update("active", false).await()

                    RepositoryProvider.provideDriverRepository()
                        .removeActiveRideFromDriver(ride.driverId, ride.rideId)

                    val passengerRepo = RepositoryProvider.providePassengerRepository()
                    for (passengerId in ride.passengers) {
                        passengerRepo.removeActiveRideFromPassenger(passengerId, ride.rideId)
                    }

                    Log.d("RideRepo", "🛑 נסיעה ${ride.rideId} כובתה והוסרה מכל משתמש")
                    continue // לא צריך לבדוק בקשות לנסיעה שפגה
                }

                // 🆕 טיפול בבקשות ממתינות
                val requestSnapshot = firestore.collection("rides")
                    .document(ride.rideId)
                    .collection("requests")
                    .whereEqualTo("status", RequestStatus.PENDING.name)
                    .get()
                    .await()

                val departureTimeStr = ride.departureTime ?: continue
                val departureTime = LocalTime.parse(departureTimeStr, timeFormatter)
                val departureDateTime = LocalDateTime.of(rideDate, departureTime)

                val minutesBefore = Duration.between(nowDateTime, departureDateTime).toMinutes()

                val declineThreshold = when (ride.direction) {
                    RideDirection.TO_WORK -> 60
                    RideDirection.TO_HOME -> 10
                    else -> continue
                }

                if (minutesBefore <= declineThreshold) {
                    for (requestDoc in requestSnapshot.documents) {
                        val requestId = requestDoc.id
                        val request = requestDoc.toObject(RideRequest::class.java)
                        val passengerId = request?.passengerId ?: continue

                        val success = declineRideRequest(ride.rideId, requestId)
                        if (success) {
                            Log.d(
                                "RideRepo",
                                "⏱ הבקשה $requestId של $passengerId נדחתה עקב קירבה לזמן יציאה"
                            )

                            NotificationService.notifyPassengers(
                                passengerIds = listOf(passengerId),
                                rideId = ride.rideId,
                                title = "⏳ הבקשה שלך בוטלה",
                                body = "הבקשה לנסיעה בוטלה אוטומטית מאחר ונותר זמן קצר מדי ליציאה.",
                                screen = "rideCancelled"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RideRepo", "❌ שגיאה בבדיקת תוקף נסיעות/בקשות: ${e.message}", e)
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
            val requestSnapshot = requestRef.get().await()
            val request = requestSnapshot.toObject(RideRequest::class.java)
                ?: return@withContext false

            val pickupLocation = request.pickupLocation

            val pickupStop = PickupStop(
                location = pickupLocation,
                passengerId = passengerId,
                pickupTime = if (ride.direction == RideDirection.TO_WORK)
                    detour.pickupLocation?.pickupTime else null,
                dropoffTime = if (ride.direction == RideDirection.TO_HOME)
                    detour.pickupLocation?.dropoffTime else null
            )

            val pickupStopsIncludingNew = ride.pickupStops + pickupStop
            val rideWithNewPassenger = ride.copy(
                passengers = ride.passengers + passengerId,
                occupiedSeats = ride.occupiedSeats + 1,
                encodedPolyline = detour.encodedPolyline ?: ride.encodedPolyline,
            )

            val originalDuration = calculateTimeDifferenceInMinutes(
                ride.originalRoute.departureTime,
                ride.originalRoute.arrivalTime
            )

            val updatedRide = rebuildRideWithNewStops(
                ride = rideWithNewPassenger,
                pickupStops = pickupStopsIncludingNew,
                originalDuration = originalDuration,
                originalDetour = ride.currentDetourMinutes,
                addingPassenger = true
            )

            val success = firestore.runTransaction { tx ->
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
                    .addActiveRideToPassenger(passengerId, ride.rideId)

                updatePendingRequestsTimesFromRide(updatedRide, originalDuration)
            }

            // 🔔 שליחת התראה אישית לנוסע המאושר
            NotificationService.notifyPassengers(
                passengerIds = listOf(passengerId),
                rideId = ride.rideId,
                title = "✅ הבקשה שלך אושרה!",
                body = "הצטרפת בהצלחה לנסיעה.",
                screen = "rideUpdated"
            )

            // 🔔 שליחת עדכון כללי לשאר הנוסעים בלבד (לא כולל המאשר והמאושר)
            val otherPassengers = updatedRide.passengers.filterNot { it == passengerId }
            if (otherPassengers.isNotEmpty()) {
                NotificationService.notifyPassengers(
                    passengerIds = otherPassengers,
                    rideId = ride.rideId,
                    title = "📢 בוצעו שינויים בנסיעה",
                    body = "פרטי הנסיעה עודכנו בעקבות צירוף נוסע חדש.",
                    screen = "rideUpdated"
                )
            }

            return@withContext success

        } catch (e: Exception) {
            Log.e("RideJoin", "❌ שגיאה באישור הבקשה: ${e.message}", e)
            return@withContext false
        }
    }

    private suspend fun updatePendingRequestsTimesFromRide(
        updatedRide: Ride,
        originalDuration: Int
    ) {
        val pendingRequests = rideRequestRepository.getRequestsForRide(updatedRide.rideId)
            .filter { it.status == RequestStatus.PENDING }

        for (request in pendingRequests) {
            try {
                val evaluation = RouteMatcher.evaluatePickupDetour(
                    encodedPolyline = updatedRide.encodedPolyline,
                    pickupPoint = PickupStop(
                        location = request.pickupLocation,
                        passengerId = request.passengerId
                    ),
                    maxAllowedDetourMinutes = updatedRide.maxDetourMinutes,
                    currentDetourMinutes = updatedRide.currentDetourMinutes,
                    currentRouteTimeMinutes = originalDuration,
                    timeReference = updatedRide.arrivalTime ?: updatedRide.departureTime!!,
                    date = updatedRide.date,
                    mapsService = mapsService,
                    startLocation = updatedRide.startLocation.geoPoint,
                    destination = updatedRide.destination.geoPoint,
                    currentPickupStops = updatedRide.pickupStops,
                    rideRepository = RepositoryProvider.provideRideRepository(),
                    rideDirection = updatedRide.direction!!
                )

                if (!evaluation.isAllowed) {
                    Log.d("RequestUpdate", "🚫 הבקשה ${request.requestId} נדחית: סטייה לא מותרת")
                    declineRideRequest(updatedRide.rideId, request.requestId)
                    continue
                }

                // אם עבר את ההערכה — נעדכן את DetourEvaluationResult
                rideRequestRepository.updateDetourEvaluationResult(
                    rideId = updatedRide.rideId,
                    requestId = request.requestId,
                    newDetour = evaluation
                )

                Log.d(
                    "RequestUpdate",
                    "✅ הבקשה ${request.requestId} עודכנה עם סטייה ${evaluation.addedDetourMinutes} דקות"
                )

            } catch (e: Exception) {
                Log.e("RequestUpdate", "❌ שגיאה בעיבוד הבקשה ${request.requestId}: ${e.message}", e)
            }
        }
    }

    private fun updatePickupStopTimesFromRouteResult(
        ride: Ride,
        route: DurationAndRoute
    ): List<PickupStop> {
        return route.orderedStops.map { stop ->
            stop.copy(
                pickupTime = if (ride.direction == RideDirection.TO_WORK)
                    route.pickupTimes[stop.passengerId] else stop.pickupTime,
                dropoffTime = if (ride.direction == RideDirection.TO_HOME)
                    route.dropoffTimes[stop.passengerId] else stop.dropoffTime
            )
        }
    }

    private suspend fun rebuildRideWithNewStops(
        ride: Ride,
        pickupStops: List<PickupStop>,
        addingPassenger: Boolean,
        originalDuration: Int = 0,
        originalDetour: Int = 0
    ): Ride {
        val timeReference: String = when (ride.direction) {
            RideDirection.TO_WORK -> ride.arrivalTime
            RideDirection.TO_HOME -> ride.departureTime
            else -> null
        } ?: throw IllegalArgumentException("❌ timeReference is null for rideId=${ride.rideId}")

        if (pickupStops.isEmpty()) {
            Log.d("RideUpdate", "🚫 אין תחנות כלל — חישוב מסלול בסיסי ללא waypoints")

            val route = mapsService.getDurationAndRouteFromGoogleApi(
                origin = ride.startLocation.geoPoint,
                destination = ride.destination.geoPoint,
                timeReference = timeReference,
                date = ride.date,
                direction = ride.direction!!
            )

            val updatedTimeReference = adjustTimeAccordingToDirection(
                time = timeReference,
                minutes = route.durationMinutes,
                direction = ride.direction
            )

            val baseRide = ride.copy(
                pickupStops = emptyList(),
                currentDetourMinutes = 0,
                encodedPolyline = route.encodedPolyline
            )

            return if (ride.direction == RideDirection.TO_WORK) {
                baseRide.copy(departureTime = updatedTimeReference)
            } else {
                baseRide.copy(arrivalTime = updatedTimeReference)
            }
        }

        val route = mapsService.getDurationAndRouteWithWaypoints(
            origin = ride.startLocation.geoPoint,
            waypoints = pickupStops,
            destination = ride.destination.geoPoint,
            timeReference = timeReference,
            date = ride.date,
            direction = ride.direction!!,
            passengerStop = null
        )

        val updatedStopsWithTimes = updatePickupStopTimesFromRouteResult(ride, route)

        val updatedTimeReference = adjustTimeAccordingToDirection(
            time = timeReference,
            minutes = route.durationMinutes,
            direction = ride.direction
        )

        val orderedPassengerIds = updatedStopsWithTimes.map { it.passengerId }

        val baseUpdatedRide = ride.copy(
            pickupStops = updatedStopsWithTimes,
            passengers = orderedPassengerIds,
            encodedPolyline = route.encodedPolyline
        )

        val finalRide = if (addingPassenger) {
            val newDetour = route.durationMinutes - originalDuration
            baseUpdatedRide.copy(currentDetourMinutes = newDetour)
        } else {
            val newDetour = originalDuration - route.durationMinutes
            val detour = originalDetour - newDetour
            baseUpdatedRide.copy(currentDetourMinutes = detour)
        }

        return if (ride.direction == RideDirection.TO_WORK) {
            finalRide.copy(departureTime = updatedTimeReference)
        } else {
            finalRide.copy(arrivalTime = updatedTimeReference)
        }
    }

    override suspend fun declineRideRequest(
        rideId: String,
        requestId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {

            val requestSnapshot = firestore.collection("rides")
                .document(rideId)
                .collection("requests")
                .document(requestId)
                .get()
                .await()

            val request = requestSnapshot.toObject(RideRequest::class.java)
            val passengerId = request?.passengerId

            if (passengerId.isNullOrBlank()) {
                Log.w("RideRequest", "⚠️ לא נמצא passengerId לבקשה $requestId")
                return@withContext false
            }

            val statusUpdated = rideRequestRepository.updateRequestStatus(
                rideId = rideId,
                requestId = requestId,
                newStatus = RequestStatus.DECLINED
            )

            if (!statusUpdated) {
                Log.w("RideRequest", "⚠ לא ניתן לעדכן סטטוס ל-DECLINED (requestId=$requestId)")
                return@withContext false
            }

            NotificationService.notifyPassengers(
                passengerIds = listOf(passengerId),
                rideId = rideId,
                title = "⛔ הבקשה נדחתה",
                body = "הבקשה שלך להצטרף לנסיעה נדחתה.",
                screen = "rideCancelled"
            )

            Log.d("RideRequest", "🗑 בקשה נדחתה והתראה נשלחה (requestId=$requestId)")
            return@withContext true

        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בדחיית בקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun cancelRideRequest(
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
        passengerId: String,
        rideCanceledForOnePassenger: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        val docRef = rideCollection.document(rideId)

        val ride = firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val ride = snapshot.toObject(Ride::class.java)

            if (ride == null || !ride.passengers.contains(passengerId)) {
                Log.w("RideLeave", "⚠ הנוסע $passengerId לא נמצא בנסיעה (rideId: $rideId)")
                return@runTransaction null
            }

            return@runTransaction ride
        }.await()

        if (ride == null) return@withContext

        try {
            val updatedPickupStops = ride.pickupStops.filterNot { it.passengerId == passengerId }
            val updatedPassengers = ride.passengers - passengerId
            val updatedOccupiedSeats = maxOf(ride.occupiedSeats - 1, 0)

            val rideWithoutPassenger = ride.copy(
                passengers = updatedPassengers,
                occupiedSeats = updatedOccupiedSeats
            )

            val duration =
                calculateTimeDifferenceInMinutes(ride.arrivalTime!!, ride.departureTime!!)
            Log.d("RideUpdate", "🕒 משך נסיעה כולל (arrival - departure): $duration דקות")

            val updatedRide = rebuildRideWithNewStops(
                rideWithoutPassenger,
                updatedPickupStops,
                false,
                duration,
                ride.currentDetourMinutes
            )

            rideCollection.document(rideId).set(updatedRide).await()

            val originalDuration = calculateTimeDifferenceInMinutes(
                startTime = ride.originalRoute.departureTime,
                endTime = ride.originalRoute.arrivalTime
            )

            updatePendingRequestsTimesFromRide(updatedRide, originalDuration)

            RepositoryProvider.providePassengerRepository()
                .removeActiveRideFromPassenger(passengerId, rideId)

            Log.d("RideLeave", "✅ הנוסע $passengerId הוסר והמסלול עודכן (rideId=$rideId)")

            if (rideCanceledForOnePassenger) {
                NotificationService.notifyPassengers(
                    passengerIds = listOf(passengerId),
                    rideId = ride.rideId,
                    title = "🚫 הצטרפותך לנסיעה בוטלה",
                    body = "נהג הנסיעה הסיר אותך ממנה",
                    screen = "rideCancelled"
                )

                val otherPassengers = updatedRide.passengers
                if (otherPassengers.isNotEmpty()) {
                    NotificationService.notifyPassengers(
                        passengerIds = otherPassengers,
                        rideId = ride.rideId,
                        title = "📢 בוצעו שינויים בנסיעה",
                        body = "פרטי הנסיעה עודכנו בעקבות ביטול נוסע.",
                        screen = "rideUpdated"
                    )
                }
            }

            //deletePassengerRequestsForRide(rideId, passengerId)

        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה בהסרת נוסע: ${e.message}", e)
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

                NotificationService.notifyRideCancelled(rideId)

                for (passengerId in ride.passengers.toList()) {
                    try {
                        removePassengerFromRide(rideId, passengerId)
                        deletePassengerRequestsForRide(rideId, passengerId)
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

    override suspend fun updateRide(ride: Ride) {
        rideCollection.document(ride.rideId).set(ride).await()
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
        companyCode: String,
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
            companyCode = companyCode,
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
            active = true,
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
            if (direction == RideDirection.TO_WORK)
                Log.e("RideLogic", "❌ שגיאה בחישוב זמן היציאה: ${e.message}", e)
            else
                Log.e("RideLogic", "❌ שגיאה בחישוב זמן הההגעה: ${e.message}", e)
            return@withContext false
        }

        val finalizedRide = baseRide.copy(
            arrivalTime = result.arrivalTime,
            departureTime = result.departureTime,
            encodedPolyline = result.encodedPolyline,
            originalRoute = result
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
        val result = getDurationAndRouteFromGoogleApi(
            origin,
            destination,
            timeReference,
            date,
            ride.direction!!
        )
        val timeInHours = adjustTimeAccordingToDirection(
            time = timeReference,
            minutes = result.durationMinutes,
            direction = ride.direction
        )
        if (ride.direction == RideDirection.TO_WORK)
            return DepartureCalculationResult(timeInHours, timeReference, result.encodedPolyline)
        else
            return DepartureCalculationResult(timeReference, timeInHours, result.encodedPolyline)
    }

    private suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String,
        direction: RideDirection
    ): DurationAndRoute {
        return mapsService.getDurationAndRouteFromGoogleApi(
            origin,
            destination,
            timeReference,
            date,
            direction
        )
    }

    override suspend fun adjustTimeAccordingToDirection(
        time: String,
        minutes: Int,
        direction: RideDirection
    ): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // הגדרת פורמט זמן
        val localTime =
            LocalTime.parse(time, formatter) // הופך את המחרוזת של time לאובייקט מסוג localTime
        val adjustedTime = when (direction) {
            RideDirection.TO_WORK -> localTime.minusMinutes(minutes.toLong())
            RideDirection.TO_HOME -> localTime.plusMinutes(minutes.toLong())
        }
        return adjustedTime.format(formatter)
    }

    override suspend fun calculateTimeDifferenceInMinutes(startTime: String, endTime: String): Int {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val start = LocalTime.parse(startTime, formatter)
        val end = LocalTime.parse(endTime, formatter)

        return abs(Duration.between(start, end).toMinutes().toInt())
    }
}