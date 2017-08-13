package com.lkoskela.headlessbarcodescanner.barcode.luminancesource

import android.media.Image

/**
 * Copyright Lauri Koskela 2017.
 */
internal interface LuminanceSourceFactory {

    val supportedImageFormats: IntArray

    fun createLuminanceSource(image: Image): PooledLuminanceSource<*>
}