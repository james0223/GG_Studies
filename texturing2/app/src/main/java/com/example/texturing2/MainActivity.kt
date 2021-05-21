package com.example.texturing2

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.charset.Charset
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class MainActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
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
    private val mContext: Context = context
    private var vPMatrix = FloatArray(16)
    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)

    private lateinit var rectangle: Rectangle

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        rectangle = Rectangle(mContext)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 1.0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        rectangle.draw(vPMatrix)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)
    }
}

class Rectangle(context: Context){
    private val myContext: Context = context
    private val vertexShaderStream = context.resources.openRawResource(R.raw.vertex)
    private val vertexShaderCode = vertexShaderStream.readBytes().toString(Charset.defaultCharset())

    private val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment)
    private val fragmentShaderCode =fragmentShaderStream.readBytes().toString(Charset.defaultCharset())

    private var vPMatrixHandle: Int = 0

    private var mProgram: Int

    private val rectangleCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f,
        -0.5f, 0.5f, 0.0f,
        0.5f, -0.5f, 0.0f,
        0.5f, 0.5f, 0.0f
    )
    private val texCoord = floatArrayOf(
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )
    private val colorCoord = floatArrayOf( //---
        1.0f, 0.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f
    )
    private val colorR = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)

    private var vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(rectangleCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(rectangleCoords)
                position(0)
            }
        }
    private var texBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(texCoord.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(texCoord)
                position(0)
            }
        }
    private var colorBuffer: FloatBuffer = //---
        ByteBuffer.allocateDirect(colorCoord.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(colorCoord)
                position(0)
            }
        }

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    val COORDS_PER_VERTEX = 3
    val COORDS_PER_COLOR = 4
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0
    private val vertexCount: Int = 18 / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4
    private val colorStride: Int = COORDS_PER_COLOR * 4

    private var mTextureUniformHandle = 0
    private var mTextureDataHandle = 0
    private var mTextureDataHandle2 = 0
    private var mTextureCoordinateHandle = 0
    private val COORDS_PER_TEXEL = 2
    private val texelStride: Int = COORDS_PER_TEXEL * 4

    fun draw(mvpMatrix: FloatArray){
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor")
        GLES20.glEnableVertexAttribArray(mColorHandle)
        GLES20.glVertexAttribPointer(mColorHandle, COORDS_PER_COLOR, GLES20.GL_FLOAT, false, colorStride, colorBuffer)

        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)


        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate")
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, COORDS_PER_TEXEL, GLES20.GL_FLOAT, false, texelStride, texBuffer)

        //mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")

        mTextureDataHandle = loadTexture(myContext, R.drawable.ground1)
        mTextureDataHandle2 = loadTexture(myContext, R.drawable.ground2)
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle)
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle2)
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        //GLES20.glUniform1i(mTextureUniformHandle, 0)

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "u_Texture1"), 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "u_Texture2"), 1)


        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false // No pre-scaling

            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // Set filtering (minification, magnification)
            //glTexParameteri(target, pname, param)
            //target: GL_TEXTURE_2D (2D image texture), GL_TEXTURE_CUBE_MAP
            //pname: GL_TEXTURE_WRAP (texture wrapping) / param: GL_REPEAT, GL_MIRRORED_REPEAT, GL_CLAMP_TO_EDGE
            //pname: GL_TEXTURE_MIN(MAX)_FILTERING (texture filtering) / param: GL_NEAREST, GL_LINEAR, GL_NEAREST_MIPMAP_LINEAR ...
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_MIRRORED_REPEAT) //---
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE) //---
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            //GLES20.glTexImage2D()

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle()
        }
        if (textureHandle[0] == 0) {
            throw RuntimeException("Error loading texture.")
        }
        return textureHandle[0]
    }

    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)

        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}