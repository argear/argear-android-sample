package com.seerslab.argear.sample.rendering

import android.graphics.Bitmap
import android.opengl.GLES20
import com.seerslab.argear.session.ARGFrame
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ScreenRenderer {

    private lateinit var pVertex: FloatBuffer
    private lateinit var pTexCoord: FloatBuffer
    private var hProgram = 0

    private val vss = "attribute vec2 vPosition;\n" +
                            "attribute vec2 vTexCoord;\n" +
                            "varying vec2 texCoord;\n" +
                            "void main() {\n" +
                            "  texCoord = vTexCoord;\n" +
                            "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
                            "}"

    private val fss = "precision mediump float;\n" +
                            "uniform sampler2D sTexture;\n" +
                            "varying vec2 texCoord;\n" +
                            "void main() {\n" +
                            "  gl_FragColor = texture2D(sTexture,texCoord);\n" +
                            "}"

    fun create(gl: GL10?, config: EGLConfig?) {
        val vtmp = floatArrayOf(1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f)
        val ttmp = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f)

        pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        pVertex.put(vtmp)
        pVertex.position(0)
        pTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        pTexCoord.put(ttmp)
        pTexCoord.position(0)

        hProgram = loadShader(vss, fss)
    }

    fun draw(frame: ARGFrame, viewWidth: Int, viewHeight: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        GLES20.glUseProgram(hProgram)

        val ph = GLES20.glGetAttribLocation(hProgram, "vPosition")
        val tch = GLES20.glGetAttribLocation(hProgram, "vTexCoord")
        val th = GLES20.glGetUniformLocation(hProgram, "sTexture")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frame.textureId)
        GLES20.glUniform1i(th, 0)

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex)
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord)
        GLES20.glEnableVertexAttribArray(ph)
        GLES20.glEnableVertexAttribArray(tch)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFlush()
    }

    private fun loadShader(vss: String, fss: String): Int {
        var vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vshader, vss)
        GLES20.glCompileShader(vshader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(vshader)
            vshader = 0
        }

        var fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fshader, fss)
        GLES20.glCompileShader(fshader)
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(fshader)
            fshader = 0
        }

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vshader)
        GLES20.glAttachShader(program, fshader)
        GLES20.glLinkProgram(program)

        return program
    }
}