package tk.luryus.headlessbarcodescanner.camera.camera2

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * Copyright Lauri Koskela 2017.
 */

fun CameraManager.getCameras() =
    this.cameraIdList.map { CameraHolder(this, it) }


fun CameraHolder.isRearFacing() =
    this.characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK

