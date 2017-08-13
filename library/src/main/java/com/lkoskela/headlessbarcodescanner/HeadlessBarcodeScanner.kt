package com.lkoskela.headlessbarcodescanner

import android.content.Context
import android.media.Image
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import com.lkoskela.headlessbarcodescanner.barcode.BarcodeReader
import com.lkoskela.headlessbarcodescanner.barcode.BarcodeResult
import com.lkoskela.headlessbarcodescanner.barcode.DefaultBarcodeReader
import com.lkoskela.headlessbarcodescanner.barcode.luminancesource.DefaultLuminanceSourceFactory
import com.lkoskela.headlessbarcodescanner.camera.ImageCapturer
import com.lkoskela.headlessbarcodescanner.camera.camera2.Camera2ImageCapturer

/**
 * The main entrypoint class for using the HeadlessBarcodeScanner library.
 *
 * Example usage (in an activity):
 * ```
 * HeadlessBarcodeScanner scanner =
 *      new HeadlessBarcodeScanner.Builder(this).build();
 * scanner.setBarcodeListener = barcode -> Log.i(TAG, "Barcode read: " + barcode.text);
 * scanner.start(
 *      () -> Log.d(TAG, "Barcode reading started"),
 *      err -> Log.e(TAG, "BarcodeScanner init failed", err));
 * ```
 *
 * Starting the scanner includes initializing the camera, and thus it can take a while. Because
 * of this, the start operation is asynchronous and its completion is signaled via the callback
 * parameters passed to [start].
 *
 * By default, the scanner only looks for QR codes in the images. This and other scanning
 * preferences can be set by providing decoding hints to ZXing with [Builder.decodeHints] method.
 * More info at https://zxing.github.io/zxing/apidocs/com/google/zxing/DecodeHintType.html
 *
 * The size of the captured images (for scanning) can be adjusted. Three image sizes are available:
 * * 320 x 240
 * * 640 x 480
 * * 1280 x 720
 * By default, 320 x 240 is used. This should work on pretty much all devices, be fast and
 * not consume much RAM. This might not work well with high-resolution barcodes though, so if needed
 * the quality can be increased with [Builder.pictureSize] method.
 */
class HeadlessBarcodeScanner private constructor(
        private val imageCapturer: ImageCapturer, private val barcodeReader: BarcodeReader) {

    /**
     * The previous read barcode result. Stored to avoid duplicate notifications for read barcodes.
     */
    private var previousResult: BarcodeResult? = null
    /**
     * A callback which is called when a new barcode is read.
     */
    var barcodeListener: OnBarcodeReadListener? = null

    init {
        imageCapturer.onImageCapturedListener = this::handleImageCaptured
    }

    /**
     * Starts the scanner.
     * @param onComplete Callback called when the scanner has been started
     * @param onError Callback called when an error occured while starting the scanner
     */
    fun start(onComplete: () -> Unit, onError: (Exception) -> Unit) {
        previousResult = null

        launch(CommonPool) {
            try {
                imageCapturer.start().await()
                onComplete.invoke()
            } catch(e: Exception) {
                onError.invoke(e)
            }
        }
    }

    /**
     * Stops the scanner
     */
    fun stop() {
        imageCapturer.stop()
    }

    /**
     * Handles a new image coming in from [imageCapturer]. Basically just throws the image to
     * [barcodeReader] to handle.
     */
    private fun handleImageCaptured(image: Image) {
        val barcodeResult = barcodeReader.read(image)
        if (barcodeResult != previousResult) {
            previousResult = barcodeResult

            if (barcodeResult != null) {
                barcodeListener?.onBarcodeRead(barcodeResult)
            }
        }
    }

    /**
     * Options for captured picture sizes for [ImageCapturer]
     */
    enum class CameraPictureSize {
        SMALL, MEDIUM, LARGE
    }

    /**
     * A builder class for creating a [HeadlessBarcodeScanner].
     * @param context The context to use for camera access.
     */
    class Builder(private val context: Context) {
        /** The selected picture size */
        private var picSize: CameraPictureSize = CameraPictureSize.SMALL
        /** The selected decode hints */
        private var decodeHints: Map<DecodeHintType, *> =
                mapOf(DecodeHintType.POSSIBLE_FORMATS to BarcodeFormat.QR_CODE)

        /** Set a picture size for the captured images */
        fun pictureSize(size: CameraPictureSize) = apply { picSize = size }

        /**
         * Set decodeHints for ZXing barcode reader.
         * See https://zxing.github.io/zxing/apidocs/com/google/zxing/DecodeHintType.html for
         * available options.
         */
        fun decodeHints(hints: Map<DecodeHintType, *>) = apply { decodeHints = hints }

        /** Builds a new HeadlessBarcodeScanner instance */
        fun build(): HeadlessBarcodeScanner {
            val camOutputSize = when (picSize) {
                HeadlessBarcodeScanner.CameraPictureSize.SMALL ->
                    Camera2ImageCapturer.CAM_OUTPUT_SIZE_SMALL
                HeadlessBarcodeScanner.CameraPictureSize.MEDIUM ->
                    Camera2ImageCapturer.CAM_OUTPUT_SIZE_MEDIUM
                HeadlessBarcodeScanner.CameraPictureSize.LARGE ->
                    Camera2ImageCapturer.CAM_OUTPUT_SIZE_LARGE
            }
            val capturer = Camera2ImageCapturer(context, camOutputSize)
            val reader = DefaultBarcodeReader(DefaultLuminanceSourceFactory(), decodeHints)

            return HeadlessBarcodeScanner(capturer, reader)
        }
    }
}