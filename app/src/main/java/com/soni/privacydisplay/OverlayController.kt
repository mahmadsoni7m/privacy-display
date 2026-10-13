package com.soni.privacydisplay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * Owns the single overlay window. The window is a plain full-screen [View] that is
 * not focusable and not touchable, so touches pass through to the app underneath.
 */
class OverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    val isShowing: Boolean get() = overlayView != null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(appContext)

    /** Adds the overlay, or updates the existing one (never creates a duplicate). */
    fun show(intensity: Int, color: FilterColor): Result<Unit> {
        if (!canDrawOverlays()) {
            return Result.failure(IllegalStateException("Overlay permission is not granted."))
        }
        return try {
            val existing = overlayView
            if (existing != null) {
                existing.setBackgroundColor(overlayColor(intensity, color))
            } else {
                val view = View(appContext).apply {
                    setBackgroundColor(overlayColor(intensity, color))
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                windowManager.addView(view, buildLayoutParams())
                overlayView = view
            }
            Result.success(Unit)
        } catch (e: WindowManager.BadTokenException) {
            Result.failure(IllegalStateException("Android refused to create the overlay window.", e))
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("Android denied permission to draw the overlay.", e))
        } catch (e: RuntimeException) {
            Result.failure(IllegalStateException("The overlay could not be shown: ${e.message}", e))
        }
    }

    /** Removes the overlay. The reference is always cleared, even if removal throws. */
    fun remove(): Result<Unit> {
        val view = overlayView ?: return Result.success(Unit)
        return try {
            windowManager.removeViewImmediate(view)
            Result.success(Unit)
        } catch (e: RuntimeException) {
            // IllegalArgumentException = view was already detached. Either way it is gone from our side.
            Result.failure(IllegalStateException("The overlay could not be removed cleanly: ${e.message}", e))
        } finally {
            overlayView = null
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.title = "PrivacyDisplayOverlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return params
    }

    companion object {
        /** ARGB color for a given intensity (0..90 %). Also used by the in-app preview. */
        fun overlayColor(intensity: Int, color: FilterColor): Int {
            val safe = intensity.coerceIn(PrivacySettings.MIN_INTENSITY, PrivacySettings.MAX_INTENSITY)
            val alpha = (safe / 100f * 255f).roundToInt()
            return when (color) {
                FilterColor.BLACK -> Color.argb(alpha, 0, 0, 0)
                FilterColor.BLUE_TINT -> Color.argb(alpha, 0, 10, 40)
            }
        }
    }
}
