package com.wepool.app.data.model.logic

import android.util.Log
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class PassengerRideFinder(
    private val mapsService: IGoogleMapsService,
    private val routeMatcher: RouteMatcher
) {

    private val maxArrivalTimeDifferenceMinutes = 30L
    private val maxDepartureTimeDifferenceMinutes = 15L
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val minDepartureDelayToWorkMinutes = 60L
    private val minDepartureDelayToHomeMinutes = 10L

    suspend fun getAvailableRidesForPassenger(
        companyCode: String,
        direction: RideDirection,
        passengerArrivalTime: String = "",
        passengerDepartureTime: String = "",
        passengerDate: String,
        pickupPoint: PickupStop,
        rideRepository: IRideRepository,
        rideRequestRepository: IRideRequestRepository
    ): List<RideCandidate> = withContext(Dispatchers.IO) {

        val allRides = rideRepository.getRidesByCompanyAndDirection(companyCode, direction)

        val candidates = allRides.mapNotNull { ride ->
            val dateOK = ride.date == passengerDate
            val timeOK: Boolean = if (direction == RideDirection.TO_WORK) {
                isRideTimeValid(ride.arrivalTime!!, ride.maxDetourMinutes, passengerArrivalTime, direction)
            } else {
                isRideTimeValid(ride.departureTime!!, ride.maxDetourMinutes, passengerDepartureTime, direction)
            }

            val notAlreadyRequested = try {
                val existingRequests = rideRequestRepository.getRequestsForRide(ride.rideId)
                existingRequests.none {
                    it.passengerId == pickupPoint.passengerId &&
                            (it.status == RequestStatus.PENDING || it.status == RequestStatus.ACCEPTED)
                }
            } catch (e: Exception) {
                Log.e("RideFilter", "❌ שגיאה בשליפת בקשות לנסיעה ${ride.rideId}: ${e.message}")
                false
            }

            val notAlreadyJoined = !ride.passengers.contains(pickupPoint.passengerId)
            val seatOK = ride.passengers.size < ride.availableSeats

            val currentRouteTimeMinutes = try {
                val departure = LocalTime.parse(ride.departureTime, timeFormatter)
                val arrival = LocalTime.parse(ride.arrivalTime, timeFormatter)
                Duration.between(departure, arrival).toMinutes().toInt()
            } catch (e: Exception) {
                Log.e("RideFilter", "❌ שגיאה בחישוב זמן מסלול: ${e.message}")
                return@mapNotNull null
            }

            val timeReference = if(ride.direction == RideDirection.TO_WORK){
                ride.arrivalTime!!
            }
            else{
                ride.departureTime!!
            }

            val evaluation = routeMatcher.evaluatePickupDetour(
                encodedPolyline = ride.encodedPolyline,
                pickupPoint = pickupPoint,
                maxAllowedDetourMinutes = ride.maxDetourMinutes,
                currentDetourMinutes = ride.currentDetourMinutes,
                currentRouteTimeMinutes = currentRouteTimeMinutes,
                timeReference = timeReference,
                date = ride.date,
                mapsService = mapsService,
                startLocation = ride.startLocation.geoPoint,
                destination = ride.destination.geoPoint,
                currentPickupStops = ride.pickupStops,
                rideRepository = rideRepository,
                rideDirection = direction
            )

            val detourOK = evaluation.isAllowed

            val now = LocalDateTime.now()
            val rideDate = LocalDate.parse(ride.date, dateFormatter)
            val rideTime = LocalTime.parse(ride.departureTime?.trim(), timeFormatter)
            val rideDateTime = LocalDateTime.of(rideDate, rideTime)
            val futureEnough = when (direction) {
                RideDirection.TO_WORK -> rideDateTime.isAfter(now.plusMinutes(minDepartureDelayToWorkMinutes)) // אי אפשר להצטרף לנסיעה שיוצאת עוד פחות משעה לעבודה
                RideDirection.TO_HOME -> rideDateTime.isAfter(now.plusMinutes(minDepartureDelayToHomeMinutes)) // אי אפשר להצטרף לנסיעה שיוצאת עוד פחות מ10 דקות הביתה
            }

            val notHisOwnRide = ride.driverId != pickupPoint.passengerId

            if (!dateOK || !timeOK || !seatOK || !notAlreadyJoined || !notAlreadyRequested || !detourOK || !futureEnough || !notHisOwnRide) {
                Log.d("RideFilter", """ ❌ נסיעה לא מתאימה:
                - תאריך תואם? $dateOK
                - זמן תואם? $timeOK
                - יש מקומות פנויים? $seatOK
                - נוסע עדיין לא הצטרף? $notAlreadyJoined
                - נוסע עדיין לא שלח בקשה? $notAlreadyRequested
                - סטייה מותרת? $detourOK
                - זמן יציאה מספיק עתידי? $futureEnough
                - האם הנסיעה היא לא של עצמו? $notHisOwnRide
                """.trimIndent())
                return@mapNotNull null
            }


            return@mapNotNull RideCandidate(
                ride = ride,
                detourEvaluationResult = evaluation
            )
        }

        Log.d("RideFilter", "✅ נמצאו ${candidates.size} נסיעות מתאימות לנוסע")
        return@withContext candidates
    }

    private fun isRideTimeValid(
        driverTime: String,
        detourMinutes: Int,
        passengerTime: String,
        direction: RideDirection
    ): Boolean {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val driver = LocalTime.parse(driverTime, formatter)
        val driverToWork = driver.plusMinutes(detourMinutes.toLong())
        val passenger = LocalTime.parse(passengerTime, formatter)

        return if (direction == RideDirection.TO_WORK) { //זמן ההגעה של הנוסע לא אחרי זמן ההגעה של הנהג ולא X דקות לםניו
            !passenger.isAfter(driverToWork.plusMinutes(maxArrivalTimeDifferenceMinutes)) &&
                    !passenger.isBefore(driverToWork)
        } else { //זמן היציאה של הנוסע לא אחרי זמן היציאה של הנהג ולא X דקות לפניו
            !passenger.isBefore(driver.minusMinutes(maxDepartureTimeDifferenceMinutes)) &&
                    !passenger.isAfter(driver)

        }
    }
}