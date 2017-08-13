package com.lkoskela.headlessbarcodescanner.camera.camera2

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * Copyright Lauri Koskela 2017.
 */

internal fun CameraManager.getCameras() =
    this.cameraIdList.map { CameraHolder(this, it) }


internal fun CameraHolder.isRearFacing() =
    this.characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK

