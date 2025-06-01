package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IHRManagerRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import kotlinx.coroutines.tasks.await

class CompanyRepository(
    private val firestore: FirebaseFirestore,
    private val mapsService: IGoogleMapsService,
    private val userRepository: IUserRepository,
) : ICompanyRepository {

    private val companiesCollection = firestore.collection("companies")

    override suspend fun getAllCompanies(): List<Company> {
        return try {
            val snapshot = companiesCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(Company::class.java) }
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to get all companies", e)
            emptyList()
        }
    }

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

    override suspend fun getCompanyByHrUid(hrUid: String): Company? {
        return try {
            val snapshot = companiesCollection
                .whereEqualTo("hrManagerUid", hrUid)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(Company::class.java)
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to get company by HR UID", e)
            null
        }
    }

    override suspend fun deleteCompanyById(
        companyId: String,
        hrManagerRepository: IHRManagerRepository
    ) {
        try {
            val companyDoc = firestore.collection("companies").document(companyId).get().await()
            val company = companyDoc.toObject(Company::class.java)

            if (company != null) {
                val employeeUids = company.employees
                val hrManagerUid = company.hrManagerUid

                for (uid in employeeUids) {
                    userRepository.updateUserCompanyCode(uid, "")
                }

                if (hrManagerUid != null) {
                    userRepository.removeRoleFromUser(hrManagerUid, UserRole.HR_MANAGER.name)
                    hrManagerRepository.deleteHRManager(hrManagerUid)
                    Log.d("CompanyRepository", "👤 HR Manager $hrManagerUid demoted and HR data deleted")
                }

                firestore.collection("companies").document(companyId).delete().await()

                Log.d("CompanyRepository", "🗑️ Company $companyId deleted and all employees updated")

            } else {
                Log.e("CompanyRepository", "❌ Company $companyId not found for deletion")
            }
        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Error deleting company and updating employees", e)
        }
    }

    override suspend fun generateRandomUniqueCompanyCode(): String {
        val allowedChars = ('1'..'9') + ('A'..'Z').filterNot { it == 'O' } // ללא האות O וללא הספרה 0 כדי למנוע בלבולים בהזנת קוד החברה
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

    override suspend fun getLocationByCompanyCode(companyCode: String): LocationData? {
        return try {
            Log.d("CompanyRepository", "🔍 Searching for company with code: $companyCode")

            val snapshot = firestore.collection("companies")
                .whereEqualTo("companyCode", companyCode)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                Log.e("CompanyRepository", "❌ No company found with code: $companyCode")
                return null
            }

            val locationMap = doc.get("location") as? Map<*, *>
            if (locationMap == null) {
                Log.e("CompanyRepository", "❌ 'location' field missing in company document")
                return null
            }

            val name = locationMap["name"] as? String ?: ""
            val placeId = locationMap["placeId"] as? String ?: ""
            val geoPoint = locationMap["geoPoint"] as? com.google.firebase.firestore.GeoPoint

            if (geoPoint == null) {
                Log.e("CompanyRepository", "❌ 'geoPoint' is missing or invalid in location")
                return null
            }

            val locationData = LocationData(
                name = name,
                placeId = placeId,
                geoPoint = geoPoint
            )

            Log.d("CompanyRepository", "✅ Found location: $locationData")
            locationData

        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Exception while fetching location by code: $companyCode", e)
            null
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

    override suspend fun setHrManager(
        companyId: String,
        hrManagerUid: String,
        hrManagerRepository: IHRManagerRepository
    ) {
        try {
            val companyDoc = companiesCollection.document(companyId).get().await()
            val company = companyDoc.toObject(Company::class.java)
            val previousHrUid = company?.hrManagerUid

            companiesCollection.document(companyId)
                .update("hrManagerUid", hrManagerUid)
                .await()

            userRepository.addRoleToUser(hrManagerUid, UserRole.HR_MANAGER.name)

            if (previousHrUid != null && previousHrUid != hrManagerUid) {
                userRepository.removeRoleFromUser(previousHrUid, UserRole.HR_MANAGER.name)
                hrManagerRepository.deleteHRManager(previousHrUid)
            }

            val hrUser = userRepository.getUser(hrManagerUid)
            if (hrUser != null && !hrUser.active) {
                userRepository.activateUser(hrManagerUid)
                Log.d("CompanyRepository", "✅ HR Manager $hrManagerUid was re-activated")
            }

            Log.d("CompanyRepository", "🏢 HR manager updated: $hrManagerUid (prev: $previousHrUid) for $companyId")

        } catch (e: Exception) {
            Log.e("CompanyRepository", "❌ Failed to update HR manager", e)
        }
    }

}