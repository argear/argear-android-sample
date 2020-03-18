package com.seerslab.argear.sample.rendering

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import javax.microedition.khronos.opengles.GL10

class CameraTexture {

    var textureId: Int = 0
        private set

    lateinit var surfaceTexture: SurfaceTexture
        private set

    fun createCameraTexture() {
        val texture = IntArray(1)

        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE
        )

        textureId = texture[0]
        surfaceTexture = SurfaceTexture(textureId)
    }
}