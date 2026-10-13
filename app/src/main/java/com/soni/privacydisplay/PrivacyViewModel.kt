package com.soni.privacydisplay

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionState(
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val writeSettingsGranted: Boolean = false,
)

data class UiState(
    val loaded: Boolean = false,
    val settings: PrivacySettings = PrivacySettings(),
    /** The value shown on the slider/preview; updates instantly while dragging. */
    val intensity: Int = PrivacySettings.DEFAULT_INTENSITY,
    val permissions: PermissionState = PermissionState(),
    val running: Boolean = false,
    val error: String? = null,
    val warning: String? = null,
)

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val repository = PrivacySettingsRepository(app)
    private val brightness = BrightnessController(app, repository)

    private val liveIntensity = MutableStateFlow<Int?>(null)
    private val permissions = MutableStateFlow(readPermissions())
    private val message = MutableStateFlow<String?>(null)

    private var persistJob: Job? = null
    private var reconciled = false

    val uiState: StateFlow<UiState> = combine(
        repository.settings,
        OverlayState.status,
        permissions,
        liveIntensity,
        message,
    ) { settings, status, perms, live, msg ->
        UiState(
            loaded = true,
            settings = settings,
            intensity = (live ?: settings.intensity)
                .coerceIn(PrivacySettings.MIN_INTENSITY, PrivacySettings.MAX_INTENSITY),
            permissions = perms,
            running = status.running,
            error = msg ?: status.error,
            warning = status.warning,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun readPermissions() = PermissionState(
        overlayGranted = Settings.canDrawOverlays(app),
        notificationsGranted = NotificationManagerCompat.from(app).areNotificationsEnabled(),
        writeSettingsGranted = Settings.System.canWrite(app),
    )

    /** Called from the activity's onResume (also after returning from a system settings screen). */
    fun onResume() {
        val perms = readPermissions()
        permissions.value = perms
        if (OverlayState.status.value.running && !perms.overlayGranted) {
            message.value = "Overlay permission was revoked, so the privacy filter was stopped."
            stopFilter()
        }
        if (!reconciled) {
            reconciled = true
            viewModelScope.launch { reconcileAfterRestart() }
        }
    }

    /**
     * Runs once per app process. If the filter was left "active" but its service is gone
     * (system stopped it), either restore it (if the user opted in and permission is still
     * granted) or clear the stale state and put the user's brightness back.
     */
    private suspend fun reconcileAfterRestart() {
        val saved = repository.settings.first()
        val running = OverlayState.status.value.running
        if (running) return
        if (saved.filterActive && saved.restoreOnLaunch && permissions.value.overlayGranted) {
            startFilter()
            return
        }
        if (saved.filterActive) {
            safely { repository.setFilterActive(false) }
        }
        val restored = brightness.restore()
        if (restored.isFailure) message.value = restored.exceptionOrNull()?.message
    }

    fun startFilter() {
        permissions.value = readPermissions()
        if (!permissions.value.overlayGranted) {
            message.value = "Overlay permission is required. Tap \"Grant\" to allow it."
            return
        }
        message.value = null
        OverlayState.clearMessages()
        try {
            ContextCompat.startForegroundService(
                app,
                Intent(app, PrivacyOverlayService::class.java).setAction(PrivacyOverlayService.ACTION_START),
            )
        } catch (e: Exception) {
            message.value = "Could not start the privacy filter: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }

    fun stopFilter() {
        if (OverlayState.status.value.running) {
            try {
                app.startService(
                    Intent(app, PrivacyOverlayService::class.java).setAction(PrivacyOverlayService.ACTION_STOP),
                )
            } catch (e: Exception) {
                message.value = "Could not stop the privacy filter: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        } else {
            viewModelScope.launch { safely { repository.setFilterActive(false) } }
        }
    }

    fun setIntensity(value: Int) {
        val safe = value.coerceIn(PrivacySettings.MIN_INTENSITY, PrivacySettings.MAX_INTENSITY)
        liveIntensity.value = safe
        persistJob?.cancel()
        persistJob = viewModelScope.launch { safely { repository.setIntensity(safe) } }
    }

    fun setColor(color: FilterColor) {
        viewModelScope.launch { safely { repository.setColor(color) } }
    }

    fun setRestoreOnLaunch(enabled: Boolean) {
        viewModelScope.launch { safely { repository.setRestoreOnLaunch(enabled) } }
    }

    fun setBrightnessDim(enabled: Boolean) {
        viewModelScope.launch {
            permissions.value = readPermissions()
            if (enabled && !permissions.value.writeSettingsGranted) {
                message.value = "Allow \"Modify system settings\" first, then turn brightness dimming on."
                return@launch
            }
            safely { repository.setBrightnessDimEnabled(enabled) }
            // While the filter runs, the service restores brightness itself.
            if (!enabled && !OverlayState.status.value.running) {
                val restored = brightness.restore()
                if (restored.isFailure) message.value = restored.exceptionOrNull()?.message
            }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            if (OverlayState.status.value.running) stopFilter()
            val restored = brightness.restore()
            if (restored.isFailure) message.value = restored.exceptionOrNull()?.message
            persistJob?.cancel()
            safely { repository.resetSettings() }
            liveIntensity.value = null
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        permissions.value = readPermissions()
        if (!granted) {
            message.value = "Notification permission was denied. The filter still works, " +
                "but you may not see the notification with the Stop button."
        }
    }

    fun showMessage(text: String) {
        message.value = text
    }

    fun dismissMessages() {
        message.value = null
        OverlayState.clearMessages()
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message.value = "Could not save your settings: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }
}
