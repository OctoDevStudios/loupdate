package com.loupdate.core.internal

import android.content.Context
import com.loupdate.core.UpdateManifest
import java.io.File
import java.security.MessageDigest

internal class ApkDownloader(
    private val context: Context,
    private val client: S3Client,
) {
    fun download(manifest: UpdateManifest, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "loupdate").apply { mkdirs() }
        val out = File(dir, "update-${manifest.versionCode}.apk")
        if (out.exists()) out.delete()

        val (stream, length) = client.getObjectWithLength(manifest.apkObject)
        stream.use { input ->
            out.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var readTotal = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    readTotal += read
                    if (length != null && length > 0) {
                        onProgress((readTotal.toFloat() / length).coerceIn(0f, 1f))
                    } else {
                        onProgress(-1f)
                    }
                }
                output.flush()
            }
        }
        onProgress(1f)

        manifest.sha256?.let { expected ->
            val actual = sha256Hex(out)
            check(actual.equals(expected, ignoreCase = true)) {
                "APK sha256 mismatch (expected $expected, got $actual)"
            }
        }
        return out
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
