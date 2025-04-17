package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.users.Driver

interface IDriverRepository {

    // שמירת כל פרטי הנהג למסמך info
    suspend fun saveDriver(driver: Driver)

    // שליפת פרטי הנהג לפי UID
    suspend fun getDriver(uid: String): Driver?

    // מחיקת מסמך הנהג (driverData/info)
    suspend fun deleteDriver(uid: String)

    // שליפת נסיעה עתידית קרובה של הנהג
    //suspend fun getUpcomingRide(driverUid: String): Ride?

    // שליפת היסטוריית נסיעות של הנהג
   // suspend fun getRideHistory(driverUid: String): List<Ride>

    suspend fun updateVehicleDetails(uid: String, vehicleDetails: String)
    suspend fun updateActiveRideId(uid: String, rideId: String?)
}

