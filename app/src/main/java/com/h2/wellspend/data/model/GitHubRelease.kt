package com.h2.wellspend.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("assets") val assets: List<GitHubAsset> = emptyList()
)

@Keep
data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Int,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("content_type") val contentType: String
)
