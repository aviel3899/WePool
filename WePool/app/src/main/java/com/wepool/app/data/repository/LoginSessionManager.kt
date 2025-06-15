package com.wepool.app.data.repository

import android.content.Context
import android.content.SharedPreferences

object LoginSessionManager {
    private const val PREFS_NAME = "login_prefs"
    private const val KEY_DID_LOGIN_MANUALLY = "did_login_manually"

    fun setDidLoginManually(context: Context, value: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DID_LOGIN_MANUALLY, value).apply()
    }

    fun didLoginManually(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DID_LOGIN_MANUALLY, false)
    }

    fun clearLoginFlag(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DID_LOGIN_MANUALLY).apply()
    }
}