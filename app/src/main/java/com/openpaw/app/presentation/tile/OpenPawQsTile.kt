package com.openpaw.app.presentation.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.openpaw.app.MainActivity
import com.openpaw.app.presentation.voice.VoiceInputManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Quick Settings Tile — the 🐾 tile in the notification pull-down panel.
 *
 * Tap: fires VoiceInputManager.triggerVoiceStart() + opens OpenPaw.
 * No tile state tracking needed (it's always "active").
 *
 * To add: pull down panel → long-press the tile list → drag 🐾 into the active area.
 * Android 13+: use the "Add Tile" button in Settings → Quick Access section.
 */
@AndroidEntryPoint
class OpenPawQsTile : TileService() {

    @Inject lateinit var voiceInputManager: VoiceInputManager

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "OpenPaw"
            contentDescription = "OpenPaw AI Agent – Spracheingabe starten"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        voiceInputManager.triggerVoiceStart()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {   // API 34
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
