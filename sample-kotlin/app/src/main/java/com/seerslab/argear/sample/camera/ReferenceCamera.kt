package com.seerslab.argear.sample.camera

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.Image

abstract class ReferenceCamera {

    protected var listener: CameraListener? = null

    abstract val cameraFacingFront: Int
    abstract val cameraFacingBack: Int

    abstract fun setCameraTexture(textureId: Int?, surfaceTexture: SurfaceTexture?)
    abstract fun setFacing(CameraFacing: Int)
    abstract fun isCameraFacingFront(): Boolean
    abstract val previewSize: IntArray?
    abstract fun startCamera()
    abstract fun stopCamera()
    abstract fun destroy()
    abstract fun changeCameraFacing(): Boolean

    interface CameraListener {
        fun setConfig(
            previewWidth: Int,
            previewHeight: Int,
            verticalFov: Float,
            horizontalFov: Float,
            orientation: Int,
            isFrontFacing: Boolean,
            fps: Float
        )
        // region - for camera api 1
        fun feedRawData(data: ByteArray?)
        // endregion
        // region - for camera api 2
        fun feedRawData(data: Image?)
        // endregion
    }
}