package com.loupdate.flutter

import android.app.Activity
import android.content.Context
import com.loupdate.core.Loupdate
import com.loupdate.core.LoupdateConfig
import com.loupdate.core.LoupdateResult
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class LoupdatePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var appContext: Context? = null
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "com.loupdate/flutter")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        appContext = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                val ctx = appContext
                if (ctx == null) {
                    result.error("no_context", "No application context", null)
                    return
                }
                val config = LoupdateConfig(
                    endpoint = call.argument<String>("endpoint")!!,
                    region = call.argument<String>("region")!!,
                    bucket = call.argument<String>("bucket")!!,
                    accessKeyId = call.argument<String>("accessKeyId")!!,
                    secretAccessKey = call.argument<String>("secretAccessKey")!!,
                    appId = call.argument<String>("appId")!!,
                    forceOverride = call.argument<Boolean>("forceOverride"),
                )
                Loupdate.init(ctx, config)
                result.success(null)
            }
            "check" -> {
                val act = activity
                if (act == null) {
                    result.error("no_activity", "No activity attached", null)
                    return
                }
                Loupdate.check(act) { r ->
                    val label = when (r) {
                        is LoupdateResult.UpToDate -> "upToDate"
                        is LoupdateResult.Skipped -> "skipped"
                        is LoupdateResult.Available -> if (r.forced) "force" else "soft"
                        is LoupdateResult.Error -> "error:${r.message}"
                    }
                    act.runOnUiThread { result.success(label) }
                }
            }
            else -> result.notImplemented()
        }
    }
}
