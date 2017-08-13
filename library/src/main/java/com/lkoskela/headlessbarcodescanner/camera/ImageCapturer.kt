package com.lkoskela.headlessbarcodescanner.camera

import android.media.Image
import kotlinx.coroutines.experimental.CompletableDeferred

/**
 * Interface for a generic image source providing barcode images to decode.
 */
internal interface ImageCapturer {

    /**
     * Start the capturer.
     *
     * This can be an async function, so returns a [CompletableDeferred] which is completed when
     * the capturer has started, or is completed with an exception if an error occurs while starting
     * the capturer.
     */
    fun start(): CompletableDeferred<Unit>

    /**
     * Stops the capturer.
     */
    fun stop()

    /**
     * Callback which is called when the image is captured.
     */
    var onImageCapturedListener: ((Image) -> Unit)?
}