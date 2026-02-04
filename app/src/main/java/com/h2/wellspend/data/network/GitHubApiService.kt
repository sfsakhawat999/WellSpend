package com.h2.wellspend.data.network

import androidx.annotation.Keep
import com.h2.wellspend.data.model.GitHubRelease
import retrofit2.http.GET
import retrofit2.http.Path

@Keep
interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRelease
}
