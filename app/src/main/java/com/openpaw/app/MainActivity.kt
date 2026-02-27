package com.openpaw.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openpaw.app.presentation.chat.ChatScreen
import com.openpaw.app.presentation.settings.SettingsScreen
import com.openpaw.app.presentation.voice.VoiceInputManager
import com.openpaw.app.ui.theme.OpenPawTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var voiceInputManager: VoiceInputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle START_VOICE flag when app cold-starts from FloatingBubble / QsTile
        handleVoiceTrigger(intent)
        setContent {
            OpenPawTheme {
                OpenPawNavGraph()
            }
        }
    }

    /**
     * Called when the Activity is already running and a new Intent arrives,
     * e.g. user taps the floating bubble while app is in the background.
     */
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
private fun OpenPawNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
