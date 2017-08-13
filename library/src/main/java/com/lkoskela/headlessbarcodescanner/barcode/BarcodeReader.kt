package com.lkoskela.headlessbarcodescanner.barcode

import android.media.Image
import com.google.zxing.DecodeHintType

/**
 * Copyright Lauri Koskela 2017.
 */
internal interface BarcodeReader {

    fun setHints(hints: Map<DecodeHintType, *>): Unit
    fun read(image: Image): BarcodeResult?
}