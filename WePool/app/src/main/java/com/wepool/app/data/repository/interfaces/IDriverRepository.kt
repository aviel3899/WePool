package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Driver
//import com.wepool.app.data.model.users.PassengerRideInfo

interface IDriverRepository {

    // שמירת כל פרטי הנהג למסמך info
    suspend fun saveDriver(driver: Driver)

    // שליפת פרטי הנהג לפי UID
    suspend fun getDriver(uid: String): Driver?

    // מחיקת מסמך הנהג (driverData/info)
    suspend fun deleteDriver(uid: String)

    // יצירת נסיעה חדשה על בסיס פרטי הנהג
    //suspend fun createRideForDriver(driver: Driver): String

    // שליפת נסיעה עתידית קרובה של הנהג
    //suspend fun getUpcomingRide(driverUid: String): Ride?

    // שליפת היסטוריית נסיעות של הנהג
   // suspend fun getRideHistory(driverUid: String): List<Ride>

    // חישוב זמן יציאה לפי יעד, מוצא, זמן הגעה, כולל הפחתת 10 דקות
    suspend fun calculateDepartureTimeFromArrival(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DepartureCalculationResult

    // עדכון רשימת נוסעים לנסיעה תוך בדיקה של סטייה וזמינות כיסאות
    /*suspend fun updateRideWithPassenger(
        rideId: String,
        passengerInfo: PassengerRideInfo
    ): Boolean*/

    // פונקציות לעדכון שדות ספציפיים בפרטי הנהג
    suspend fun updateAvailableSeats(uid: String, seats: Int)
    suspend fun updateVehicleDetails(uid: String, vehicleDetails: String)
    suspend fun updateMaxDetourMinutes(uid: String, maxDetour: Int)
    suspend fun updatePreferredArrivalTime(uid: String, time: String)
    suspend fun updateCalculatedDepartureTime(uid: String, time: String)
    suspend fun updateDirection(uid: String, direction: String)
    suspend fun updateDestination(uid: String, destination: GeoPoint)
    suspend fun updateActiveRideId(uid: String, rideId: String?)
}

