package com.seerslab.argear.sample.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max

class ReferenceCamera2(
    context: Context,
    listener: CameraListener,
    deviceRotation: Int
) :
    ReferenceCamera() {

    private var cameraID = 0
    private var cameraTextureId: Int? = 0
    private var cameraTexture: SurfaceTexture? = null
    private val switchingCamera = AtomicBoolean(false)
    private val isStarted: AtomicBoolean
    private val frontCameraIds: MutableList<String> = ArrayList()
    private val rearCameraIds: MutableList<String> = ArrayList()
    private val cameraIdIndices = IntArray(2)
    private var supportedFlash = false
    private var faceDetectSupported = false
    private var faceDetectMode = 0

    private var displayedPreviewSize = IntArray(2)
    private var displayedVideoSize = IntArray(2)

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val deviceRotation: Int
    private val context: Context
    private val cameraOpenCloseLock = Semaphore(1)
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private var cameraSensorResolution: Size? = null
    private var horizontalViewAngle = -1.0f
    private var verticalViewAngle = -1.0f

    override val cameraFacingFront: Int
        get() = if (frontCameraIds.size == 0) {
            -1
        } else CAMERA_FACING_FRONT

    override val cameraFacingBack: Int
        get() = if (rearCameraIds.size == 0) {
            -1
        } else CAMERA_FACING_BACK

    override val previewSize: IntArray
        get() = displayedPreviewSize

    val videoSize: IntArray
        get() = displayedVideoSize

    var orientation = 0
        private set

    /**
     * Compares two `Size`s based on their areas.
     */
    object CompareSizesByArea : Comparator<Size> {
        override fun compare(
            lhs: Size,
            rhs: Size
        ): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    override fun setCameraTexture(textureId: Int?, surfaceTexture: SurfaceTexture?) {
        if (cameraDevice != null || imageReader == null) return
        if (cameraTexture == null && surfaceTexture != null) {
            cameraTextureId = textureId
            cameraTexture = surfaceTexture
            val cameraId = getCameraId(cameraID, cameraIdIndices[cameraID])
            openCamera(cameraId, cameraID)
        }
    }

    override fun setFacing(CameraFacing: Int) {
        cameraID = CameraFacing
    }

    override fun startCamera() {
        startCamera(cameraID, cameraIdIndices[cameraID])
    }

    override fun stopCamera() {
        Log.d(TAG, "stopCamera")
        synchronized(isStarted) {
            if (isStarted.compareAndSet(true, false)) {
                try {
                    closeCamera()
                } catch (e: NullPointerException) {
                    Log.e(TAG, "Error Stopping camera - NullPointerException: ", e)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Error Stopping camera - RuntimeException: ", e)
                }
            }
        }
    }

    override fun destroy() {
        stopHandlerThread()
    }

    override fun isCameraFacingFront(): Boolean {
        return cameraID == CAMERA_FACING_FRONT
    }

    override fun changeCameraFacing(): Boolean {
        if (frontCameraIds.size == 0 || rearCameraIds.size == 0) {
            return false
        }
        return if (switchingCamera.compareAndSet(false, true)) {
            if (cameraID == CAMERA_FACING_BACK) {
                cameraID = CAMERA_FACING_FRONT
            } else if (cameraID == CAMERA_FACING_FRONT) {
                cameraID = CAMERA_FACING_BACK
            }
            changeCamera(cameraID, cameraIdIndices[cameraID])
            true
        } else {
            false
        }
    }

    val isRunning: Boolean
        get() = isStarted.get()

    private fun startHandlerThread() {
        handlerThread = HandlerThread(TAG)
        handlerThread?.start()
        handler = Handler(handlerThread?.looper)
    }

    private fun stopHandlerThread() {
        if (handlerThread == null) {
            return
        }
        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun getCameraId(facing: Int, cameraIdIndex: Int): String? {
        var cameraId: String? = null
        if (facing == CAMERA_FACING_FRONT
            && frontCameraIds.size > cameraIdIndex
        ) {
            cameraId = frontCameraIds[cameraIdIndex]
        } else if (facing == CAMERA_FACING_BACK
            && rearCameraIds.size > cameraIdIndex
        ) {
            cameraId = rearCameraIds[cameraIdIndex]
        }
        return cameraId
    }

    private fun initializeCameraIds() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (cameraId in manager.cameraIdList) {
                val characteristics =
                    manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(
                    CameraCharacteristics.LENS_FACING
                )
                if (facing == CAMERA_FACING_FRONT) {
                    frontCameraIds.add(cameraId)
                } else if (facing == CAMERA_FACING_BACK) {
                    rearCameraIds.add(cameraId)
                }
            }
            cameraIdIndices[0] = 0
            cameraIdIndices[1] = 0
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Getting camera IDs failed.", e)
            frontCameraIds.clear()
            rearCameraIds.clear()
            cameraIdIndices[0] = 0
            cameraIdIndices[1] = 0
        }
    }

    private fun initCameraParameter(cameraId: String, facing: Int) {
        val realSize = Point()
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getRealSize(realSize)
        val width = realSize.x
        val height = realSize.y

        val previewSize: Size?
        val videoSize: Size?

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap =
                characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )
            printSizes(
                streamConfigurationMap?.getOutputSizes(
                    SurfaceTexture::class.java
                ), "preview"
            )
            printSizes(
                streamConfigurationMap?.getOutputSizes(
                    MediaRecorder::class.java
                ), "video"
            )
            previewSize = getFittedPreviewSize(
                streamConfigurationMap.getOutputSizes(
                    SurfaceTexture::class.java
                ), 0.75f
            )
            videoSize = getOptimalSize(
                streamConfigurationMap?.getOutputSizes(
                    MediaRecorder::class.java
                ), previewSize!!.height, previewSize.width, "video"
            )
//            if(mCameraRatio == CAMERA_RATIO_FULL) {
//                previewSize = getOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height, "preview");
//                videoSize = getOptimalSize(streamConfigurationMap.getOutputSizes(MediaRecorder.class), previewSize.getHeight(), previewSize.getWidth(), "video");
//            } else {
//                previewSize = getFittedPreviewSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), 0.75f);
//                videoSize = getOptimalSize(streamConfigurationMap.getOutputSizes(MediaRecorder.class), previewSize.getHeight(), previewSize.getWidth(), "video");
//            }
            Log.d(TAG, "displayMetrics w = $width  h = $height")
            Log.d(
                TAG,
                "previewSize w = " + previewSize.width + "  h = " + previewSize.height
            )
            Log.d(
                TAG,
                "videoSize w = " + videoSize!!.width + "  h = " + videoSize.height
            )

            displayedPreviewSize[0] = previewSize.width
            displayedPreviewSize[1] = previewSize.height

            displayedVideoSize[0] = videoSize.width
            displayedVideoSize[1] = videoSize.height

            cameraSensorResolution = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )
            imageReader = ImageReader.newInstance(
                displayedPreviewSize[0], displayedPreviewSize[1],
                ImageFormat.YUV_420_888,  /*maxImages*/2
            )
            imageReader?.setOnImageAvailableListener(mOnImageAvailableListener, handler)
            val sensorOrientation = characteristics.get(
                CameraCharacteristics.SENSOR_ORIENTATION
            )
            orientation = getJpegOrientation(facing, deviceRotation, sensorOrientation)
            //mCameraOrientation = getCameraDisplayOrientation(facing, mDeviceRotation, sensorOrientation);
            // Check if the flash is supported.
            val available = characteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE
            )
            supportedFlash = available ?: false
            // Check if the face detection is supported.
            val fdMode = characteristics.get(
                CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES
            ) ?: IntArray(0)
            val maxFdCount = characteristics.get(
                CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT
            ) ?: 0
            if (fdMode.isNotEmpty()) {
                val fdList: MutableList<Int> = ArrayList()
                for (mode in fdMode) {
                    fdList.add(mode)
                }
                if (maxFdCount > 0) {
                    faceDetectSupported = true
                    faceDetectMode = Collections.max(fdList)
                }
            }
            // calculateFOV
            val maxFocus = characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            val size = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )
            val w = size.width
            val h = size.height
            // FOV (rectilinear) =  2 * arctan (frame size/(focal length * 2))
            horizontalViewAngle =
                (2 * atan(w / (maxFocus[0] * 2).toDouble())).toFloat()
            verticalViewAngle =
                (2 * atan(h / (maxFocus[0] * 2).toDouble())).toFloat()
            Log.i(
                TAG,
                String.format("HorizontalViewAngle:%.2f", horizontalViewAngle)
            )
            Log.i(
                TAG,
                String.format("VerticalViewAngle:%.2f", verticalViewAngle)
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera(cameraId: String?, facing: Int) {
        if (cameraDevice != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(
                    2500,
                    TimeUnit.MILLISECONDS
                )
            ) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            if (cameraId != null) {
                manager.openCamera(cameraId, mStateCallback, handler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            if (null != captureSession) {
                captureSession?.close()
                captureSession = null
            }
            if (null != cameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }
            if (null != imageReader) {
                imageReader?.close()
                imageReader = null
            }
            cameraTexture = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startCamera(facing: Int, cameraIdIndex: Int) {
        Log.d(TAG, "startCamera $isStarted $cameraTexture")
        synchronized(isStarted) {
            if (isStarted.compareAndSet(false, true)) {
                if (frontCameraIds.size == 0 && rearCameraIds.size == 0) {
                    isStarted.set(false)
                    return
                }
                val cameraId = getCameraId(facing, cameraIdIndex)
                if (cameraId == null) {
                    isStarted.set(false)
                    return
                }
                initCameraParameter(cameraId, facing)
            }
        }
    }

    private fun changeCamera(facing: Int, cameraIdIndex: Int) {
        if (isStarted.compareAndSet(true, false)) {
            try {
                closeCamera()
            } catch (e: NullPointerException) {
                Log.e(TAG, "Error Stopping camera - NullPointerException: ", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Error Stopping camera - RuntimeException: ", e)
            }
        }
        val cameraId = getCameraId(facing, cameraIdIndex)
        if (cameraId == null) {
            isStarted.set(false)
            return
        }
        if (isStarted.compareAndSet(false, true)) {
            initCameraParameter(cameraId, facing)
        }
        switchingCamera.set(false)
    }

    private fun setCameraFlash(flag: Boolean) {
        try {
            if (cameraID == CAMERA_FACING_BACK) {
                if (supportedFlash) {
                    if (flag) {
                        previewRequestBuilder?.set(
                            CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH
                        )
                        captureSession?.setRepeatingRequest(
                            previewRequestBuilder?.build(),
                            null,
                            null
                        )
                    } else {
                        previewRequestBuilder?.set(
                            CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF
                        )
                        captureSession?.setRepeatingRequest(
                            previewRequestBuilder?.build(),
                            null,
                            null
                        )
                    }
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getCameraDisplayOrientation(
        facing: Int,
        deviceRotation: Int,
        sensorOrientation: Int
    ): Int {
        Log.d(
            TAG,
            "setCameraDisplayOrientation $deviceRotation $sensorOrientation"
        )
        var degrees = 0
        when (deviceRotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (facing == CAMERA_FACING_FRONT) {
            result = (sensorOrientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else { // back-facing
            result = (sensorOrientation - degrees + 360) % 360
        }
        return result
    }

    private fun getJpegOrientation(
        facing: Int,
        deviceOrientation: Int,
        sensorOrientation: Int
    ): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90
        // Reverse device orientation for front-facing cameras
        if (facing == CAMERA_FACING_FRONT) deviceOrientation =
            -deviceOrientation
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }

    private fun getOptimalSize(
        sizes: Array<Size>?,
        w: Int,
        h: Int,
        where: String
    ): Size? {
        Log.d(TAG, "getOptimalSize $w $h $where $sizes")
        if (sizes == null) {
            return null
        }
        val targetRatio = h.toDouble() / w
        var optimalSize: Size? = null
        val ASPECT_TOLERANCE = 0.0
        // 1. find exactly matched
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            val maxSize = max(size.width, size.height)
            Log.d(
                TAG, "optimal size (exactly) " + size.width + " " + size.height + " " +
                        ratio + " " + targetRatio + " " + abs(ratio - targetRatio)
            )
            var isCorrectkSize: Boolean
            isCorrectkSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                size.height <= w
            } else {
                maxSize <= 1920 && size.height <= w
            }
            if (abs(ratio - targetRatio) == ASPECT_TOLERANCE && isCorrectkSize) {
                optimalSize = size
                break
            }
        }
        if (optimalSize == null) { // 2. find matched
            var minDiff = Double.MAX_VALUE
            for (size in sizes) {
                val ratio = size.width.toDouble() / size.height
                val maxSize = max(size.width, size.height)
                Log.d(
                    TAG, "optimal size (step 2) " + size.width + " " + size.height + " "
                            + targetRatio + " " + minDiff
                )
                var isCorrectkSize: Boolean
                isCorrectkSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    size.height <= w
                } else {
                    maxSize <= 1920 && size.height <= w
                }
                if (Math.abs(targetRatio - ratio) < minDiff && isCorrectkSize) {
                    optimalSize = size
                    minDiff = abs(targetRatio - ratio)
                }
            }
        }
        if (optimalSize != null) {
            Log.d(
                TAG,
                "result size " + targetRatio + " " + optimalSize.width.toDouble() / optimalSize.height.toDouble()
                        + " " + optimalSize.width + " " + optimalSize.height
            )
        } else {
            Log.d(TAG, "could not find optimal size $targetRatio")
        }
        return optimalSize
    }

    private fun getFittedPreviewSize(
        supportedPreviewSize: Array<Size>,
        previewRatio: Float
    ): Size? {
        var retSize: Size? = null
        for (size in supportedPreviewSize) {
            val ratio = size.height / size.width.toFloat()
            val EPSILON = 1e-4.toFloat()
            if (abs(ratio - previewRatio) < EPSILON && max(
                    size.width,
                    size.height
                ) <= 1920
            ) {
                if (retSize == null || retSize.width < size.width) {
                    retSize = size
                }
            }
        }
        return retSize
    }

    private fun printSizes(
        sizes: Array<Size>?,
        title: String
    ) {
        Log.d(TAG, "==== print sizes for $title")
        if (sizes != null) {
            for (size in sizes) {
                val ratio = size.width.toDouble() / size.height
                Log.d(
                    TAG,
                    size.width.toString() + " " + size.height + " " + ratio
                )
            }
        }
        Log.d(TAG, "==== print sizes end $title")
    }

    private fun createCameraPreviewSession() {
        try {
            var surface: Surface? = null
            cameraTexture?.setDefaultBufferSize(previewSize[0], previewSize[1])
            surface = Surface(cameraTexture)
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)
            previewRequestBuilder?.addTarget(imageReader!!.surface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(
                Arrays.asList<Surface>(surface, imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) { // The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        try { // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Enable face detection if supported
                            if (faceDetectSupported) {
                                previewRequestBuilder?.set(
                                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                    faceDetectMode
                                )
                            }
                            //Range<Integer> fpsRange = Range.create(30, 30);
                            //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                            // Flash is automatically enabled when necessary.
                            if (supportedFlash) {
                                previewRequestBuilder?.set(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON
                                )
                            }
                            // Set orientation
                            previewRequestBuilder?.set(
                                CaptureRequest.JPEG_ORIENTATION,
                                orientation
                            )
                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder?.build()
                            captureSession?.setRepeatingRequest(
                                previewRequest,
                                mCaptureCallback, handler
                            )
                            val fpsRange =
                                previewRequestBuilder?.get(
                                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
                                )
                            listener?.setConfig(
                                previewSize[0],
                                previewSize[1],
                                verticalViewAngle,
                                horizontalViewAngle,
                                orientation,
                                isCameraFacingFront(),
                                fpsRange?.upper?.toFloat() ?: 30.0f
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(
                        cameraCaptureSession: CameraCaptureSession
                    ) {
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader ->
            handler?.post {
                val image = reader.acquireLatestImage()
                if (image != null) {
                    listener.feedRawData(image)
                    image.close()
                }
            }
        }

    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release()
            this@ReferenceCamera2.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@ReferenceCamera2.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@ReferenceCamera2.cameraDevice = null
            Log.e(TAG, "CameraDevice.StateCallback onError() $error")
        }
    }

    private val mCaptureCallback: CaptureCallback = object : CaptureCallback() {
        private fun process(result: CaptureResult) {
            val mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)
            val faces =
                result.get(
                    CaptureResult.STATISTICS_FACES
                )
            if (faces != null && mode != null) {
                if (faces.isNotEmpty()) {
                    //Log.e(TAG, "face detected = " + faces.length);
                    val bbox = Array(faces.size) { IntArray(4) }
                    var rect: Rect
                    for (i in faces.indices) {
                        rect = faces[i].bounds
                        bbox[i][0] = rect.left * previewSize[0] / cameraSensorResolution!!.width
                        bbox[i][1] = rect.top * previewSize[1] / cameraSensorResolution!!.height
                        bbox[i][2] = rect.right * previewSize[0] / cameraSensorResolution!!.width
                        bbox[i][3] = rect.bottom * previewSize[1] / cameraSensorResolution!!.height
                    }
                    listener.updateFaceRects(faces.size, bbox)
                }
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }
    }

    companion object {
        private val TAG = ReferenceCamera2::class.java.simpleName

        // 아래의 카메라 ID에 기종별 int 값을 정의하면 됩니다. (현재는 Camera API의 Camera ID 사용하고 있습니다.)
        private const val CAMERA_FACING_BACK = CameraCharacteristics.LENS_FACING_BACK
        private const val CAMERA_FACING_FRONT = CameraCharacteristics.LENS_FACING_FRONT
    }

    init {
        super.listener = listener
        this.deviceRotation = deviceRotation
        isStarted = AtomicBoolean(false)
        this.context = context
        initializeCameraIds()
        startHandlerThread()
        if (cameraFacingFront != -1) {
            setFacing(cameraFacingFront)
        } else if (cameraFacingBack != -1) {
            setFacing(cameraFacingBack)
        }
    }
}