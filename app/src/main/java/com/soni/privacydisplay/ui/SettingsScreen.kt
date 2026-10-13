package com.soni.privacydisplay.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soni.privacydisplay.FilterColor
import com.soni.privacydisplay.PrivacyViewModel

@Composable
fun SettingsScreen(viewModel: PrivacyViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWriteRationale by rememberSaveable { mutableStateOf(false) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    if (showWriteRationale) {
        AlertDialog(
            onDismissRequest = { showWriteRationale = false },
            title = { Text("Allow changing brightness") },
            text = {
                Text(
                    "Optional brightness dimming lowers your system screen brightness while the filter " +
                        "is on. Android requires the \"Modify system settings\" permission for this. " +
                        "Your original brightness is saved first and restored when you turn this off " +
                        "or stop the filter. After allowing it, come back and switch this option on.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWriteRationale = false
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (e: ActivityNotFoundException) {
                        viewModel.showMessage("Could not open the settings page for \"Modify system settings\".")
                    }
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showWriteRationale = false }) { Text("Cancel") } },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset settings?") },
            text = {
                Text(
                    "This stops the filter and sets intensity, color and all options back to their " +
                        "defaults. Your original screen brightness will be restored if it was changed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.resetSettings()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
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
                FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.size(16.dp))
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            state.error?.let { MessageCard(it, isError = true, onDismiss = viewModel::dismissMessages) }
            state.warning?.let { MessageCard(it, isError = false, onDismiss = viewModel::dismissMessages) }

            SectionCard {
                Text("Filter color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorChip("Black", state.settings.color == FilterColor.BLACK) { viewModel.setColor(FilterColor.BLACK) }
                    ColorChip("Dark blue tint", state.settings.color == FilterColor.BLUE_TINT) { viewModel.setColor(FilterColor.BLUE_TINT) }
                }
            }

            SectionCard {
                SettingSwitchRow(
                    title = "Restore filter when app opens",
                    description = "If the system stopped the filter, turn it back on when you open Privacy Display. " +
                        "The filter never starts by itself at boot.",
                    checked = state.settings.restoreOnLaunch,
                    onCheckedChange = viewModel::setRestoreOnLaunch,
                )
            }

            SectionCard {
                SettingSwitchRow(
                    title = "Also dim system brightness",
                    description = if (state.permissions.writeSettingsGranted) {
                        "Off by default. Lowers brightness while the filter is on and restores your original value afterwards."
                    } else {
                        "Off by default. Needs the \"Modify system settings\" permission, which you have not granted."
                    },
                    checked = state.settings.brightnessDimEnabled,
                    onCheckedChange = { wantOn ->
                        if (wantOn && !state.permissions.writeSettingsGranted) {
                            showWriteRationale = true
                        } else {
                            viewModel.setBrightnessDim(wantOn)
                        }
                    },
                )
            }

            SectionCard {
                Text("Reset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) { Text("Reset settings") }
            }

            SectionCard(containerColor = CharcoalLight) {
                Text("About & limitations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Privacy Display by SONI is not affiliated with Samsung. It draws a translucent dark " +
                        "layer above other apps. It cannot reproduce hardware viewing-angle protection, " +
                        "and Android may keep the layer from covering some system screens.\n\n" +
                        "Privacy: no internet access, no analytics, no ads. The app never captures, " +
                        "records or sends your screen. Only your settings are stored on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ColorChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.padding(vertical = 12.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Accent,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
        ),
    )
}
