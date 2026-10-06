package com.nexus.app.ui.marketplace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.app.data.Listing
import com.nexus.app.data.formatPrice
import com.nexus.app.data.listingCategories

// The Marketplace tab: "Today's picks" in two columns, like Facebook.
@Composable
fun MarketplaceScreen(vm: MarketplaceViewModel, onSell: () -> Unit, onOpen: (String) -> Unit) {
    val loaded by vm.items.collectAsState()
    val errorText by vm.error.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var category by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(errorText) {
        errorText?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    val list = loaded.orEmpty().filter { item ->
        (category == "All" || item.category == category) &&
            (query.isBlank() || item.title.contains(query, ignoreCase = true))
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Marketplace",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = onSell) { Text("Sell") }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search Marketplace") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (listOf("All") + listingCategories).forEach { name ->
                            FilterChip(
                                selected = category == name,
                                onClick = { category = name },
                                label = { Text(name) },
                            )
                        }
                    }
                    Text(
                        "Today's picks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            if (loaded == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (list.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Nothing here yet. Tap Sell to list the first item!",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(list, key = { it.id }) { item ->
                ListingCard(item) { onOpen(item.id) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ListingCard(item: Listing, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (item.sold) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    ) {
                        Text(
                            "SOLD",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(formatPrice(item.price), fontWeight = FontWeight.Bold)
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.location.isNotBlank()) {
                    Text(
                        item.location,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
