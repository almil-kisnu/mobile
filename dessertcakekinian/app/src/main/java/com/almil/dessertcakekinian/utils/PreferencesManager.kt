package com.almil.dessertcakekinian.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_preferences"

        // Product Sync Keys
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"

        // Order Sync Keys
        private const val KEY_FIRST_ORDER_LAUNCH = "first_order_launch"
        private const val KEY_LAST_ORDER_SYNC_TIME = "last_order_sync_time"
    }

    // ========== PRODUCT PREFERENCES ==========

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunch(isFirst: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }

    fun clearSyncData() {
        prefs.edit().apply {
            remove(KEY_FIRST_LAUNCH)
            remove(KEY_LAST_SYNC_TIME)
            apply()
        }
    }

    // ========== ORDER PREFERENCES ==========

    fun isFirstOrderLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_ORDER_LAUNCH, true)
    }

    fun setFirstOrderLaunch(isFirst: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_ORDER_LAUNCH, isFirst).apply()
    }

    fun getLastOrderSyncTime(): Long {
        return prefs.getLong(KEY_LAST_ORDER_SYNC_TIME, 0L)
    }

    fun setLastOrderSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_ORDER_SYNC_TIME, time).apply()
    }

    fun clearOrderSyncData() {
        prefs.edit().apply {
            remove(KEY_FIRST_ORDER_LAUNCH)
            remove(KEY_LAST_ORDER_SYNC_TIME)
            apply()
        }
    }

    // ========== CLEAR ALL DATA ==========

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}