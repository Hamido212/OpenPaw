package com.openpaw.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openpaw.app.data.repository.SettingsRepository
import com.openpaw.app.presentation.chat.ChatScreen
import com.openpaw.app.presentation.onboarding.OnboardingScreen
import com.openpaw.app.presentation.settings.SettingsScreen
import com.openpaw.app.presentation.voice.VoiceInputManager
import com.openpaw.app.ui.theme.OpenPawTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var voiceInputManager: VoiceInputManager
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle START_VOICE flag when cold-starting from FloatingBubble / QsTile
        handleVoiceTrigger(intent)
        setContent {
            OpenPawTheme {
                OpenPawNavGraph(settingsRepository = settingsRepository)
            }
        }
    }

    /** Called when app is already running and a new Intent arrives (e.g. FloatingBubble tap). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVoiceTrigger(intent)
    }

    private fun handleVoiceTrigger(intent: Intent?) {
        if (intent?.getBooleanExtra("START_VOICE", false) == true) {
            voiceInputManager.triggerVoiceStart()
        }
    }
}

@Composable
private fun OpenPawNavGraph(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()

    // Observe onboarding state (null while DataStore is loading)
    val isOnboardingDone by settingsRepository.isOnboardingDone.collectAsState(initial = null)

    // Navigate as soon as we know the onboarding state
    LaunchedEffect(isOnboardingDone) {
        when (isOnboardingDone) {
            true  -> navController.navigate("chat") {
                popUpTo("loading") { inclusive = true }
            }
            false -> navController.navigate("onboarding") {
                popUpTo("loading") { inclusive = true }
            }
            null  -> { /* still reading DataStore – stay on splash */ }
        }
    }

    NavHost(navController = navController, startDestination = "loading") {

        // Splash – shown for the brief moment while DataStore loads
        composable("loading") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🐾", fontSize = 72.sp)
            }
        }

        // Onboarding – first launch only
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("chat") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // Main chat
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
