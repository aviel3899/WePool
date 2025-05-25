package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.HRManagerRepository

interface IUserRepository {
    suspend fun createOrUpdateUser(user: User)
    suspend fun deleteUser(
        uid: String,
        driverRepository: IDriverRepository,
        passengerRepository: IPassengerRepository,
        hrManagerRepository: IHRManagerRepository
    )
    suspend fun deleteAllUsers(driverRepository: IDriverRepository, passengerRepository: IPassengerRepository, hrManagerRepository: IHRManagerRepository)

    suspend fun getUser(uid: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun getUsersByCompany(companyCode: String): List<User>
    suspend fun getUsersByRole(role: String): List<User>

    suspend fun updateUserName(uid: String, newName: String)
    suspend fun updateUserEmail(uid: String, newEmail: String)
    suspend fun updateUserPhoneNumber(uid: String, newPhone: String)
    suspend fun updateUserCompanyCode(uid: String, newCompanyCode: String?)

    suspend fun banUser(uid: String)
    suspend fun unbanUser(uid: String)
    suspend fun activateUser(uid: String)
    suspend fun unActivateUser(uid: String)
    suspend fun updateLastLoginTimestamp(uid: String, timestamp: Long)

    suspend fun addRoleToUser(uid: String, role: String)
    suspend fun removeRoleFromUser(uid: String, role: String)

    suspend fun updateUserToken(uid: String, newToken: String)
    fun uploadFcmTokenForCurrentUser()

    suspend fun addFavoriteLocation(uid: String, location: LocationData)
    suspend fun removeFavoriteLocation(uid: String, placeId: String)
    suspend fun updateFavoriteLocations(uid: String, updatedList: List<LocationData>)
}