package tk.luryus.headlessbarcodescanner.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.ResultPoint
import java.util.*

/**
 * Copyright Lauri Koskela 2017.
 */

class BarcodeResult(private val result: Result) {

    val text: String?
        get() = result.text

    val barcodeFormat: BarcodeFormat
        get() = result.barcodeFormat

    val numBits: Int
        get() = result.numBits

    val rawBytes: ByteArray?
        get() = result.rawBytes

    val resultMetadata: Map<ResultMetadataType, *>?
        get() = result.resultMetadata

    val resultPoints: Array<ResultPoint>?
        get() = result.resultPoints

    val timestamp: Long
        get() = result.timestamp

    override fun toString() = result.toString()

    override fun hashCode(): Int {
        var result = 13

        result = 31 * result + this.barcodeFormat.hashCode()
        result = 31 * result + (this.text?.hashCode() ?: 0)
        result = 31 * result + (this.rawBytes?.contentHashCode() ?: 0)

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other is BarcodeResult) {
            return this.barcodeFormat == other.barcodeFormat
                && this.numBits == other.numBits
                && this.text == other.text
                && Arrays.equals(this.rawBytes, other.rawBytes)
        }

        return false
    }
}