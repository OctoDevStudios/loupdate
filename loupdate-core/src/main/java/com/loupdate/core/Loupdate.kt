package com.loupdate.core

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.loupdate.core.internal.ApkDownloader
import com.loupdate.core.internal.ApkInstaller
import com.loupdate.core.internal.S3Client
import com.loupdate.core.ui.SoftUpdateDialog
import com.loupdate.core.ui.ForceUpdateActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Entry point. Call [init] once, then [check] on launch.
 */
object Loupdate {
    @Volatile
    private var config: LoupdateConfig? = null

    @Volatile
    private var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(context: Context, config: LoupdateConfig) {
        this.appContext = context.applicationContext
        this.config = config
    }

    fun isInitialized(): Boolean = config != null

    /**
     * Checks for an update and shows soft/force UI when needed.
     * Safe to call from the main thread.
     */
    @JvmOverloads
    fun check(activity: Activity, callback: ((LoupdateResult) -> Unit)? = null) {
        val cfg = config
        val ctx = appContext
        if (cfg == null || ctx == null) {
            callback?.invoke(LoupdateResult.Error("Loupdate.init() was not called"))
            return
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { fetchDecision(ctx, cfg) }
                    .getOrElse { LoupdateResult.Error(it.message ?: "check failed", it) }
            }

            when (result) {
                is LoupdateResult.Available -> {
                    if (result.forced) {
                        ForceUpdateActivity.start(activity, result.manifest)
                    } else {
                        SoftUpdateDialog.show(activity, result.manifest)
                    }
                }
                else -> Unit
            }
            callback?.invoke(result)
        }
    }

    /**
     * Headless check (no UI). Useful for Flutter / custom UI.
     */
    suspend fun checkSuspend(context: Context): LoupdateResult {
        val cfg = config ?: return LoupdateResult.Error("Loupdate.init() was not called")
        return withContext(Dispatchers.IO) {
            runCatching { fetchDecision(context.applicationContext, cfg) }
                .getOrElse { LoupdateResult.Error(it.message ?: "check failed", it) }
        }
    }

    internal fun requireConfig(): LoupdateConfig =
        config ?: error("Loupdate.init() was not called")

    private suspend fun fetchDecision(context: Context, cfg: LoupdateConfig): LoupdateResult {
        val client = S3Client(cfg)
        val raw = withTimeoutOrNull(cfg.checkTimeoutMs) {
            client.getObjectUtf8(cfg.manifestKey())
        } ?: return LoupdateResult.Skipped

        val manifest = UpdateManifest.fromJson(raw, cfg.appId)
        val local = currentVersionCode(context)

        if (manifest.versionCode <= local) {
            return LoupdateResult.UpToDate
        }

        val forced = cfg.forceOverride
            ?: (manifest.force || local < manifest.minVersionCode)

        return LoupdateResult.Available(manifest, forced)
    }

    fun currentVersionCode(context: Context): Long {
        val pm = context.packageManager
        val pkg = context.packageName
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    internal suspend fun downloadAndInstall(
        context: Context,
        manifest: UpdateManifest,
        onProgress: (Float) -> Unit,
    ) {
        val cfg = requireConfig()
        val client = S3Client(cfg)
        val downloader = ApkDownloader(context, client)
        val file = downloader.download(manifest, onProgress)
        ApkInstaller.install(context, file)
    }
}
