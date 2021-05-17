package com.example.lighting

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.contentValuesOf
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.charset.Charset
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = MyGLSurfaceView(this)
        setContentView(glView)
    }
}

class MyGLSurfaceView(context: Context): GLSurfaceView(context){
    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)
        setRenderer(renderer)
    }
}

class MyGLRenderer(context: Context): GLSurfaceView.Renderer{
    private val mContext = context

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private lateinit var triangle: Obj

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(1f, 1f, 1f, 1.0f)
        //set viewMatrix
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f)
        //object triangle
        triangle = Obj(mContext, triangleCoords, triangleNormals)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        triangle.draw(viewMatrix, projectionMatrix)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        //set projectionMatrix
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }
}

class Obj(context: Context, coordinate: FloatArray, normal: FloatArray){
    private var vertexBuffer: FloatBuffer
    private var normalBuffer: FloatBuffer
    private var colorBuffer: FloatBuffer

    private val vertexShaderStream = context.resources.openRawResource(R.raw.vertex)
    private val vertexShaderCode = vertexShaderStream.readBytes().toString(Charset.defaultCharset())

    private val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment)
    private val fragmentShaderCode =fragmentShaderStream.readBytes().toString(Charset.defaultCharset())

    private var mProgram: Int//primitive type

    init {
        //vertex buffer
        val vertexByteBuffer = ByteBuffer.allocateDirect(coordinate.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        vertexBuffer.put(coordinate).position(0)

        //vertex normal buffer
        val normalByteBuffer = ByteBuffer.allocateDirect(normal.size * 4).order(ByteOrder.nativeOrder())
        normalBuffer = normalByteBuffer.asFloatBuffer()
        normalBuffer.put(normal).position(0)

        //color buffer
        val colorByteBuffer = ByteBuffer.allocateDirect(colorR.size * 3 * 4).order(ByteOrder.nativeOrder())
        colorBuffer = colorByteBuffer.asFloatBuffer()
        colorBuffer.put(colorR)
        colorBuffer.put(colorR)
        colorBuffer.put(colorR)
        colorBuffer.position(0)

        //shader
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        //program
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
    }

    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var vMatrixHandle: Int = 0
    private var pMatrixHandle: Int = 0

    private var lightHandle: Int = 0
    private var lPosition = floatArrayOf(0f, 0.6f, 0f, 1f)
    private var normalHandle: Int = 0

    private val COORDS_PER_VERTEX = 3
    private val vertexStride = COORDS_PER_VERTEX * 4
    private val vertexCount = coordinate.size / COORDS_PER_VERTEX

    private val COORDS_PER_COLOR = 4
    private val colorStride = COORDS_PER_COLOR * 4


    fun draw(vMatrix: FloatArray, pMatrix: FloatArray){
        //use program
        GLES20.glUseProgram(mProgram)

        //vertex handle
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        //vertex shader matrix handle
        vMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, vMatrix, 0)
        pMatrixHandle = GLES20.glGetUniformLocation(mProgram, "pMatrix")
        GLES20.glUniformMatrix4fv(pMatrixHandle, 1, false, pMatrix, 0)

        //light handle
        lightHandle = GLES20.glGetUniformLocation(mProgram, "lightPosition")
        // glUniform4fv - Specify the value of a uniform variable for the current program object
        GLES20.glUniform4fv(lightHandle, 1, lPosition, 0)

        //normal handle
        normalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal")
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, normalBuffer)

        //color handle
        colorHandle = GLES20.glGetAttribLocation(mProgram, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, COORDS_PER_COLOR, GLES20.GL_FLOAT, false, colorStride, colorBuffer)
        //colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        //GLES20.glUniform4fv(colorHandle, 1, colorR, 0)//(location, count, transpose, *value)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}

val triangleCoords = floatArrayOf(
    0.0f, 0.5772f, 0.0f,
    -0.5f, -0.2886f, 0.0f,
    0.5f, -0.2886f, 0.0f
)
val triangleNormals = floatArrayOf(
    0f, 1f, 0f,
    -0.86625f, -0.5f, 0f,
    0.86625f, -0.5f, 0f
)

val colorR = floatArrayOf(1.0f, 0f, 0f, 1f)
val colorG = floatArrayOf(0f, 1.0f, 0f, 1f)
val colorB = floatArrayOf(0f, 0f, 1.0f, 1f)


fun loadShader(type: Int, shaderCode: String): Int{
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
    return shader
}