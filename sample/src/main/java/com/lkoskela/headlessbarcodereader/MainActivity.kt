package com.lkoskela.headlessbarcodereader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.lkoskela.headlessbarcodescanner.HeadlessBarcodeScanner
import com.lkoskela.headlessbarcodescanner.OnBarcodeReadListener

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: HeadlessBarcodeScanner
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById<TextView>(R.id.text_view)
        textView.setOnClickListener { textView.text = "" }

        scanner = HeadlessBarcodeScanner.Builder(this)
                .pictureSize(HeadlessBarcodeScanner.CameraPictureSize.SMALL)
                .decodeHints(mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.AZTEC)))
                .build()
        scanner.barcodeListener = OnBarcodeReadListener {
            runOnUiThread {
                textView.text = it.text
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            scanner.start({}, { err -> Log.w(TAG, "Starting camera failed", err) })
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
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"
    }
}
