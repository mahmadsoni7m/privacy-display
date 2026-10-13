package com.soni.privacydisplay

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class FilterColor {
    BLACK,
    BLUE_TINT;

    companion object {
        /** Unknown or corrupted saved values fall back to BLACK. */
        fun fromName(name: String?): FilterColor = entries.firstOrNull { it.name == name } ?: BLACK
    }
}

data class PrivacySettings(
    val intensity: Int = DEFAULT_INTENSITY,
    val color: FilterColor = FilterColor.BLACK,
    val restoreOnLaunch: Boolean = false,
    val filterActive: Boolean = false,
    val brightnessDimEnabled: Boolean = false,
) {
    companion object {
        const val MIN_INTENSITY = 0
        const val MAX_INTENSITY = 90
        const val DEFAULT_INTENSITY = 50
    }
}

/** The user's brightness values captured before the optional dimming feature changed them. */
data class BrightnessBackup(val brightness: Int, val mode: Int)

private val Context.privacyDataStore by preferencesDataStore(
    name = "privacy_display_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class PrivacySettingsRepository(context: Context) {

    private val store = context.applicationContext.privacyDataStore

    private object Keys {
        val INTENSITY = intPreferencesKey("intensity")
        val COLOR = stringPreferencesKey("filter_color")
        val RESTORE_ON_LAUNCH = booleanPreferencesKey("restore_on_launch")
        val FILTER_ACTIVE = booleanPreferencesKey("filter_active")
        val BRIGHTNESS_DIM = booleanPreferencesKey("brightness_dim_enabled")
        val ORIGINAL_BRIGHTNESS = intPreferencesKey("original_brightness")
        val ORIGINAL_BRIGHTNESS_MODE = intPreferencesKey("original_brightness_mode")
    }

    val settings: Flow<PrivacySettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            PrivacySettings(
                intensity = (p[Keys.INTENSITY] ?: PrivacySettings.DEFAULT_INTENSITY)
                    .coerceIn(PrivacySettings.MIN_INTENSITY, PrivacySettings.MAX_INTENSITY),
                color = FilterColor.fromName(p[Keys.COLOR]),
                restoreOnLaunch = p[Keys.RESTORE_ON_LAUNCH] ?: false,
                filterActive = p[Keys.FILTER_ACTIVE] ?: false,
                brightnessDimEnabled = p[Keys.BRIGHTNESS_DIM] ?: false,
            )
        }

    suspend fun setIntensity(value: Int) {
        store.edit {
            it[Keys.INTENSITY] = value.coerceIn(PrivacySettings.MIN_INTENSITY, PrivacySettings.MAX_INTENSITY)
        }
    }

    suspend fun setColor(color: FilterColor) {
        store.edit { it[Keys.COLOR] = color.name }
    }

    suspend fun setRestoreOnLaunch(enabled: Boolean) {
        store.edit { it[Keys.RESTORE_ON_LAUNCH] = enabled }
    }

    suspend fun setFilterActive(active: Boolean) {
        store.edit { it[Keys.FILTER_ACTIVE] = active }
    }

    suspend fun setBrightnessDimEnabled(enabled: Boolean) {
        store.edit { it[Keys.BRIGHTNESS_DIM] = enabled }
    }

    suspend fun brightnessBackup(): BrightnessBackup? {
        val p = readPreferences()
        val brightness = p[Keys.ORIGINAL_BRIGHTNESS]
        val mode = p[Keys.ORIGINAL_BRIGHTNESS_MODE]
        return if (brightness != null && mode != null) BrightnessBackup(brightness, mode) else null
    }

    suspend fun saveBrightnessBackup(backup: BrightnessBackup) {
        store.edit {
            it[Keys.ORIGINAL_BRIGHTNESS] = backup.brightness
            it[Keys.ORIGINAL_BRIGHTNESS_MODE] = backup.mode
        }
    }

    suspend fun clearBrightnessBackup() {
        store.edit {
            it.remove(Keys.ORIGINAL_BRIGHTNESS)
            it.remove(Keys.ORIGINAL_BRIGHTNESS_MODE)
        }
    }

    /**
     * Resets user-facing preferences. The brightness backup is intentionally kept so the
     * user's original brightness can still be restored afterwards.
     */
    suspend fun resetSettings() {
        store.edit {
            it.remove(Keys.INTENSITY)
            it.remove(Keys.COLOR)
            it.remove(Keys.RESTORE_ON_LAUNCH)
            it.remove(Keys.FILTER_ACTIVE)
            it.remove(Keys.BRIGHTNESS_DIM)
        }
    }

    private suspend fun readPreferences(): Preferences =
        store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.first()
}
