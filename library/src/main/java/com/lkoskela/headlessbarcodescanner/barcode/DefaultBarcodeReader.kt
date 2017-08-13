package com.lkoskela.headlessbarcodescanner.barcode

import android.media.Image
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.lkoskela.headlessbarcodescanner.barcode.luminancesource.LuminanceSourceFactory

/**
 * Copyright Lauri Koskela 2017.
 */
internal class DefaultBarcodeReader(
        private val lumSourceFactory: LuminanceSourceFactory,
        decodeHints: Map<DecodeHintType, *> = mapOf<DecodeHintType, Unit>()) : BarcodeReader {

    private val reader: MultiFormatReader = MultiFormatReader()

    init {
        setHints(decodeHints)
    }

    override fun setHints(hints: Map<DecodeHintType, *>) {
        reader.setHints(hints)
    }

    override fun read(image: Image): BarcodeResult? {
        lumSourceFactory.createLuminanceSource(image).use { source ->
            val bmap = BinaryBitmap(HybridBinarizer(source.luminanceSource))

            try {
                return BarcodeResult(reader.decodeWithState(bmap))
            } catch(e: NotFoundException) {
                return null
            }
        }
    }
}