package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.company.Company

interface ICompanyRepository {
    suspend fun getCompanyById(companyId: String): Company?
    suspend fun getCompanyByCode(companyCode: String): Company?
    suspend fun generateRandomUniqueCompanyCode(): String
    suspend fun createOrUpdateCompany(company: Company)
    suspend fun isCompanyCodeTaken(companyCode: String): Boolean
    suspend fun updateCompanyName(companyId: String, newName: String)
    suspend fun updateLocation(companyId: String, newAddress: String)
    suspend fun updateLogoUrl(companyId: String, newLogoUrl: String?)
    suspend fun updateIsCompanyActive(companyId: String, isActive: Boolean)
    suspend fun removeEmployeeFromCompany(companyId: String, userUid: String)
    suspend fun addEmployeeToCompany(companyId: String, userUid: String)
    suspend fun setHrManager(companyId: String, hrManagerUid: String)
}
