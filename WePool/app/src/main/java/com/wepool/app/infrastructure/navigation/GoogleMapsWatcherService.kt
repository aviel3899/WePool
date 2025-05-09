package com.wepool.app.infrastructure.navigation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class GoogleMapsWatcherService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = arrayOf("com.google.android.apps.maps")
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        Log.d("GoogleMapsWatcher", "✅ Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()
        if (packageName != "com.google.android.apps.maps") {
            Log.d("GoogleMapsWatcher", "🛑 Google Maps exited or moved to background")
            stopForegroundNavigationService()
        }
    }

    private fun stopForegroundNavigationService() {
        val intent = Intent(this, RideNavigationForegroundService::class.java).apply {
            action = RideNavigationForegroundService.ACTION_STOP
        }
        stopService(intent)
    }

    override fun onInterrupt() {
        Log.w("GoogleMapsWatcher", "⚠ Accessibility service interrupted")
    }
}
