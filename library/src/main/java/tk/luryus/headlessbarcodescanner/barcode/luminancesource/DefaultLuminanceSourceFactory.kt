package tk.luryus.headlessbarcodescanner.barcode.luminancesource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource

class DefaultLuminanceSourceFactory : LuminanceSourceFactory {

    override val supportedImageFormats: IntArray = intArrayOf(
            ImageFormat.JPEG, ImageFormat.YUV_420_888)

    private val byteArrayPool = ArrayPool { size -> ByteArray(size) }
    private val intArrayPool = ArrayPool { size -> IntArray(size) }

    private val jpegBufferLock = Any()
    private var jpegBuffer: ByteArray = ByteArray(0)

    private val byteBufferReleaseCallback: (ByteArray) -> Unit =
            { ba -> byteArrayPool.put(ba.size, ba) }
    private val intBufferReleaseCallback: (IntArray) -> Unit =
            { ia -> intArrayPool.put(ia.size, ia) }

    override fun createLuminanceSource(image: Image): PooledLuminanceSource<*> =
        when (image.format) {
            ImageFormat.JPEG -> createJpegLuminanceSource(image)
            ImageFormat.YUV_420_888 -> createYUVLuminanceSource(image)
            else -> throw UnsupportedOperationException(
                    "Image format ${image.format} not supported")
        }

    private fun createYUVLuminanceSource(image: Image): PooledLuminanceSource<ByteArray> {
        // the incoming images should be always same size, so we can reuse the byte arrays (buffers)
        val lumBuffer = image.planes.first().buffer
        val lumArray = byteArrayPool.get(lumBuffer.rewind().remaining())
        lumBuffer.get(lumArray)

        val source = PlanarYUVLuminanceSource(lumArray, image.width, image.height,
                0, 0, image.width, image.height,  // do not crop
                false)

        return PooledLuminanceSource(source, lumArray, byteBufferReleaseCallback)
    }

    private fun createJpegLuminanceSource(image: Image): PooledLuminanceSource<IntArray> {
        val imgBuffer = image.planes.first().buffer
        var bitmap: Bitmap? = null

        synchronized(jpegBufferLock) {
            val wantedBufferSize = imgBuffer.rewind().remaining()
            if (jpegBuffer.size < wantedBufferSize) {
                jpegBuffer = ByteArray(wantedBufferSize)
            }

            imgBuffer.get(jpegBuffer, 0, wantedBufferSize)

            bitmap = BitmapFactory.decodeByteArray(jpegBuffer, 0, wantedBufferSize)
        }

        bitmap?.let {
            // the images will be the same size so we can reuse the pixel buffers
            val pixelCount = it.width * it.height
            val pixelBuffer = intArrayPool.get(pixelCount)
            it.getPixels(pixelBuffer, 0, it.width, 0, 0, it.width, it.height)

            val source = RGBLuminanceSource(it.width, it.height, pixelBuffer)

            return PooledLuminanceSource(source, pixelBuffer, intBufferReleaseCallback)
        }

        throw IllegalStateException("Jpeg could not be decoded to bitmap")
    }
}