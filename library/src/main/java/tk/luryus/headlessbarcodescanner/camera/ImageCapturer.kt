package tk.luryus.headlessbarcodescanner.camera

import android.media.Image
import kotlinx.coroutines.experimental.CompletableDeferred

/**
 * Copyright Lauri Koskela 2017.
 */
interface ImageCapturer {

    fun start(): CompletableDeferred<Unit>
    fun stop()

    var onImageCapturedListener: ((Image) -> Unit)?
}