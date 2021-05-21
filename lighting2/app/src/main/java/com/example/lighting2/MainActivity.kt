package com.example.lighting2

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
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

    private lateinit var cube: Obj

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        //set viewMatrix
        Matrix.setLookAtM(viewMatrix, 0, 1f, 2f, -5f, 0f, 0f, 0f, 0f, 1f, 0f)
        //object triangle
        cube = Obj(mContext)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        cube.draw(viewMatrix, projectionMatrix)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        //set projectionMatrix
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }
}

class Obj(context: Context){
    private var vertices = mutableListOf<Float>()
    private var indices = mutableListOf<Short>()
    private var normals = mutableListOf<Float>()

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    private lateinit var normalBuffer: FloatBuffer

    private val vertexShaderStream = context.resources.openRawResource(R.raw.vertex)
    private val vertexShaderCode = vertexShaderStream.readBytes().toString(Charset.defaultCharset())

    private val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment)
    private val fragmentShaderCode =fragmentShaderStream.readBytes().toString(Charset.defaultCharset())

    private var mProgram: Int = 0//primitive type

    init {
        try {
            val scanner = Scanner(context.assets.open("cube.obj"))
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                if (line.startsWith("v  ")) {
                    val vertex = line.split(" ")
                    val x = vertex[2].toFloat()
                    val y = vertex[3].toFloat()
                    val z = vertex[4].toFloat()
                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z)
                } else if (line.startsWith("f ")) {
                    val face = line.split(" ")
                    val vertex1 = face[1].split("/")[0].toShort()
                    val vertex2 = face[2].split("/")[0].toShort()
                    val vertex3 = face[3].split("/")[0].toShort()
                    indices.add(vertex1)
                    indices.add(vertex2)
                    indices.add(vertex3)
                } else if (line.startsWith("vn ")){
                    val normal = line.split(" ")
                    val a = normal[1].toFloat()
                    val b = normal[2].toFloat()
                    val c = normal[3].toFloat()
                    normals.add(a)
                    normals.add(b)
                    normals.add(c)
                }
            }
            Log.d("test", vertices.size.toString())//24 (x, y, z) * 8
            Log.d("test", indices.size.toString())//36 (xyz index) 3 * 12
            Log.d("test", normals.size.toString())//24 (x, y, z) * 8

            //vertex buffer
            val vertexByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())//96
            vertexBuffer = vertexByteBuffer.asFloatBuffer()
            for(vertex in vertices){
                vertexBuffer.put(vertex)
            }
            vertexBuffer.position(0)

            //index buffer
            val indexByteBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder())//72
            indexBuffer = indexByteBuffer.asShortBuffer()
            for(index in indices) {
                indexBuffer.put((index - 1).toShort())
            }
            indexBuffer.position(0)

            //normal buffer
            val normalByteBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder())//96
            normalBuffer = normalByteBuffer.asFloatBuffer()
            for (normal in normals){
                normalBuffer.put(normal)
            }
            normalBuffer.position(0)

            //shader
            val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

            //program
            mProgram = GLES20.glCreateProgram()
            GLES20.glAttachShader(mProgram, vertexShader)
            GLES20.glAttachShader(mProgram, fragmentShader)
            GLES20.glLinkProgram(mProgram)

        } catch (e: Exception) {
            Log.e("file_read", e.message.toString())
        }
    }

    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var normalHandle: Int = 0
    private var lightHandle: Int = 0;
    private var viewPosHandle: Int = 0;

    private var lPosition = floatArrayOf(1.2f, 1.0f, -2.0f, 1f)

    private var vMatrixHandle: Int = 0
    private var pMatrixHandle: Int = 0

    private val COORDS_PER_VERTEX = 3
    private val vertexStride = COORDS_PER_VERTEX * 4

    private val COORDS_PER_COLOR = 3
    private val colorStride = COORDS_PER_COLOR * 4

    private val COORDS_PER_NORMAL = 3
    private val normalStride = COORDS_PER_NORMAL * 4

    fun draw(vMatrix: FloatArray, pMatrix: FloatArray){
        //use program
        GLES20.glUseProgram(mProgram)

        //vertex handle
        positionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        //vertex shader matrix handle
        vMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, vMatrix, 0)
        pMatrixHandle = GLES20.glGetUniformLocation(mProgram, "pMatrix")
        GLES20.glUniformMatrix4fv(pMatrixHandle, 1, false, pMatrix, 0)

        //normal handle
        normalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal")
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, COORDS_PER_NORMAL, GLES20.GL_FLOAT, false, normalStride, normalBuffer)

        //color handle
        colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, colorR, 0)

        //light handle
        lightHandle = GLES20.glGetUniformLocation(mProgram, "lightPosition")
        GLES20.glUniform4fv(lightHandle, 1, lPosition, 0)


        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }
}

val colorR = floatArrayOf(1f, 0.5f, 0.31f, 1.0f)

fun loadShader(type: Int, shaderCode: String): Int{
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
    GLES20.glGetShaderInfoLog(shader)
    return shader
}