package tk.luryus.headlessbarcodereader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import tk.luryus.headlessbarcodescanner.HeadlessBarcodeScanner
import tk.luryus.headlessbarcodescanner.OnBarcodeReadListener
import tk.luryus.headlessbarcodescanner.camera.ImageCapturer

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: HeadlessBarcodeScanner
    private lateinit var imageCapturer: ImageCapturer
    private lateinit var textView: TextView
    private lateinit var imgView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById<TextView>(R.id.text_view)
        imgView = findViewById<ImageView>(R.id.image)

        scanner = HeadlessBarcodeScanner.create(this)
        scanner.setBarcodeDecodingHints(mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.AZTEC)
        ))
        scanner.barcodeListener = OnBarcodeReadListener {
            runOnUiThread {
                textView.text = it.text
            }
        }

//        imageCapturer = Camera2ImageCapturer(this)
//        imageCapturer.onImageCapturedListener = { image ->
//            if (image.format == ImageFormat.JPEG) {
//                val jpegByte = image.planes.first().buffer
//                val arr = ByteArray(jpegByte.remaining())
//                jpegByte.get(arr)
//
//                val bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.size)
//
//                runOnUiThread { imgView.setImageBitmap(bitmap) }
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            scanner.start({}, { err -> Log.w(TAG, "Starting camera failed", err) })
//            imageCapturer.start()
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CODE
                && permissions[0] == Manifest.permission.CAMERA
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanner.start({}, { err -> Log.w(TAG, "Starting camera failed", err) })
        }
    }

    override fun onPause() {
        super.onPause()
        scanner.stop()
//        imageCapturer.stop()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"
    }
}
