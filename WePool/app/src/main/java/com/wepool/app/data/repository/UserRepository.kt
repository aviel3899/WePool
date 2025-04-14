package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.users.Driver
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : IUserRepository {

    private val usersCollection = db.collection("users")

    // searches a user by UID
    // if exists -> turns it into an user (object), if not -> returns null
    override suspend fun getUser(uid: String): User? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) doc.toObject(User::class.java) else null
        } catch (e: Exception) {
            logException("getUser", e)
            null
        }
    }

    // writes an user (object) to the firestore's document with the same UID
    // if doesnt exist -> will be created, if exists -> override
    override suspend fun createOrUpdateUser(user: User) {
        try {
            usersCollection.document(user.uid).set(user).await()
        } catch (e: Exception) {
            logException("createOrUpdateUser", e)
        }
    }

    // deletes an user
    override suspend fun deleteUser(uid: String, driverRepository: IDriverRepository) {
        try {
            val userSnapshot = usersCollection.document(uid).get().await()
            val user = userSnapshot.toObject(User::class.java) ?: return

            // מחיקת כל Role לפי הרשימה
            user.roles.forEach { role ->
                when (role) {
                    "DRIVER" -> driverRepository.deleteDriver(uid)
                    // "PASSENGER" -> passengerRepository.deletePassenger(uid)
                    // תוסיף תפקידים נוספים במידת הצורך
                }
            }

            // ולבסוף – מחיקת המשתמש עצמו
            usersCollection.document(uid).delete().await()

            Log.d("Firestore", "🧹 המשתמש $uid וכל הנתונים המשויכים לו נמחקו")

        } catch (e: Exception) {
            logException("deleteUserWithRoles", e)
        }
    }

    override suspend fun deleteAllUsers(driverRepository: IDriverRepository) {
        try {
            val usersSnapshot = usersCollection.get().await()
            for (document in usersSnapshot.documents) {
                val uid = document.id
                deleteUser(uid, driverRepository)
            }
            Log.d("Firestore", "🧹 כל המשתמשים נמחקו בהצלחה")
        } catch (e: Exception) {
            logException("deleteAllUsers", e)
        }
    }

    // makes a query where all the users with the same companyID
    // return a list of user objects with the same companyID
    override suspend fun getUsersByCompany(companyId: String): List<User> {
        return try {
            usersCollection
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            logException("getUsersByCompany", e)
            listOf()
        }
    }

    // makes a query where all the users with the same role
    // return a list of user objects with the same role
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

    // if user exists -> updates only his name
    override suspend fun updateUserName(uid: String, newName: String) {
        try {
            usersCollection.document(uid).update("name", newName).await()
        } catch (e: Exception) {
            logException("updateUserName", e)
        }
    }

    // if user exists -> updates only his email
    override suspend fun updateUserEmail(uid: String, newEmail: String) {
        try {
            usersCollection.document(uid).update("email", newEmail).await()
        } catch (e: Exception) {
            logException("updateUserEmail", e)
        }
    }

    // if user exists -> updates only his phoneNumber
    override suspend fun updateUserPhoneNumber(uid: String, newPhone: String) {
        try {
            usersCollection.document(uid).update("phoneNumber", newPhone).await()
        } catch (e: Exception) {
            logException("updateUserPhoneNumber", e)
        }
    }

    // if user exists -> updates only his companyID
    override suspend fun updateUserCompanyId(uid: String, newCompanyId: String?) {
        try {
            usersCollection.document(uid).update("companyId", newCompanyId).await()
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

    // add a role to user's role list, checks for duplicates
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

    private fun logException(func: String, e: Exception) {
        // כאן תוכל להוסיף הדפסה, שליחה ל-Firebase Crashlytics, Sentry, וכו'
        println("🔥 [UserRepository::$func] ${e.message}")
    }
}
