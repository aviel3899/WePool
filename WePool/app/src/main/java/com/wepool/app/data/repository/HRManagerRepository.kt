package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.users.HRManager
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.data.repository.interfaces.IHRManagerRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import kotlinx.coroutines.tasks.await

class HRManagerRepository(
    private val firestore: FirebaseFirestore,
    private val userRepository: IUserRepository,
    private val companyRepository: ICompanyRepository
) : IHRManagerRepository {

    private fun hrManagerDocRef(uid: String) =
        firestore.collection("users").document(uid).collection("HRManagerData").document("info")

    override suspend fun saveHRManager(uid: String, hrManager: HRManager) {
        try {
            hrManagerDocRef(uid).set(hrManager).await()
            Log.d("HRManagerRepository", "✅ HRManager saved for user: $uid")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ Failed to save HRManager", e)
        }
    }

    override suspend fun getHRManager(uid: String): HRManager? {
        return try {
            val snapshot = hrManagerDocRef(uid).get().await()
            snapshot.toObject(HRManager::class.java)
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ Failed to get HRManager", e)
            null
        }
    }

    override suspend fun deleteHRManager(uid: String) {
        try {
            hrManagerDocRef(uid).delete().await()
            Log.d("HRManagerRepository", "🗑️ HRManager deleted for user: $uid")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ Failed to delete HRManager", e)
        }
    }

    override suspend fun activateUser(uid: String) {
        try {
            userRepository.activateUser(uid)
            Log.d("HRManagerRepository", "✅ המשתמש $uid הופעל מחדש ע״י HR")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ שגיאה בהפעלת משתמש", e)
        }
    }

    override suspend fun unActivateUser(uid: String) {
        try {
            userRepository.unActivateUser(uid)
            Log.d("HRManagerRepository", "🚫 המשתמש $uid הפך ללא פעיל ע״י HR")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ שגיאה בהשבתת משתמש", e)
        }
    }

    override suspend fun banUser(uid: String) {
        try {
            firestore.collection("users").document(uid)
                .update("isBanned", true)
                .await()
            Log.d("HRManagerRepository", "⛔ המשתמש $uid נחסם ע״י HR")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ שגיאה בחסימת משתמש", e)
        }
    }

    override suspend fun unbanUser(uid: String) {
        try {
            firestore.collection("users").document(uid)
                .update("isBanned", false)
                .await()
            Log.d("HRManagerRepository", "🔓 המשתמש $uid הוסר מחסימה ע״י HR")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ שגיאה בהסרת חסימה", e)
        }
    }

    override suspend fun addEmployeeToCompany(companyId: String, userUid: String) {
        try {
            companyRepository.addEmployeeToCompany(companyId, userUid)
            userRepository.activateUser(userUid)
            Log.d("HRManagerRepository", "✅ Added and activated employee $userUid to company $companyId")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ Failed to add and activate employee", e)
        }
    }

    override suspend fun reactivateEmployee(companyId: String, userUid: String) {
        try {
            // אם העובד לא קיים ברשימת החברה, נוסיף אותו
            val company = companyRepository.getCompanyById(companyId)
            if (company != null && !company.employees.contains(userUid)) {
                companyRepository.addEmployeeToCompany(companyId, userUid)
            }

            userRepository.activateUser(userUid)

            Log.d("HRManagerRepository", "✅ Reactivated employee $userUid for company $companyId")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ Failed to reactivate employee", e)
        }
    }

    override suspend fun removeEmployeeFromCompany(companyId: String, userUid: String) {
        try {
            companyRepository.removeEmployeeFromCompany(companyId, userUid)
            userRepository.unActivateUser(userUid)

            Log.d("HRManagerRepository", "✅ Removed employee $userUid and deactivated user")
        } catch (e: Exception) {
            Log.e("HRManagerRepository", "❌ Failed to remove employee and deactivate", e)
        }
    }



}
