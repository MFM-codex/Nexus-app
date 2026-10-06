package com.nexus.app.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Event
import com.nexus.app.data.formatEventTime
import com.nexus.app.ui.components.Avatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Menu > Events: upcoming events, soonest first.
@Composable
fun EventsScreen(vm: EventsViewModel, onBack: () -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit) {
    val loaded by vm.items.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val list = loaded.orEmpty()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Events",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onCreate, modifier = Modifier.padding(end = 8.dp)) { Text("Create event") }
            }
            HorizontalDivider()

            LazyColumn(Modifier.fillMaxSize()) {
                if (loaded == null) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (list.isEmpty()) {
                    item {
                        Text(
                            "No upcoming events. Create one!",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(list, key = { it.id }) { event -> EventRow(event) { onOpen(event.id) } }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    val month = event.startAt?.let { SimpleDateFormat("MMM", Locale.getDefault()).format(it).uppercase() } ?: ""
    val day = event.startAt?.let { SimpleDateFormat("d", Locale.getDefault()).format(it) } ?: ""
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(60.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(month, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(event.title, fontWeight = FontWeight.Bold)
                Text(
                    formatEventTime(event.startAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (event.location.isNotBlank()) {
                    Text(
                        event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// One event: details, who's coming, and Going / Interested buttons.
@Composable
fun EventDetailScreen(vm: EventDetailViewModel, myUid: String, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val event = state.event
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (!state.loaded) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (event == null) {
                Text("This event no longer exists.", modifier = Modifier.padding(24.dp))
            } else {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        formatEventTime(event.startAt),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (event.location.isNotBlank()) {
                        Text(event.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.going} going \u00B7 ${state.interested} interested",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.myRsvp == "going") {
                            Button(onClick = { vm.rsvp("going") }, modifier = Modifier.weight(1f)) { Text("Going \u2713") }
                        } else {
                            OutlinedButton(onClick = { vm.rsvp("going") }, modifier = Modifier.weight(1f)) { Text("Going") }
                        }
                        if (state.myRsvp == "interested") {
                            Button(onClick = { vm.rsvp("interested") }, modifier = Modifier.weight(1f)) { Text("Interested \u2713") }
                        } else {
                            OutlinedButton(onClick = { vm.rsvp("interested") }, modifier = Modifier.weight(1f)) { Text("Interested") }
                        }
                    }

                    if (event.description.isNotBlank()) {
                        Spacer(Modifier.height(20.dp))
                        Text("Details", fontWeight = FontWeight.Bold)
                        Text(event.description)
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Hosted by", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Avatar(state.host?.avatarUrl, state.host?.name ?: "?")
                        Spacer(Modifier.width(10.dp))
                        Text(state.host?.name ?: "Unknown host")
                    }

                    if (event.hostId == myUid) {
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Delete event")
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this event?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

// Turn "18/10/2026" + "15:00" into a date (null if it isn't valid).
private fun parseStart(date: String, time: String): Date? =
    try {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        format.isLenient = false
        format.parse(date.trim() + " " + time.trim())
    } catch (e: Exception) {
        null
    }

@Composable
fun NewEventScreen(vm: EventsViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val busy by vm.busy.collectAsState()
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Create event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { if (it.length <= 80) title = it },
            label = { Text("Event name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = date,
                onValueChange = { date = it.filter { c -> c.isDigit() || c == '/' }.take(10) },
                label = { Text("Date") },
                placeholder = { Text("dd/mm/yyyy") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = time,
                onValueChange = { time = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                label = { Text("Time") },
                placeholder = { Text("hh:mm") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "Use 24-hour time, for example 15:30.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedTextField(
            value = location,
            onValueChange = { if (it.length <= 100) location = it },
            label = { Text("Location") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 500) description = it },
            label = { Text("Details") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (busy) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val start = parseStart(date, time)
                if (start == null) {
                    error = "Enter a valid date (dd/mm/yyyy) and time (hh:mm)."
                } else if (!start.after(Date())) {
                    error = "The event must be in the future."
                } else {
                    error = null
                    vm.create(title.trim(), description.trim(), location.trim(), start) { id, message ->
                        if (id != null) onCreated(id) else error = message
                    }
                }
            },
        ) { Text("Create event") }
    }
}
