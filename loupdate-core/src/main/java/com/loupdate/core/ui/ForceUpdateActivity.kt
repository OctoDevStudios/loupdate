package com.loupdate.core.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.loupdate.core.Loupdate
import com.loupdate.core.R
import com.loupdate.core.UpdateManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForceUpdateActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loupdate_force)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Block back — force update
            }
        })

        val manifest = ManifestHolder.current
        if (manifest == null) {
            finish()
            return
        }
        bind(manifest)
    }

    private fun bind(manifest: UpdateManifest) {
        findViewById<TextView>(R.id.loupdateTitle).text = manifest.title
        findViewById<TextView>(R.id.loupdateMessage).text =
            "${manifest.message}\n\nv${manifest.versionName}"
        val changelog = findViewById<TextView>(R.id.loupdateChangelog)
        if (manifest.changelog.isEmpty()) {
            changelog.visibility = View.GONE
        } else {
            changelog.text = manifest.changelog.joinToString("\n") { "• $it" }
        }

        val progress = findViewById<ProgressBar>(R.id.loupdateProgress)
        val status = findViewById<TextView>(R.id.loupdateStatus)
        val action = findViewById<MaterialButton>(R.id.loupdateAction)

        action.setOnClickListener {
            action.isEnabled = false
            progress.visibility = View.VISIBLE
            status.visibility = View.VISIBLE
            status.setText(R.string.loupdate_downloading)

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        Loupdate.downloadAndInstall(this@ForceUpdateActivity, manifest) { p ->
                            runOnUiThread {
                                if (p < 0f) {
                                    progress.isIndeterminate = true
                                } else {
                                    progress.isIndeterminate = false
                                    progress.progress = (p * 1000).toInt()
                                }
                            }
                        }
                    }
                    status.setText(R.string.loupdate_installing)
                } catch (e: Exception) {
                    status.text = getString(R.string.loupdate_error) + ": ${e.message}"
                    action.isEnabled = true
                    action.setText(R.string.loupdate_retry)
                }
            }
        }
    }

    companion object {
        fun start(context: Context, manifest: UpdateManifest) {
            ManifestHolder.current = manifest
            context.startActivity(
                Intent(context, ForceUpdateActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
