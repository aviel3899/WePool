package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.users.User

interface IUserRepository {
    suspend fun createOrUpdateUser(user: User)
    suspend fun deleteUser(uid: String, driverRepository: IDriverRepository, passengerRepository: IPassengerRepository)
    suspend fun deleteAllUsers(driverRepository: IDriverRepository, passengerRepository: IPassengerRepository)

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

    suspend fun addRoleToUser(uid: String, role: String)
    suspend fun removeRoleFromUser(uid: String, role: String)
}


