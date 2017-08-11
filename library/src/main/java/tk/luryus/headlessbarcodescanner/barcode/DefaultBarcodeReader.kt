package tk.luryus.headlessbarcodescanner.barcode

import android.media.Image
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import tk.luryus.headlessbarcodescanner.barcode.luminancesource.LuminanceSourceFactory

/**
 * Copyright Lauri Koskela 2017.
 */
class DefaultBarcodeReader(private val lumSourceFactory: LuminanceSourceFactory) : BarcodeReader {

    private val reader: MultiFormatReader = MultiFormatReader()

    init {
        setHints(mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
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