package tk.luryus.headlessbarcodescanner.camera.camera2

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Handler
import android.util.Size

/**
 * Copyright Lauri Koskela 2017.
 */
class CameraHolder(private val cameraManager: CameraManager, private val id: String) {

    val characteristics: CameraCharacteristics
            by lazy { cameraManager.getCameraCharacteristics(id) }

    val supportedFormats: IntArray
        get() = streamConfigMap?.outputFormats ?: IntArray(0)

    fun getSupportedSizesForFormat(format: Int): Array<Size>? =
        streamConfigMap?.getOutputSizes(format)

    override fun toString(): String = "Camera: $id"

    @SuppressLint("MissingPermission")
    fun open(callback: CameraDevice.StateCallback, handler: Handler) {
        cameraManager.openCamera(id, callback, handler)
    }

    private val streamConfigMap: StreamConfigurationMap?
            by lazy { characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) }
}