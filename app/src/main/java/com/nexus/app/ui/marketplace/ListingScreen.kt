package com.nexus.app.ui.marketplace

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.app.data.formatPrice
import com.nexus.app.data.whatsappLink
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.ReportDialog
import com.nexus.app.ui.components.timeAgo

// One item for sale: photo, price, description, and buttons to contact the seller.
@Composable
fun ListingScreen(vm: ListingViewModel, myUid: String, onBack: () -> Unit) {
    val listing by vm.listing.collectAsState()
    val seller by vm.seller.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val message by vm.message.collectAsState()
    val deleted by vm.deleted.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var menuOpen by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // no app can handle it
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                if (listing != null) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (listing?.sellerId == myUid) {
                                DropdownMenuItem(
                                    text = { Text("Delete listing") },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Report listing") },
                                    onClick = { menuOpen = false; reporting = true },
                                )
                            }
                        }
                    }
                }
            }

            val item = listing
            if (!loaded) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (item == null) {
                Text("This listing is no longer available.", modifier = Modifier.padding(24.dp))
            } else {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(Modifier.padding(16.dp)) {
                    Text(
                        formatPrice(item.price),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOf(item.category, item.location, timeAgo(item.createdAt))
                            .filter { it.isNotBlank() }.joinToString(" \u00B7 "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.sold) {
                        Text(
                            "This item has been sold.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (item.description.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Description", fontWeight = FontWeight.Bold)
                        Text(item.description)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Seller", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Avatar(seller?.avatarUrl, seller?.name ?: "?")
                        Spacer(Modifier.width(10.dp))
                        Text(seller?.name ?: "Unknown seller")
                    }

                    Spacer(Modifier.height(20.dp))
                    if (item.sellerId == myUid) {
                        OutlinedButton(onClick = { vm.toggleSold() }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (item.sold) "Mark as available" else "Mark as sold")
                        }
                    } else if (item.contact.isNotBlank() && !item.sold) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val link = whatsappLink(item.contact)
                            if (link != null) {
                                Button(
                                    onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("WhatsApp seller") }
                            }
                            OutlinedButton(
                                onClick = { open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.contact))) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Call") }
                        }
                    } else if (!item.sold) {
                        Text(
                            "The seller didn't add a phone number.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Meet in a public place and check the item before you pay. Never pay in advance to someone you don't know.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (reporting) {
        ReportDialog(
            title = "Report this listing",
            onDismiss = { reporting = false },
        ) { reason, details ->
            reporting = false
            vm.report(reason, details)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this listing?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
