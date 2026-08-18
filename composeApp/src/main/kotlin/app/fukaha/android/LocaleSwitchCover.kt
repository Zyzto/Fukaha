package app.fukaha.android

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.core.view.drawToBitmap
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Holds one full-window snapshot across the AppCompat locale recreation so the
 * next Activity can keep the previous UI on screen instead of a black window.
 */
internal object LocaleSwitchCover {
    @Volatile
    private var held: Bitmap? = null

    suspend fun capture(activity: Activity) {
        val window = activity.window ?: return
        val view = window.decorView
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val copied = suspendCancellableCoroutine { continuation ->
            val finished = PixelCopy.OnPixelCopyFinishedListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result == PixelCopy.SUCCESS)
                } else if (result == PixelCopy.SUCCESS) {
                    bitmap.recycle()
                }
            }
            runCatching {
                PixelCopy.request(window, bitmap, finished, Handler(Looper.getMainLooper()))
            }.onFailure {
                if (continuation.isActive) continuation.resume(false)
            }
        }
        if (copied) {
            replace(bitmap)
            return
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        runCatching { replace(view.drawToBitmap()) }
    }

    fun take(): Bitmap? {
        val bitmap = held
        held = null
        return bitmap
    }

    fun discard() {
        replace(null)
    }

    private fun replace(next: Bitmap?) {
        val previous = held
        held = next
        if (previous != null && previous !== next && !previous.isRecycled) {
            previous.recycle()
        }
    }
}
