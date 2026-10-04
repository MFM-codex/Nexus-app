package com.nexus.app.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val helpTopics = listOf(
    "Who can see my posts?" to "Only you and your friends. Reels are different: everyone on Nexus can see them.",
    "How do I add friends?" to "Open the Friends tab, search for a username, and tap Add. They'll see your request under Requests.",
    "How do I report something?" to "Tap the three dots on a post or reel, or on someone's profile, then choose Report. Our admin reviews every report.",
    "How do I block someone?" to "Open their profile, tap the three dots, then Block. You can undo it in Menu > Settings & privacy > Blocked people.",
    "How do I save a post?" to "Tap the bookmark on a post. Find it later in Menu > Saved.",
    "Reels use too much data" to "Turn on Data saver in Settings & privacy. Reels then wait for a tap before playing, and shorter videos use less data.",
    "My video won't upload" to "Videos can be up to 60 seconds and 100 MB. On slow data, a 720p video uploads much faster than a 1080p one.",
    "I forgot my password" to "Log in with Google if you signed up with it. Otherwise, ask the admin of this app to help.",
)

// Menu > Help & support
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Help & support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()

        helpTopics.forEach { (question, answer) ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(question, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(answer, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
        Text(
            "Still stuck? Tell the admin of Nexus what happened.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
