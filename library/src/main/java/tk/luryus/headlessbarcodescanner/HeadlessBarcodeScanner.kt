package tk.luryus.headlessbarcodescanner

import android.content.Context
import android.media.Image
import com.google.zxing.DecodeHintType
import tk.luryus.headlessbarcodescanner.barcode.BarcodeReader
import tk.luryus.headlessbarcodescanner.barcode.BarcodeResult
import tk.luryus.headlessbarcodescanner.barcode.DefaultBarcodeReader
import tk.luryus.headlessbarcodescanner.barcode.luminancesource.DefaultLuminanceSourceFactory
import tk.luryus.headlessbarcodescanner.camera.ImageCapturer
import tk.luryus.headlessbarcodescanner.camera.camera2.Camera2ImageCapturer

/**
 * Copyright Lauri Koskela 2017.
 */

class HeadlessBarcodeScanner private constructor(
        private val imageCapturer: ImageCapturer, private val barcodeReader: BarcodeReader) {

    private var previousResult: BarcodeResult? = null

    var barcodeListener: OnBarcodeReadListener? = null

    init {
        imageCapturer.onImageCapturedListener = this::handleImageCaptured
    }

    fun start() {
        previousResult = null
        imageCapturer.start()
    }

    fun stop() {
        imageCapturer.stop()
    }

    fun setBarcodeDecodingHints(hints: Map<DecodeHintType, *>) = barcodeReader.setHints(hints)

    private fun handleImageCaptured(image: Image) {
        val barcodeResult = barcodeReader.read(image)
        if (barcodeResult != null && barcodeResult != previousResult) {
            previousResult = barcodeResult
            barcodeListener?.onBarcodeRead(barcodeResult)
        }
    }

    companion object {
        fun create(ctx: Context): HeadlessBarcodeScanner {
            val capturer = Camera2ImageCapturer(ctx)
            val reader = DefaultBarcodeReader(DefaultLuminanceSourceFactory())

            return HeadlessBarcodeScanner(capturer, reader)
        }
    }
}