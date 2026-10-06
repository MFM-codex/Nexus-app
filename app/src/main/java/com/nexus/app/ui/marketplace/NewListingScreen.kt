package com.nexus.app.ui.marketplace

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.app.data.listingCategories

// Sell something: photo, title, price, category, place, phone number, description.
@Composable
fun NewListingScreen(vm: MarketplaceViewModel, onDone: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val resolver = LocalContext.current.contentResolver

    var imageUrl by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(listingCategories.last()) }
    var location by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            error = null
            vm.uploadImage(resolver, uri) { result ->
                result.onSuccess { imageUrl = it }.onFailure { error = it.message ?: "Upload failed." }
            }
        }
    }

    val priceValue = price.toLongOrNull()
    val canPost = !busy && imageUrl.isNotEmpty() && title.isNotBlank() && priceValue != null

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Sell something", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Item photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (imageUrl.isEmpty()) "Add a photo (required)" else "Change photo") }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { if (it.length <= 80) title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = price,
            onValueChange = { text -> price = text.filter { it.isDigit() }.take(9) },
            label = { Text("Price in naira") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Text("Category", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listingCategories.forEach { name ->
                FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
            }
        }

        OutlinedTextField(
            value = location,
            onValueChange = { if (it.length <= 60) location = it },
            label = { Text("Location (for example Minna)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = contact,
            onValueChange = { text -> contact = text.filter { it.isDigit() || it == '+' }.take(20) },
            label = { Text("WhatsApp / phone number (optional)") },
            supportingText = { Text("Buyers will see this number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 1000) description = it },
            label = { Text("Description") },
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
            enabled = canPost,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val amount = priceValue
                if (amount != null) {
                    error = null
                    vm.createListing(
                        title.trim(), amount, description.trim(), category,
                        location.trim(), contact.trim(), imageUrl,
                    ) { message ->
                        if (message == null) {
                            onDone()
                        } else {
                            error = message
                        }
                    }
                }
            },
        ) { Text("Post for sale") }
    }
}
