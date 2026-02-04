package com.h2.wellspend.data.repository

import android.content.Context
import android.util.Log
import com.h2.wellspend.BuildConfig
import com.h2.wellspend.data.model.GitHubRelease
import com.h2.wellspend.data.network.GitHubApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UpdateRepository(context: Context) {

    private val apiService: GitHubApiService
    private val updatePreferences: UpdatePreferences

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GitHubApiService::class.java)
        updatePreferences = UpdatePreferences(context)
    }

    suspend fun checkForUpdate(owner: String, repo: String, forceCheck: Boolean = false): Result<GitHubRelease?> {
        // Check if we should use cached result
        if (!forceCheck && !updatePreferences.shouldCheckForUpdate()) {
            Log.d("UpdateRepository", "Using cached update result")
            return Result.success(updatePreferences.getCachedRelease())
        }

        return try {
            Log.d("UpdateRepository", "Fetching update from GitHub API")
            val release = apiService.getLatestRelease(owner, repo)
            val currentVersion = BuildConfig.VERSION_NAME
            
            // Basic version comparison logic
            // Assuming semantic versioning (v1.0.0 or 1.0.0)
            val cleanTagName = release.tagName.removePrefix("v")
            val cleanCurrentVersion = currentVersion.removePrefix("v")

            val result = if (isNewerVersion(cleanTagName, cleanCurrentVersion)) {
                release
            } else {
                null // No update available
            }

            // Cache the result
            updatePreferences.saveUpdateResult(result)
            Result.success(result)
        } catch (e: Exception) {
            Log.e("UpdateRepository", "Error checking for update", e)
            // On error, try to return cached result if available
            val cached = updatePreferences.getCachedRelease()
            if (cached != null) {
                Log.d("UpdateRepository", "Returning cached result due to API error")
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(newParts.size, currentParts.size)
        
        for (i in 0 until length) {
            val newPart = if (i < newParts.size) newParts[i] else 0
            val currentPart = if (i < currentParts.size) currentParts[i] else 0
            
            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }
        
        return false
    }
}
