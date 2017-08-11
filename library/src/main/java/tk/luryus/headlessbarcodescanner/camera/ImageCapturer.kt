package tk.luryus.headlessbarcodescanner.camera

import android.media.Image

/**
 * Copyright Lauri Koskela 2017.
 */
interface ImageCapturer {

    fun start()
    fun stop()

    var onImageCapturedListener: ((Image) -> Unit)?
}