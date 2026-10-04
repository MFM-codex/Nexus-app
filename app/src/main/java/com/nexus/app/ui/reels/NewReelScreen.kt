package com.nexus.app.ui.reels

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MAX_SECONDS = 60
private const val MAX_BYTES = 100L * 1024 * 1024

// How long is this video, in milliseconds? (0 if we can't tell)
private fun videoDurationMs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    } finally {
        retriever.release()
    }
}

// Pick a video, write a caption, upload.
@Composable
fun NewReelScreen(vm: ReelsViewModel, onDone: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val progress by vm.progress.collectAsState()
    val context = LocalContext.current

    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var info by rememberSaveable { mutableStateOf("") }
    var caption by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            error = null
            val durationMs = videoDurationMs(context, uri)
            val sizeBytes = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (durationMs > MAX_SECONDS * 1000L) {
                videoUri = null
                error = "That video is longer than $MAX_SECONDS seconds. Pick a shorter one."
            } else if (sizeBytes > MAX_BYTES) {
                videoUri = null
                error = "That video is bigger than 100 MB. Pick a smaller or shorter one."
            } else {
                videoUri = uri
                info = "Video selected: ${durationMs / 1000} seconds, ${sizeBytes / 1_000_000} MB"
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone, enabled = !busy) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("New reel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (videoUri == null) "Choose a video" else "Choose a different video") }

        if (videoUri != null) {
            Spacer(Modifier.height(8.dp))
            Text(info, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Up to $MAX_SECONDS seconds and 100 MB. Shorter videos upload faster, and on slow data " +
                "a 720p video (camera settings) uploads much faster than 1080p.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = caption,
            onValueChange = { if (it.length <= 150) caption = it },
            label = { Text("Caption (optional)") },
            supportingText = { Text("${caption.length}/150") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        if (busy) {
            Spacer(Modifier.height(12.dp))
            val fraction = progress
            if (fraction != null && fraction > 0f) {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text("Uploading... ${(fraction * 100).toInt()}%")
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Uploading...")
            }
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && videoUri != null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val uri = videoUri
                if (uri != null) {
                    error = null
                    vm.uploadReel(context.contentResolver, uri, caption.trim()) { message ->
                        if (message == null) {
                            onDone()
                        } else {
                            error = message
                        }
                    }
                }
            },
        ) { Text("Post reel") }
    }
}
