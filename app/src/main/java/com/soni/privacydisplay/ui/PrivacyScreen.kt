package com.soni.privacydisplay.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soni.privacydisplay.PrivacySettings
import com.soni.privacydisplay.PrivacyViewModel
import kotlin.math.roundToInt

@Composable
fun PrivacyScreen(viewModel: PrivacyViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showOverlayRationale by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
        // The filter works without the notification permission, so continue either way.
        viewModel.startFilter()
    }

    fun requestToggle(turnOn: Boolean) {
        when {
            !turnOn -> viewModel.stopFilter()
            !state.permissions.overlayGranted -> showOverlayRationale = true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.permissions.notificationsGranted ->
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> viewModel.startFilter()
        }
    }

    if (showOverlayRationale) {
        AlertDialog(
            onDismissRequest = { showOverlayRationale = false },
            title = { Text("Allow display over other apps") },
            text = {
                Text(
                    "Privacy Display needs the \"Display over other apps\" permission to draw a dark " +
                        "layer above your screen. It never reads, records or sends what is on your " +
                        "screen. Next, Android will open a settings page where you can allow it. " +
                        "Come back here afterwards and turn the filter on.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayRationale = false
                    openOverlaySettings(context) { viewModel.showMessage(it) }
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayRationale = false }) { Text("Cancel") }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Privacy Display",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "by SONI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = onOpenSettings, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }

            StatusPill(active = state.running)

            SectionCard {
                BigSwitchRow(
                    title = "Privacy Display",
                    subtitle = if (state.running) "Filter is on" else "Filter is off",
                    checked = state.running,
                    onCheckedChange = { requestToggle(it) },
                )
            }

            Button(
                onClick = { requestToggle(!state.running) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.large,
                colors = if (state.running) {
                    ButtonDefaults.buttonColors(containerColor = CharcoalLight, contentColor = MaterialTheme.colorScheme.onSurface)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(
                    if (state.running) "Stop filter" else "Start filter",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Filter intensity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${state.intensity}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                    )
                }
                Slider(
                    value = state.intensity.toFloat(),
                    onValueChange = { viewModel.setIntensity(it.roundToInt()) },
                    valueRange = PrivacySettings.MIN_INTENSITY.toFloat()..PrivacySettings.MAX_INTENSITY.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Accent,
                        activeTrackColor = Accent,
                        inactiveTrackColor = CharcoalLight,
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .semantics { contentDescription = "Filter intensity" },
                )
                Text(
                    "0% = no dimming, 90% = strongest. Changes apply instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PreviewCard(intensity = state.intensity, color = state.settings.color)

            SectionCard {
                Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                PermissionRow(
                    name = "Display over other apps",
                    granted = state.permissions.overlayGranted,
                    actionLabel = "Grant",
                    onAction = { showOverlayRationale = true },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        name = "Notifications (Stop button)",
                        granted = state.permissions.notificationsGranted,
                        actionLabel = "Allow",
                        onAction = {
                            openNotificationSettings(context) { viewModel.showMessage(it) }
                        },
                    )
                }
            }

            state.error?.let { MessageCard(it, isError = true, onDismiss = viewModel::dismissMessages) }
            state.warning?.let { MessageCard(it, isError = false, onDismiss = viewModel::dismissMessages) }

            SectionCard(containerColor = CharcoalLight) {
                Text("Good to know", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "This is a software dimming layer, not Samsung's hardware Privacy Display. It makes " +
                        "your screen darker and harder to read at a glance, but it does not block " +
                        "side viewing. Android may not let it cover some system screens, such as " +
                        "the permission dialogs and parts of the lock screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun openOverlaySettings(context: Context, onError: (String) -> Unit) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: ActivityNotFoundException) {
        onError("Could not open the overlay permission screen. Open Settings > Apps > Privacy Display > Display over other apps.")
    }
}

private fun openNotificationSettings(context: Context, onError: (String) -> Unit) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: ActivityNotFoundException) {
        onError("Could not open notification settings. Open Settings > Apps > Privacy Display > Notifications.")
    }
}
