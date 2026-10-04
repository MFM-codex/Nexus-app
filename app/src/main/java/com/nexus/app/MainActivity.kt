package com.nexus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.app.ui.auth.AuthViewModel
import com.nexus.app.ui.auth.LoginScreen
import com.nexus.app.ui.auth.SignUpScreen
import com.nexus.app.ui.auth.SplashScreen
import com.nexus.app.ui.auth.WelcomeScreen
import com.nexus.app.ui.main.MainScaffold
import com.nexus.app.ui.theme.NexusTheme
import kotlinx.coroutines.delay

// The single screen container for the whole app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NexusTheme { NexusRoot() } }
    }
}

// Decides what to show:
// splash (loading dots) -> if signed in: the app
//                       -> if signed out: welcome -> log in or create account
@Composable
fun NexusRoot() {
    val authVm: AuthViewModel = viewModel()
    val user by authVm.user.collectAsState()

    var splashDone by rememberSaveable { mutableStateOf(false) }
    var welcomed by rememberSaveable { mutableStateOf(false) }
    var showSignUp by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1600)
        splashDone = true
    }

    val u = user
    when {
        !splashDone -> SplashScreen()
        u != null -> MainScaffold(user = u, onSignOut = authVm::signOut)
        !welcomed -> WelcomeScreen(
            onLogin = { showSignUp = false; welcomed = true },
            onCreateAccount = { showSignUp = true; welcomed = true },
        )
        showSignUp -> SignUpScreen(authVm, onSwitchToLogin = { showSignUp = false })
        else -> LoginScreen(authVm, onSwitchToSignUp = { showSignUp = true })
    }
}
