package com.nexus.app.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Relation

// The right button(s) for how I'm connected to someone.
// compact = true is the small version used in search results.
@Composable
fun RelationActions(
    relation: Relation,
    name: String,
    compact: Boolean,
    onAdd: () -> Unit,
    onAccept: () -> Unit,
    onRemove: () -> Unit, // decline, cancel request, or unfriend
) {
    var confirmUnfriend by remember { mutableStateOf(false) }

    when (relation) {
        Relation.NONE -> Button(onClick = onAdd) { Text(if (compact) "Add" else "Add friend") }

        Relation.OUTGOING ->
            if (compact) {
                Text("Requested", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OutlinedButton(onClick = onRemove) { Text("Cancel request") }
            }

        Relation.INCOMING ->
            if (compact) {
                Button(onClick = onAccept) { Text("Accept") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAccept) { Text("Accept") }
                    OutlinedButton(onClick = onRemove) { Text("Decline") }
                }
            }

        Relation.FRIEND ->
            if (compact) {
                Text("Friends", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OutlinedButton(onClick = { confirmUnfriend = true }) { Text("Unfriend") }
            }
    }

    if (confirmUnfriend) {
        AlertDialog(
            onDismissRequest = { confirmUnfriend = false },
            title = { Text("Unfriend $name?") },
            text = { Text("You won't see each other's posts anymore.") },
            confirmButton = {
                TextButton(onClick = { confirmUnfriend = false; onRemove() }) { Text("Unfriend") }
            },
            dismissButton = { TextButton(onClick = { confirmUnfriend = false }) { Text("Cancel") } },
        )
    }
}
