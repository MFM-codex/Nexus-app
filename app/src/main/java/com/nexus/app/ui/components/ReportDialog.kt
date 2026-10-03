package com.nexus.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Reasons a person or post can be reported for: stored value to shown label.
val reportReasons = listOf(
    "spam" to "Spam",
    "harassment" to "Harassment or bullying",
    "fake" to "Fake account",
    "inappropriate" to "Inappropriate content",
    "other" to "Something else",
)

// A small form: pick a reason, optionally explain, send.
@Composable
fun ReportDialog(title: String, onDismiss: () -> Unit, onSubmit: (reason: String, details: String) -> Unit) {
    var reason by remember { mutableStateOf("spam") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                reportReasons.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { reason = key },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == key, onClick = { reason = key })
                        Text(label)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 300) details = it },
                    label = { Text("More details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason, details.trim()) }) { Text("Send report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
