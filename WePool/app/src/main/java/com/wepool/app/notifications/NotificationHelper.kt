package com.wepool.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wepool.app.MainActivity
import com.wepool.app.R

object NotificationHelper {

    private const val CHANNEL_ID = "ride_navigation_channel"
    private const val CHANNEL_NAME = "Ride Navigation"
    private const val PASSENGER_NOTIFICATION_ID = 12345

    fun buildNavigationNotification(
        context: Context,
        contentText: String,
        rideId: String? = null,
        screen: String? = "rideStarted" // ברירת מחדל
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createSilentNotificationChannel(context)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("rideId", rideId)
            putExtra("screen", screen)
            putExtra("fromNotification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            //.setContentIntent(pendingIntent) // כדי שלא יהיה לחיץ אז זה בהערות
            .build()
    }

    private fun createSilentNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN // הכי שקט
            ).apply {
                description = "ערוץ שקט לניווט ברקע"
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }


    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "התראות ניווט חיות באפליקציית WePool"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showSimpleNotification(
        context: Context,
        title: String,
        message: String,
        rideId: String? = null,
        screen: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("rideId", rideId)
            putExtra("screen", screen)
            putExtra("fromNotification", true)
        }

        Log.d("NotificationHelper", "🔔 intent built with rideId=$rideId, screen=$screen, fromNotification=true")

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun storeNotificationData(context: Context, screen: String?, rideId: String?) {
        val prefs = context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("screen", screen)
            putString("rideId", rideId)
            apply()
        }
    }

    fun getStoredNotificationData(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
        val screen = prefs.getString("screen", null)
        val rideId = prefs.getString("rideId", null)
        return Pair(screen, rideId)
    }

    fun clearNotificationData(context: Context) {
        context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

}
