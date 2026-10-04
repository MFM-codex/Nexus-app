package com.nexus.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

// One rounded text box used on both the login and sign-up pages.
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    hint: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = { if (hint != null) Text(hint) },
        modifier = modifier.fillMaxWidth(),
    )
}

// The Log in page.
@Composable
fun LoginScreen(vm: AuthViewModel, onSwitchToSignUp: () -> Unit) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "nexus",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Log in to continue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            AuthField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(4.dp))
            AuthField(password, { password = it }, "Password", password = true)
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { vm.signIn(email, password) },
                enabled = !loading && email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Log in") }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { vm.signInWithGoogle(context.findActivity()) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Continue with Google") }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onSwitchToSignUp,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Create new account") }
        }
    }
}

// The Create account page: first name, last name, username, email, password.
@Composable
fun SignUpScreen(vm: AuthViewModel, onSwitchToLogin: () -> Unit) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val usernameOk = Regex("^[a-z0-9_]{3,20}$").matches(username)
    val canSubmit = !loading &&
        firstName.isNotBlank() && lastName.isNotBlank() &&
        usernameOk && email.contains("@") && password.length >= 6

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "nexus",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Create your account",
                style = MaterialTheme.typography.titleMedium,
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
                hint = "3-20 letters, numbers or underscore",
            )
            AuthField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
            AuthField(password, { password = it }, "Password", password = true, hint = "At least 6 characters")
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.signUp(firstName, lastName, username, email, password) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Sign up") }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { vm.signInWithGoogle(context.findActivity()) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Sign up with Google") }

            TextButton(onClick = onSwitchToLogin) { Text("Already have an account? Log in") }
        }
    }
}

// Compose gives us a Context; Google sign-in needs the Activity that wraps it.
fun Context.findActivity(): Activity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    throw IllegalStateException("No Activity found")
}
