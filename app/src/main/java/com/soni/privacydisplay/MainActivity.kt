package com.soni.privacydisplay

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.soni.privacydisplay.ui.PrivacyDisplayTheme
import com.soni.privacydisplay.ui.PrivacyScreen
import com.soni.privacydisplay.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            PrivacyDisplayTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                // Back closes Settings first; on the main screen Back just leaves the app
                // (the overlay keeps running until the user stops it).
                BackHandler(enabled = showSettings) { showSettings = false }
                Crossfade(targetState = showSettings, label = "screen") { settingsOpen ->
                    if (settingsOpen) {
                        SettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
                    } else {
                        PrivacyScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
