package com.wepool.app.utils

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

    fun buildNavigationNotification(context: Context, contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("WePool 🚘")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // שנה לאייקון משלך אם צריך
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
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
}
