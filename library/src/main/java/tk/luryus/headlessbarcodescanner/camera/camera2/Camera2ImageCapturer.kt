package tk.luryus.headlessbarcodescanner.camera.camera2

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import tk.luryus.headlessbarcodescanner.camera.ImageCapturer
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Copyright Lauri Koskela 2017.
 */
class Camera2ImageCapturer(private val ctx: Context) : ImageCapturer {

    private var camBackgroundThread: HandlerThread? = null
    private var camBackgroundHandler: Handler? = null
    private val openCloseCamLock: Semaphore = Semaphore(1)

    private val camera: CameraHolder

    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest? = null

    private val captureSemaphore: Semaphore = Semaphore(0)

    private val camStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            openCloseCamLock.release()
            cameraDevice = device
            createCameraSession()
        }

        override fun onDisconnected(device: CameraDevice) {
            openCloseCamLock.release()
            device.close()
            cameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            openCloseCamLock.release()
            device.close()
            cameraDevice = null

            Log.e(TAG, "Error with camera device: $error")
        }
    }

    init {
        camera = findUsableCamera()
    }

    override var onImageCapturedListener: ((Image) -> Unit)? = null

    @Synchronized
    override fun start() {
        startBackgroundThread()
        imageReader = createImageReader()
        openCamera()
    }

    @Synchronized
    override fun stop() {
        closeCamera()
        stopBackgroundThread()
        captureSemaphore.drainPermits()
    }

    private fun findUsableCamera(): CameraHolder {
        val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val allCams = cameraManager.getCameras()
        return allCams.firstOrNull { isValidCamera(it) }
                ?: throw RuntimeException("No suitable camera found")
    }

    private fun createImageReader(): ImageReader {
        // the camera we're using is already guaranteed to have support for a proper image format
        // and size
        return SUPPORTED_IMAGE_FORMATS
                .firstOrNull { camera.supportedFormats.contains(it) }
                ?.let { ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, it, 2) }
                ?.also {
                    it.setOnImageAvailableListener(
                            this::handleImageAvailable, camBackgroundHandler)
                }

                ?: throw IllegalStateException("Camera does not support any of our supported formats")

    }

    private fun createCameraSession() {
        val reader = imageReader
        val camDevice = cameraDevice
        if (reader == null || camDevice == null) {
            throw IllegalStateException(
                    "Trying to create camera session while imageReader or cameraDevice is null")
        }

        try {
            val surface = reader.surface
            val requestBuilder = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    .also { it.addTarget(surface) }
            requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF)
            requestBuilder.set(CaptureRequest.STATISTICS_HOT_PIXEL_MAP_MODE, false)
            requestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                    CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF)
            requestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureRequest = requestBuilder.build()

            camDevice.createCaptureSession(listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            throw RuntimeException("Configuring camera session failed")
                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) {
                                // the camera has already been closed
                                return
                            }

                            // start fetching images
                            cameraCaptureSession = session
                            startCaptureTimer()
                        }
                    }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startCaptureTimer() {
        val handler = camBackgroundHandler
                ?: throw IllegalStateException("Cannot start timer, handler is null")

        val capCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureSequenceCompleted(session: CameraCaptureSession?, sequenceId: Int, frameNumber: Long) {
                // we do not care about the result here, just allow taking another picture
                if (captureSemaphore.availablePermits() < 1) {
                    captureSemaphore.release()
                }
            }
        }

        val timerRunnable = object : Runnable {
            override fun run() {
                // schedule next run
                camBackgroundHandler?.postDelayed(this, CAPTURE_INTERVAL)

                // check if can already start next capture
                if (!captureSemaphore.tryAcquire()) {
                    return
                }

                // check that we actually have all the info needed
                val req = captureRequest ?: return
                val session = cameraCaptureSession ?: return

                try {
                    session.capture(req, capCallback, null)
                } catch(e: IllegalStateException) {
                    Log.w(TAG, "Could not start capture", e)
                }
            }
        }

        captureSemaphore.release()
        handler.post(timerRunnable)
    }

    private fun handleImageAvailable(imageReader: ImageReader) {
        imageReader.acquireLatestImage().use { img ->
            onImageCapturedListener?.invoke(img)
        }
    }

    private fun openCamera() {
        try {
            if (!openCloseCamLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Could not acquire lock for opening camera")
            }

            if (camBackgroundHandler == null
                    || camBackgroundThread?.isAlive != true) {
                throw RuntimeException("Background thread not initialized")
            }

            camBackgroundHandler?.let {
                camera.open(camStateCallback, it)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Could not access camera", e)
        }
    }

    private fun closeCamera() {
        try {
            openCloseCamLock.acquire()
            cameraCaptureSession?.let {
                it.close()
                cameraCaptureSession = null
            }
            cameraDevice?.let {
                it.close()
                cameraDevice = null
            }

            imageReader?.let {
                it.close()
                imageReader = null
            }
        } catch (ex: InterruptedException) {
            throw RuntimeException("Interrupted while trying to get lock for closing camera", ex)
        } finally {
            openCloseCamLock.release()
        }
    }

    private fun startBackgroundThread() {
        val thread = HandlerThread("CameraBackground")
        thread.start()
        camBackgroundThread = thread
        camBackgroundHandler = Handler(thread.looper)
    }

    private fun stopBackgroundThread() {
        camBackgroundThread?.quitSafely()
        try {
            camBackgroundThread?.join()
            camBackgroundThread = null
            camBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val TAG = "Cam2ImageReader"

        private val IMAGE_WIDTH = 320
        private val IMAGE_HEIGHT = 240
        private val CAM_OUTPUT_SIZE = Size(IMAGE_WIDTH, IMAGE_HEIGHT)
        private val SUPPORTED_IMAGE_FORMATS = arrayOf(ImageFormat.YUV_420_888, ImageFormat.JPEG)
        private val CAPTURE_INTERVAL = 250L

        private fun isValidCamera(cam: CameraHolder): Boolean {
            if (cam.isRearFacing()) {
                val formats = cam.supportedFormats

                for (format in SUPPORTED_IMAGE_FORMATS) {
                    if (formats.contains(format) && camSupportsImageFormat(cam, format)) {
                        return true
                    } else {
                        Log.i(TAG, "Rear-facing camera $cam does not support image format $format"
                                + " with size $CAM_OUTPUT_SIZE")
                    }
                }
            }

            return false
        }

        private fun camSupportsImageFormat(cam: CameraHolder, imageFormat: Int): Boolean {
            return cam.getSupportedSizesForFormat(imageFormat)?.contains(CAM_OUTPUT_SIZE) ?: false
        }
    }
}
