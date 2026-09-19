package com.loupdate.core

/**
 * Configuration for Loupdate. Point at any S3-compatible private bucket
 * (Backblaze B2 recommended). Use a **read-only** application key in the app.
 */
data class LoupdateConfig(
    /** e.g. https://s3.eu-central-003.backblazeb2.com */
    val endpoint: String,
    /** e.g. eu-central-003 */
    val region: String,
    val bucket: String,
    /** Key ID (read-only) */
    val accessKeyId: String,
    /** Application key secret (read-only) */
    val secretAccessKey: String,
    /**
     * Logical app id = folder prefix in the bucket.
     * Objects: `{appId}/update.json` and `{appId}/app-<versionCode>.apk`
     */
    val appId: String,
    /** Override remote force flag when non-null */
    val forceOverride: Boolean? = null,
    /** Network timeout for manifest check (ms). Soft updates are skipped on timeout. */
    val checkTimeoutMs: Long = 8_000L,
    val theme: LoupdateTheme = LoupdateTheme(),
) {
    init {
        require(appId.isNotBlank()) { "appId must not be blank" }
        require(!appId.contains("..") && !appId.startsWith("/")) { "invalid appId" }
        require(accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            "accessKeyId / secretAccessKey required (use a read-only key)"
        }
    }

    val normalizedEndpoint: String
        get() = endpoint.trimEnd('/')

    fun manifestKey(): String = "$appId/update.json"
}

data class LoupdateTheme(
    /** Accent color ARGB, null = inherit Material primary */
    val accentColor: Int? = null,
)
