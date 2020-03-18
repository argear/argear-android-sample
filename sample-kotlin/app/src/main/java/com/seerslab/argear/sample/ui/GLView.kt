package com.seerslab.argear.sample.ui

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.View
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLView: GLSurfaceView, GLSurfaceView.Renderer {

    companion object {
        private val TAG = GLView::class.java.simpleName
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, listener: GLViewListener) : super(context) {
        init()
        this.listener = listener
    }

    var viewWidth = 0
    var viewHeight = 0
    private var listener: GLViewListener? = null

    private fun init() {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 8)
        holder.setFormat(PixelFormat.RGBA_8888)

        setRenderer(this)
        setZOrderOnTop(true)

        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = false
    }

    // region - GLSurfaceView
    override fun onResume() {
        super.onResume()
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onPause() {
        super.onPause()
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        val glviewWidth: Int = (context as CameraActivity).gLViewWidth
        val glviewHeight: Int = (context as CameraActivity).gLViewHeight
        Log.d(TAG, "onMeasure $glviewWidth $glviewHeight $width $height")

        if (glviewWidth > 0 && glviewHeight > 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(glviewWidth, glviewHeight)
            (context as CameraActivity).setMeasureSurfaceView(this)
        } else {
            setMeasuredDimension(width, height)
        }
    }

    // endregion
    // region - Renderer
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        listener?.onSurfaceCreated(gl, config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        listener?.onDrawFrame(gl, viewWidth, viewHeight)
    }
    // endregion

    interface GLViewListener {
        fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
        fun onDrawFrame(gl: GL10?, width: Int?, height: Int?)
    }
}