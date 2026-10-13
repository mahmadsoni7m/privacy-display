package com.soni.privacydisplay

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Foreground service that owns the overlay window while the privacy filter is on.
 * Settings changes (intensity, color, brightness option) are applied live.
 */
class PrivacyOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: PrivacySettingsRepository
    private lateinit var overlay: OverlayController
    private lateinit var brightness: BrightnessController

    private var observeJob: Job? = null
    private var dimApplied = false

    /** True when the user (or a fatal error) ended the filter, as opposed to the system killing us. */
    private var explicitStop = false
    private var failureMessage: String? = null

    override fun onCreate() {
        super.onCreate()
        repository = PrivacySettingsRepository(this)
        overlay = OverlayController(this)
        brightness = BrightnessController(this, repository)
        NotificationHelper.ensureChannel(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopFilter()
            else -> startFilter()
        }
        return START_NOT_STICKY
    }

    private fun startFilter() {
        // Must be called promptly after startForegroundService(), every time.
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                NotificationHelper.build(this, null),
                type,
            )
        } catch (e: Exception) {
            failAndStop("The privacy filter service could not start: ${e.message ?: e.javaClass.simpleName}")
            return
        }

        if (observeJob?.isActive == true) return // already running: never create a second overlay

        if (!overlay.canDrawOverlays()) {
            failAndStop("Overlay permission is missing. Grant \"Display over other apps\" and try again.")
            return
        }

        explicitStop = false
        failureMessage = null
        OverlayState.update { it.copy(running = true, error = null, warning = null) }

        observeJob = scope.launch {
            try {
                repository.setFilterActive(true)
            } catch (e: CancellationException) {
                throw e
            } catch (ignored: Exception) {
                // Not fatal: the filter still works, it just may not be restored after a restart.
            }
            repository.settings.collect { applySettings(it) }
        }
    }

    private suspend fun applySettings(s: PrivacySettings) {
        if (!overlay.canDrawOverlays()) {
            failAndStop("Overlay permission was revoked. Grant it again to use the privacy filter.")
            return
        }
        val shown = overlay.show(s.intensity, s.color)
        if (shown.isFailure) {
            failAndStop(shown.exceptionOrNull()?.message ?: "The overlay could not be shown.")
            return
        }
        OverlayState.update { it.copy(running = true, error = null) }

        getSystemService(NotificationManager::class.java)
            ?.notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.build(this, s.intensity))

        if (s.brightnessDimEnabled) {
            if (!dimApplied) {
                val result = brightness.applyDim()
                if (result.isSuccess) {
                    dimApplied = true
                    OverlayState.update { it.copy(warning = null) }
                } else {
                    val msg = result.exceptionOrNull()?.message
                    OverlayState.update { it.copy(warning = msg) }
                }
            }
        } else {
            val result = brightness.restore()
            if (result.isSuccess) {
                dimApplied = false
                OverlayState.update { it.copy(warning = null) }
            } else {
                val msg = result.exceptionOrNull()?.message
                OverlayState.update { it.copy(warning = msg) }
            }
        }
    }

    private fun stopFilter() {
        explicitStop = true
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun failAndStop(message: String) {
        failureMessage = message
        stopFilter()
    }

    override fun onDestroy() {
        observeJob?.cancel()
        observeJob = null

        val removal = overlay.remove()

        var restoreWarning: String? = null
        try {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
                    val restored = brightness.restore()
                    if (restored.isFailure) restoreWarning = restored.exceptionOrNull()?.message
                    if (explicitStop) repository.setFilterActive(false)
                }
            }
        } catch (ignored: Exception) {
            // Cleanup is best effort; the app also re-checks on next launch.
        }

        val warning = restoreWarning ?: removal.exceptionOrNull()?.message
        OverlayState.update { OverlayStatus(running = false, error = failureMessage, warning = warning) }

        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.soni.privacydisplay.action.START"
        const val ACTION_STOP = "com.soni.privacydisplay.action.STOP"
        private const val CLEANUP_TIMEOUT_MS = 2_000L
    }
}
