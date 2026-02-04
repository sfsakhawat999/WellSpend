package com.h2.wellspend.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.h2.wellspend.BuildConfig
import com.h2.wellspend.data.model.GitHubRelease

class UpdatePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK_TIMESTAMP = "last_check_timestamp"
        private const val KEY_CACHED_RELEASE_JSON = "cached_release_json"
        private const val KEY_LAST_CHECKED_VERSION = "last_checked_version"
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour
    }

    fun shouldCheckForUpdate(): Boolean {
        val lastCheckTimestamp = prefs.getLong(KEY_LAST_CHECK_TIMESTAMP, 0)
        val lastCheckedVersion = prefs.getString(KEY_LAST_CHECKED_VERSION, null)
        val currentVersion = BuildConfig.VERSION_NAME
        val now = System.currentTimeMillis()

        // Check if version changed (user updated the app)
        if (lastCheckedVersion != currentVersion) {
            return true
        }

        // Check if cache expired (more than 24 hours)
        if (now - lastCheckTimestamp > CACHE_DURATION_MS) {
            return true
        }

        return false
    }

    fun getCachedRelease(): GitHubRelease? {
        val json = prefs.getString(KEY_CACHED_RELEASE_JSON, null) ?: return null
        return try {
            gson.fromJson(json, GitHubRelease::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUpdateResult(release: GitHubRelease?) {
        val currentVersion = BuildConfig.VERSION_NAME
        val now = System.currentTimeMillis()

        prefs.edit()
            .putLong(KEY_LAST_CHECK_TIMESTAMP, now)
            .putString(KEY_LAST_CHECKED_VERSION, currentVersion)
            .putString(KEY_CACHED_RELEASE_JSON, release?.let { gson.toJson(it) })
            .apply()
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }
}
