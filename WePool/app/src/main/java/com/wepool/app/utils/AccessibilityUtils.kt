package com.wepool.app.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

object AccessibilityUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = "${context.packageName}/com.wepool.app.infrastructure.navigation.GoogleMapsWatcherService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").contains(service)
    }

    fun requestAccessibilityPermission(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
