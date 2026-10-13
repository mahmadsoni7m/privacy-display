package com.soni.privacydisplay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OverlayStatus(
    val running: Boolean = false,
    /** A problem that stopped the filter (shown to the user). */
    val error: String? = null,
    /** A non-fatal problem while the filter keeps running (e.g. brightness dimming unavailable). */
    val warning: String? = null,
)

/** Process-wide status published by [PrivacyOverlayService] and observed by the UI. */
object OverlayState {
    private val _status = MutableStateFlow(OverlayStatus())
    val status: StateFlow<OverlayStatus> = _status.asStateFlow()

    fun update(block: (OverlayStatus) -> OverlayStatus) {
        _status.update(block)
    }

    fun clearMessages() {
        _status.update { it.copy(error = null, warning = null) }
    }
}
