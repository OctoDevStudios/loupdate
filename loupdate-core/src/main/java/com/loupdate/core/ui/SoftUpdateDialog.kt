package com.loupdate.core.ui

import android.app.Activity
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.loupdate.core.Loupdate
import com.loupdate.core.R
import com.loupdate.core.UpdateManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object SoftUpdateDialog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun show(activity: Activity, manifest: UpdateManifest) {
        if (activity.isFinishing) return

        val view = LayoutInflater.from(activity).inflate(R.layout.loupdate_update_content, null)
        view.findViewById<TextView>(R.id.loupdateTitle).text = manifest.title
        view.findViewById<TextView>(R.id.loupdateMessage).text =
            "${manifest.message}\n\nv${manifest.versionName} (${manifest.versionCode})"
        val changelog = view.findViewById<TextView>(R.id.loupdateChangelog)
        if (manifest.changelog.isEmpty()) {
            changelog.visibility = android.view.View.GONE
        } else {
            changelog.text = manifest.changelog.joinToString("\n") { "• $it" }
        }
        val progress = view.findViewById<LinearProgressIndicator>(R.id.loupdateProgress)
        val status = view.findViewById<TextView>(R.id.loupdateStatus)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setPositiveButton(R.string.loupdate_update, null)
            .setNegativeButton(R.string.loupdate_later) { d, _ -> d.dismiss() }
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                dialog.setCancelable(false)
                dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false
                positive.isEnabled = false
                progress.visibility = android.view.View.VISIBLE
                status.visibility = android.view.View.VISIBLE
                status.setText(R.string.loupdate_downloading)

                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            Loupdate.downloadAndInstall(activity, manifest) { p ->
                                activity.runOnUiThread {
                                    if (p < 0f) {
                                        progress.isIndeterminate = true
                                    } else {
                                        progress.isIndeterminate = false
                                        progress.progress = (p * 100).toInt()
                                    }
                                }
                            }
                        }
                        status.setText(R.string.loupdate_installing)
                    } catch (e: Exception) {
                        status.text = activity.getString(R.string.loupdate_error) + ": ${e.message}"
                        positive.isEnabled = true
                        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.isEnabled = true
                        dialog.setCancelable(true)
                        positive.setText(R.string.loupdate_retry)
                    }
                }
            }
        }
        dialog.show()
    }
}
