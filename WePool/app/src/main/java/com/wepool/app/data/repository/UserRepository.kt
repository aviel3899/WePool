package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import com.wepool.app.data.model.users.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : IUserRepository {

    private val usersCollection = db.collection("users")

    override suspend fun getUser(uid: String): User? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) doc.toObject(User::class.java) else null
        } catch (e: Exception) {
            logException("getUser", e)
            null
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            db.collection("users").get().await().documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            logException("getAllUsers", e)
            emptyList()
        }
    }

    override suspend fun createOrUpdateUser(user: User) {
        try {
            usersCollection.document(user.uid).set(user).await()
        } catch (e: Exception) {
            logException("createOrUpdateUser", e)
        }
    }

    override suspend fun deleteUser(uid: String, driverRepository: IDriverRepository, passengerRepository: IPassengerRepository) {
        try {
            val userSnapshot = usersCollection.document(uid).get().await()
            val user = userSnapshot.toObject(User::class.java) ?: return

            user.roles.forEach { role ->
                when (role) {
                    "DRIVER" -> driverRepository.deleteDriver(uid)
                    "PASSENGER" -> passengerRepository.deletePassenger(uid)
                    // צריך להוסיף HRManager & Admin
                }
            }

            usersCollection.document(uid).delete().await()

            Log.d("Firestore", "🧹 המשתמש $uid וכל הנתונים המשויכים לו נמחקו")

        } catch (e: Exception) {
            logException("deleteUserWithRoles", e)
        }
    }

    override suspend fun deleteAllUsers(driverRepository: IDriverRepository, passengerRepository: IPassengerRepository) {
        try {
            val allUsers = getAllUsers()
            for (user in allUsers) {
                deleteUser(user.uid, driverRepository, passengerRepository)
            }
            Log.d("UserRepository", "🧹 כל המשתמשים נמחקו בהצלחה עם כל הנתונים המשויכים להם.")
        } catch (e: Exception) {
            logException("deleteAllUsersWithRoles", e)
        }
    }

    override suspend fun getUsersByCompany(companyCode: String): List<User> {
        return try {
            usersCollection
                .whereEqualTo("companyCode", companyCode)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            logException("getUsersByCompany", e)
            listOf()
        }
    }

    override suspend fun getUsersByRole(role: String): List<User> {
        return try {
            usersCollection
                .whereArrayContains("roles", role)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            logException("getUsersByRole", e)
            listOf()
        }
    }

    override suspend fun updateUserName(uid: String, newName: String) {
        try {
            usersCollection.document(uid).update("name", newName).await()
        } catch (e: Exception) {
            logException("updateUserName", e)
        }
    }

    override suspend fun updateUserEmail(uid: String, newEmail: String) {
        try {
            usersCollection.document(uid).update("email", newEmail).await()
        } catch (e: Exception) {
            logException("updateUserEmail", e)
        }
    }

    override suspend fun updateUserPhoneNumber(uid: String, newPhone: String) {
        try {
            usersCollection.document(uid).update("phoneNumber", newPhone).await()
        } catch (e: Exception) {
            logException("updateUserPhoneNumber", e)
        }
    }

    override suspend fun updateUserCompanyCode(uid: String, newCompanyCode: String?) {
        try {
            usersCollection.document(uid).update("companyId", newCompanyCode).await()
        } catch (e: Exception) {
            logException("updateUserCompanyId", e)
        }
    }

    override suspend fun banUser(uid: String) {
        try {
            usersCollection.document(uid).update("isBanned", true).await()
        } catch (e: Exception) {
            logException("banUser", e)
        }
    }

    override suspend fun unbanUser(uid: String) {
        try {
            usersCollection.document(uid).update("isBanned", false).await()
        } catch (e: Exception) {
            logException("unbanUser", e)
        }
    }

    override suspend fun activateUser(uid: String) {
        try {
            usersCollection.document(uid)
                .update("isActive", true)
                .await()
            Log.d("UserRepository", "✅ המשתמש $uid הופעל מחדש")
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ שגיאה בהפעלת המשתמש", e)
        }
    }

    override suspend fun unActivateUser(uid: String) {
        try {
            usersCollection.document(uid)
                .update("isActive", false)
                .await()
            Log.d("UserRepository", "🚫 המשתמש $uid הפך ללא פעיל")
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ שגיאה בהשבתת המשתמש", e)
        }
    }

    override suspend fun updateLastLoginTimestamp(uid: String, timestamp: Long) {
        try {
            usersCollection.document(uid).update("lastLoginTimestamp", timestamp).await()
            Log.d("UserRepository", "🕒 זמן התחברות עודכן עבור המשתמש $uid")
        } catch (e: Exception) {
            logException("updateLastLoginTimestamp", e)
        }
    }

    /*override suspend fun updatePreviousLoginTimestamp(uid: String, timestamp: Long) {
        try {
            usersCollection.document(uid).update("previousLoginTimeStamp", timestamp).await()
            Log.d("UserRepository", "🕒 זמן התחברות עודכן עבור המשתמש $uid")
        } catch (e: Exception) {
            logException("updatePreviousLoginTimestamp", e)
        }
    }*/

    override suspend fun addRoleToUser(uid: String, role: String) {
        try {
            usersCollection.document(uid)
                .update("roles", FieldValue.arrayUnion(role))
                .await()
        } catch (e: Exception) {
            logException("addRoleToUser", e)
        }
    }

    override suspend fun removeRoleFromUser(uid: String, role: String) {
        try {
            usersCollection.document(uid)
                .update("roles", FieldValue.arrayRemove(role))
                .await()
        } catch (e: Exception) {
            logException("removeRoleFromUser", e)
        }
    }

    override suspend fun updateUserToken(uid: String, newToken: String) {
        try {
            val userDoc = db.collection("users").document(uid).get().await()
            val existingToken = userDoc.getString("fcmToken")

            if (existingToken != newToken) {
                db.collection("users").document(uid)
                    .update("fcmToken", newToken)
                    .await()
                Log.d("UserRepository", "✅ FCM token עודכן עבור UID: $uid")
            } else {
                Log.d("UserRepository", "ℹ️ FCM token לא השתנה עבור UID: $uid")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ שגיאה בעדכון FCM token", e)
        }
    }

    override fun uploadFcmTokenForCurrentUser() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        Log.d("FCM", "📲 Token חדש: $token")
                        if (!token.isNullOrBlank()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                updateUserToken(uid, token)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.e("UserRepository", "❌ שגיאה בקבלת token חדש", it)
                    }
            } else {
                Log.e("UserRepository", "❌ שגיאה במחיקת token קודם", deleteTask.exception)
            }
        }
    }

    private fun logException(func: String, e: Exception) {
        println("🔥 [UserRepository::$func] ${e.message}")
    }
}