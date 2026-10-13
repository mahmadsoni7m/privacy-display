package com.soni.privacydisplay

import android.content.Context
import android.provider.Settings

/**
 * Optional brightness dimming. The user's original brightness and mode are saved exactly once
 * (never overwritten while a backup exists) and restored when the feature is turned off.
 */
class BrightnessController(
    context: Context,
    private val repository: PrivacySettingsRepository,
) {
    private val appContext = context.applicationContext

    fun canWrite(): Boolean = Settings.System.canWrite(appContext)

    suspend fun applyDim(): Result<Unit> {
        if (!canWrite()) {
            return Result.failure(
                IllegalStateException("Brightness dimming needs the \"Modify system settings\" permission."),
            )
        }
        return try {
            val resolver = appContext.contentResolver
            if (repository.brightnessBackup() == null) {
                val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                val mode = Settings.System.getInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
                repository.saveBrightnessBackup(BrightnessBackup(brightness, mode))
            }
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, DIM_LEVEL)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("Android denied the brightness change.", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Brightness dimming failed: ${e.message}", e))
        }
    }

    /** Succeeds (doing nothing) when there is nothing to restore. */
    suspend fun restore(): Result<Unit> {
        return try {
            val backup = repository.brightnessBackup() ?: return Result.success(Unit)
            if (!canWrite()) {
                return Result.failure(
                    IllegalStateException(
                        "Your original brightness could not be restored because the " +
                            "\"Modify system settings\" permission was removed. Grant it again to restore it.",
                    ),
                )
            }
            val resolver = appContext.contentResolver
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, backup.brightness)
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, backup.mode)
            repository.clearBrightnessBackup()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("Android denied restoring the brightness.", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Restoring brightness failed: ${e.message}", e))
        }
    }

    companion object {
        /** Fixed low brightness value (system scale 0..255) used while dimming is on. */
        const val DIM_LEVEL = 20
    }
}
