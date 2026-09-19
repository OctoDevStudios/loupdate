package com.loupdate.example

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.loupdate.core.Loupdate
import com.loupdate.core.LoupdateConfig
import com.loupdate.core.LoupdateResult

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)

        // Intuitive one-shot config — swap BuildConfig values via secrets.properties
        Loupdate.init(
            this,
            LoupdateConfig(
                endpoint = BuildConfig.LOUPDATE_ENDPOINT,
                region = BuildConfig.LOUPDATE_REGION,
                bucket = BuildConfig.LOUPDATE_BUCKET,
                accessKeyId = BuildConfig.LOUPDATE_KEY_ID,
                secretAccessKey = BuildConfig.LOUPDATE_KEY_SECRET,
                appId = BuildConfig.LOUPDATE_APP_ID,
            )
        )

        status.text = "Checking for updates…"
        Loupdate.check(this) { result ->
            status.text = when (result) {
                is LoupdateResult.UpToDate -> "Up to date (v${Loupdate.currentVersionCode(this)})"
                is LoupdateResult.Skipped -> "Check skipped (timeout / offline)"
                is LoupdateResult.Available ->
                    "Update ${result.manifest.versionName} (force=${result.forced})"
                is LoupdateResult.Error -> "Error: ${result.message}"
            }
        }
    }
}
