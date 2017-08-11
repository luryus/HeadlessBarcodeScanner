package tk.luryus.headlessbarcodescanner.barcode.luminancesource

import android.media.Image

/**
 * Copyright Lauri Koskela 2017.
 */
interface LuminanceSourceFactory {

    val supportedImageFormats: IntArray

    fun createLuminanceSource(image: Image): PooledLuminanceSource<*>
}