package com.loupdate.core.internal

import com.loupdate.core.LoupdateConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal S3-compatible GET client (AWS SigV4). Works with Backblaze B2, R2, Tigris, MinIO, …
 */
internal class S3Client(
    private val config: LoupdateConfig,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    fun getObjectUtf8(key: String): String {
        getObjectStream(key).use { stream ->
            return stream.readBytes().toString(StandardCharsets.UTF_8)
        }
    }

    fun getObjectStream(key: String): InputStream {
        val response = executeGet(key)
        if (!response.isSuccessful) {
            val body = response.body?.string().orEmpty()
            response.close()
            error("S3 GET failed ${response.code} for $key: $body")
        }
        return response.body!!.byteStream()
    }

    fun getObjectWithLength(key: String): Pair<InputStream, Long?> {
        val response = executeGet(key)
        if (!response.isSuccessful) {
            val body = response.body?.string().orEmpty()
            response.close()
            error("S3 GET failed ${response.code} for $key: $body")
        }
        val length = response.body?.contentLength()?.takeIf { it >= 0 }
        return response.body!!.byteStream() to length
    }

    private fun executeGet(key: String): Response {
        val encodedKey = key.split("/").joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }
        // Path-style: https://endpoint/bucket/key — most compatible with B2 app keys
        val url = "${config.normalizedEndpoint}/${config.bucket}/$encodedKey"
        val host = hostOf(config.normalizedEndpoint)
        val amzDate = amzDateNow()
        val dateStamp = amzDate.substring(0, 8)
        val payloadHash = EMPTY_PAYLOAD_HASH
        val canonicalUri = "/${config.bucket}/$encodedKey"
        val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val canonicalRequest = "GET\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val credentialScope = "$dateStamp/${config.region}/s3/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest)}"
        val signature = hmacHex(signingKey(dateStamp), stringToSign)
        val authorization =
            "AWS4-HMAC-SHA256 Credential=${config.accessKeyId}/$credentialScope, " +
                "SignedHeaders=$signedHeaders, Signature=$signature"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Host", host)
            .header("x-amz-content-sha256", payloadHash)
            .header("x-amz-date", amzDate)
            .header("Authorization", authorization)
            .build()

        return http.newCall(request).execute()
    }

    private fun signingKey(dateStamp: String): ByteArray {
        val kDate = hmac(("AWS4" + config.secretAccessKey).toByteArray(), dateStamp)
        val kRegion = hmac(kDate, config.region)
        val kService = hmac(kRegion, "s3")
        return hmac(kService, "aws4_request")
    }

    private fun hostOf(endpoint: String): String {
        val withoutScheme = endpoint.removePrefix("https://").removePrefix("http://")
        return withoutScheme.substringBefore('/')
    }

    private fun amzDateNow(): String {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun sha256Hex(data: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun hmac(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun hmacHex(key: ByteArray, data: String): String =
        hmac(key, data).joinToString("") { "%02x".format(it) }

    companion object {
        private const val EMPTY_PAYLOAD_HASH =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
}
