package com.wepool.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "🔥 טוקן חדש התקבל: $token")

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val userRepository = RepositoryProvider.provideUserRepository()

            CoroutineScope(Dispatchers.IO).launch {
                userRepository.updateUserToken(uid, token)
            }
        } else {
            Log.w("FCM", "👤 אין משתמש מחובר – לא ניתן לשמור טוקן כרגע")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "WePool"
        val body = message.notification?.body ?: "יש לך הודעה חדשה"

        NotificationHelper.showSimpleNotification(this, title, body)
        Log.d("FCM", "📩 הודעה התקבלה: $title - $body")
    }
}
