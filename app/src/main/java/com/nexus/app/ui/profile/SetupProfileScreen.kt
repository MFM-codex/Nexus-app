package com.nexus.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.PendingSignup
import com.nexus.app.ui.auth.AuthField
import kotlinx.coroutines.delay

// Shown right after signing in when there is no profile yet
// (first Google sign-in, or a username that was taken during sign-up).
@Composable
fun SetupProfileScreen(vm: ProfileViewModel, displayName: String?, email: String?, onSignOut: () -> Unit) {
    val busy by vm.busy.collectAsState()

    val nameParts = (displayName ?: "").trim().split(" ", limit = 2)
    var firstName by rememberSaveable { mutableStateOf(nameParts.getOrElse(0) { "" }.take(25)) }
    var lastName by rememberSaveable { mutableStateOf(nameParts.getOrElse(1) { "" }.take(24)) }

    val suggestion = PendingSignup.username.ifEmpty {
        (email ?: "").substringBefore('@').lowercase().replace(Regex("[^a-z0-9_]"), "").take(20)
    }
    var username by rememberSaveable { mutableStateOf(suggestion) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    // null = checking or not checked, true = free, false = taken
    var available by remember { mutableStateOf<Boolean?>(null) }
    val formatOk = Regex("^[a-z0-9_]{3,20}$").matches(username)
    LaunchedEffect(username) {
        available = null
        if (formatOk) {
            delay(500)
            available = try {
                vm.isUsernameFree(username)
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Finish setting up",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Choose how you'll appear on Nexus.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField(firstName, { firstName = it.take(25) }, "First name", Modifier.weight(1f))
            AuthField(lastName, { lastName = it.take(24) }, "Last name", Modifier.weight(1f))
        }
        AuthField(
            value = username,
            onValueChange = { text ->
                username = text.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }.take(20)
            },
            label = "Username",
            hint = when {
                !formatOk -> "3-20 letters, numbers or underscore"
                available == true -> "Available \u2713"
                available == false -> "That username is taken"
                else -> "Checking..."
            },
        )

        if (busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && firstName.isNotBlank() && formatOk && available != false,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                error = null
                vm.createProfile(firstName.trim(), lastName.trim(), username) { message -> error = message }
            },
        ) { Text("Continue") }

        TextButton(onClick = onSignOut) { Text("Sign out") }
    }
}
