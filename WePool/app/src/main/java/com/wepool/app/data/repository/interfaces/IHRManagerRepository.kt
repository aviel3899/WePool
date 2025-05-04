package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.users.HRManager

interface IHRManagerRepository {
    suspend fun saveHRManager(uid: String, hrManager: HRManager)
    suspend fun getHRManager(uid: String): HRManager?
    suspend fun deleteHRManager(uid: String)
    suspend fun activateUser(uid: String)
    suspend fun unActivateUser(uid: String)
    suspend fun banUser(uid: String)
    suspend fun unbanUser(uid: String)
    suspend fun addEmployeeToCompany(companyId: String, userUid: String)
    suspend fun reactivateEmployee(companyId: String, userUid: String)
    suspend fun removeEmployeeFromCompany(companyId: String, userUid: String)
}