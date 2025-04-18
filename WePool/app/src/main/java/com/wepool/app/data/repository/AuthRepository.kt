package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth
) {

     // התחברות עם מייל וסיסמה

    suspend fun loginWithEmailAndPassword(email: String, password: String): Result<String> {
        return try {
            val result: AuthResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("UID לא קיים"))

            val tokenResult = auth.currentUser?.getIdToken(true)?.await()
            val token = tokenResult?.token ?: return Result.failure(Exception("טוקן לא התקבל"))

            Log.d("AuthRepository", "🟢 התחברות הצליחה | UID: $uid | Token: $token")
            Result.success(uid)

        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ שגיאה בהתחברות: ${e.message}", e)
            Result.failure(e)
        }
    }

     // הרשמה עם מייל וסיסמה + יצירת משתמש ב-Firestore דרך IUserRepository
    suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        user: User,
        userRepository: IUserRepository
    ): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Result.failure(Exception("UID לא נוצר"))

            val newUser = user.copy(uid = uid, email = email)
            userRepository.createOrUpdateUser(newUser)

            Log.d("AuthRepository", "🟢 הרשמה הצליחה | UID: $uid")
            Result.success(uid)

        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ שגיאה בהרשמה: ${e.message}", e)
            Result.failure(e)
        }
    }
}
