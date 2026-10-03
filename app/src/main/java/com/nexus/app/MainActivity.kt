package com.nexus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.app.ui.auth.AuthViewModel
import com.nexus.app.ui.auth.LoginScreen
import com.nexus.app.ui.main.MainScaffold
import com.nexus.app.ui.theme.NexusTheme

// The single screen container for the whole app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NexusTheme { NexusRoot() } }
    }
}

// Decides what to show: login screen if signed out, main app if signed in.
@Composable
fun NexusRoot() {
    val authVm: AuthViewModel = viewModel()
    val user by authVm.user.collectAsState()
    val u = user
    if (u == null) {
        LoginScreen(authVm)
    } else {
        MainScaffold(user = u, onSignOut = authVm::signOut)
    }
}
