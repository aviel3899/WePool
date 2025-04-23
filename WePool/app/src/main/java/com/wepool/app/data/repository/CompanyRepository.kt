package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.data.remote.IGoogleMapsService
import kotlinx.coroutines.tasks.await

class CompanyRepository(
    private val firestore: FirebaseFirestore,
    private val mapsService: IGoogleMapsService
) : ICompanyRepository {

    private val companiesCollection = firestore.collection("companies")

    override suspend fun getCompanyById(companyId: String): Company? {
        return try {
            val snapshot = companiesCollection.document(companyId).get().await()
            snapshot.toObject(Company::class.java)
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to get company $companyId", e)
            null
        }
    }

    override suspend fun getCompanyByCode(companyCode: String): Company? {
        return try {
            val snapshot = companiesCollection
                .whereEqualTo("companyCode", companyCode)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(Company::class.java)
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to get company by code", e)
            null
        }
    }

    override suspend fun generateRandomUniqueCompanyCode(): String {
        val allowedChars = ('0'..'9') + ('A'..'Z')
        var code: String
        var attempts = 0

        do {
            // יצירת קוד רנדומלי בן 6 תווים
            code = (1..6)
                .map { allowedChars.random() }
                .joinToString("")
            attempts++
            // הגבלת מספר ניסיונות למניעת לולאה אינסופית
            if (attempts > 50) throw Exception("לא ניתן למצוא קוד חברה פנוי. נסה שוב.")
        } while (isCompanyCodeTaken(code)) // בדיקה שהקוד לא קיים בfirebase

        return code
    }


    override suspend fun createOrUpdateCompany(company: Company) {
        try {
            companiesCollection.document(company.companyId).set(company).await()
            Log.d("CompanyRepository", "✅ Company saved/updated: ${company.companyId}")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to save company", e)
        }
    }

    override suspend fun isCompanyCodeTaken(companyCode: String): Boolean {
        return try {
            val snapshot = companiesCollection
                .whereEqualTo("companyCode", companyCode)
                .get()
                .await()

            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to check companyCode uniqueness", e)
            true // אם הייתה שגיאה, עדיף לא לאפשר יצירה עם הקוד הזה
        }
    }


    override suspend fun updateLocation(companyId: String, newAddress: String) {
        try {
            val locationData = mapsService.getCoordinatesFromAddress(newAddress)

            if (locationData == null) {
                Log.e("CompanyRepository", "❌ Address not found or invalid: $newAddress")
                return
            }

            companiesCollection.document(companyId)
                .update("location", locationData)
                .await()
            Log.d("CompanyRepository", "📍 Location updated from address: $newAddress")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to update location from address", e)
        }
    }

    override suspend fun updateLogoUrl(companyId: String, newLogoUrl: String?) {
        try {
            companiesCollection.document(companyId)
                .update("logoUrl", newLogoUrl)
                .await()
            Log.d("CompanyRepository", "🖼️ Logo URL updated")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to update logoUrl", e)
        }
    }

    override suspend fun updateIsCompanyActive(companyId: String, isActive: Boolean) {
        try {
            companiesCollection.document(companyId)
                .update("active", isActive)
                .await()
            Log.d("CompanyRepository", "✅ Active status updated to: $isActive")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to update active status", e)
        }
    }

    override suspend fun addEmployeeToCompany(companyId: String, userUid: String) {
        try {
            val docRef = companiesCollection.document(companyId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val company = snapshot.toObject(Company::class.java)
                if (company != null && !company.employees.contains(userUid)) {
                    val updatedEmployees = company.employees + userUid
                    transaction.update(docRef, "employees", updatedEmployees)
                }
            }.await()
            Log.d("CompanyRepository", "👤 Added employee $userUid to company $companyId")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to add employee", e)
        }
    }

    override suspend fun removeEmployeeFromCompany(companyId: String, userUid: String) {
        try {
            val docRef = companiesCollection.document(companyId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val company = snapshot.toObject(Company::class.java)
                if (company != null && company.employees.contains(userUid)) {
                    val updatedEmployees = company.employees - userUid
                    transaction.update(docRef, "employees", updatedEmployees)
                }
            }.await()

            Log.d("CompanyRepository", "🗑️ Removed employee $userUid from company $companyId")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to remove employee", e)
        }
    }

    override suspend fun updateCompanyName(companyId: String, newName: String) {
        try {
            companiesCollection.document(companyId)
                .update("companyName", newName)
                .await()
            Log.d("CompanyRepository", "🏷️ Company name updated to: $newName")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to update company name", e)
        }
    }

    override suspend fun setHrManager(companyId: String, hrManagerUid: String) {
        try {
            companiesCollection.document(companyId)
                .update("hrManagerUid", hrManagerUid)
                .await()
            Log.d("CompanyRepository", "🏢 HR manager set: $hrManagerUid for $companyId")
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to set HR manager", e)
        }
    }
}
