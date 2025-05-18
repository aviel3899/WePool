package com.wepool.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
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
            createNotificationChannel(context)
        }

        val intent = android.content.Intent(context, com.wepool.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("rideId", rideId)
            putExtra("screen", screen)
            putExtra("fromNotification", true)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("WePool 🚘")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            //.setContentIntent(pendingIntent) // כדי שלא יהיה לחיץ אז זה בהערות
            .build()
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

        val intent = android.content.Intent(context, com.wepool.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("rideId", rideId)
            putExtra("screen", screen)
            putExtra("fromNotification", true)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
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

    fun storeNotificationNavigationData(context: Context, screen: String?, rideId: String?) {
        val prefs = context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("screen", screen)
            putString("rideId", rideId)
            apply()
        }
    }

    fun getStoredNotificationNavigationData(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
        val screen = prefs.getString("screen", null)
        val rideId = prefs.getString("rideId", null)
        return Pair(screen, rideId)
    }

    fun clearNotificationNavigationData(context: Context) {
        context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

}
