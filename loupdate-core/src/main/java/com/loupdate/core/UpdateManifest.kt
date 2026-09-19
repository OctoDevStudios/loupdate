package com.loupdate.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Remote update manifest stored at `{appId}/update.json`.
 */
data class UpdateManifest(
    val versionCode: Long,
    val versionName: String,
    /** Object key inside the bucket, e.g. my-shop/app-42.apk */
    val apkObject: String,
    val force: Boolean,
    val minVersionCode: Long,
    val title: String,
    val message: String,
    val changelog: List<String>,
    val sha256: String?,
) {
    companion object {
        fun fromJson(raw: String, defaultAppId: String): UpdateManifest {
            val o = JSONObject(raw)
            val versionCode = o.getLong("versionCode")
            val apkObject = when {
                o.has("apkObject") -> o.getString("apkObject")
                o.has("apkUrl") -> {
                    // Allow full URL for flexibility; extract key if same-host path-style not used
                    val url = o.getString("apkUrl")
                    val marker = "/$defaultAppId/"
                    val idx = url.indexOf(marker)
                    if (idx >= 0) url.substring(idx + 1) else "$defaultAppId/app-$versionCode.apk"
                }
                else -> "$defaultAppId/app-$versionCode.apk"
            }
            val changelog = mutableListOf<String>()
            if (o.has("changelog")) {
                val arr = o.getJSONArray("changelog")
                for (i in 0 until arr.length()) changelog += arr.getString(i)
            }
            return UpdateManifest(
                versionCode = versionCode,
                versionName = o.optString("versionName", versionCode.toString()),
                apkObject = apkObject,
                force = o.optBoolean("force", false),
                minVersionCode = o.optLong("minVersionCode", 0L),
                title = o.optString("title", "Update available"),
                message = o.optString("message", "A new version is ready."),
                changelog = changelog,
                sha256 = o.optString("sha256", null)?.takeIf { it.isNotBlank() },
            )
        }
    }
}

sealed class LoupdateResult {
    data object UpToDate : LoupdateResult()
    data object Skipped : LoupdateResult()
    data class Available(val manifest: UpdateManifest, val forced: Boolean) : LoupdateResult()
    data class Error(val message: String, val cause: Throwable? = null) : LoupdateResult()
}
