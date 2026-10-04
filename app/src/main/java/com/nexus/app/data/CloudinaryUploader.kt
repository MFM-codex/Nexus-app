package com.nexus.app.data

import android.content.ContentResolver
import android.net.Uri
import com.nexus.app.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Uploads one image to Cloudinary and returns its https link.
// We store only that link in Firestore, never the image itself.
object CloudinaryUploader {
    suspend fun upload(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
        // Shrink the photo first so it uploads fast
        val bytes = ImageCompressor.compress(resolver, uri)

        val boundary = "----nexus" + System.currentTimeMillis()
        val url = URL("https://api.cloudinary.com/v1_1/${Config.CLOUDINARY_CLOUD_NAME}/image/upload")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        // A "multipart" request is how browsers and apps send files over HTTP.
        conn.outputStream.use { out ->
            fun text(s: String) = out.write(s.toByteArray())
            text("--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
            text("${Config.CLOUDINARY_UPLOAD_PRESET}\r\n")
            text("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo\"\r\n")
            text("Content-Type: application/octet-stream\r\n\r\n")
            out.write(bytes)
            text("\r\n--$boundary--\r\n")
        }

        val code = conn.responseCode
        val ok = code in 200..299
        val body = (if (ok) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()

        if (!ok) {
            val reason = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                .getOrDefault("error $code")
            throw IllegalStateException("Upload failed: $reason")
        }
        JSONObject(body).getString("secure_url")
    }

    // Uploads a video and reports progress from 0.0 to 1.0. The file is streamed in small
    // pieces, so even a large video does not fill the phone's memory.
    suspend fun uploadVideo(resolver: ContentResolver, uri: Uri, onProgress: (Float) -> Unit): String =
        withContext(Dispatchers.IO) {
            val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            val boundary = "----nexus" + System.currentTimeMillis()
            val head = (
                "--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n" +
                    "${Config.CLOUDINARY_UPLOAD_PRESET}\r\n" +
                    "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"reel\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n"
                ).toByteArray()
            val tail = "\r\n--$boundary--\r\n".toByteArray()

            val url = URL("https://api.cloudinary.com/v1_1/${Config.CLOUDINARY_CLOUD_NAME}/video/upload")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 180_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                if (size > 0) {
                    setFixedLengthStreamingMode(head.size + size + tail.size)
                } else {
                    setChunkedStreamingMode(64 * 1024)
                }
            }

            conn.outputStream.use { out ->
                out.write(head)
                val input = resolver.openInputStream(uri) ?: throw IllegalStateException("Could not read that video.")
                input.use {
                    val buffer = ByteArray(64 * 1024)
                    var sent = 0L
                    while (true) {
                        val read = it.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        sent += read
                        if (size > 0) onProgress((sent.toFloat() / size).coerceIn(0f, 1f))
                    }
                }
                out.write(tail)
            }

            val code = conn.responseCode
            val ok = code in 200..299
            val body = (if (ok) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            if (!ok) {
                val reason = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                    .getOrDefault("error $code")
                throw IllegalStateException("Upload failed: $reason")
            }
            JSONObject(body).getString("secure_url")
        }
}
