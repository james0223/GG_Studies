package com.example.jhp_hw1

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.lang.Math.toRadians
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.charset.Charset
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan


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
    private val cosval = cos(toRadians(0.8).toFloat())
    private val sinval = sin(toRadians(0.8).toFloat())

    //P. model matrix & 매 프레임 변화 matrix 선언
    private var cModelMatrix = floatArrayOf(
        0.5f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 0.5f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    private var pModelMatrix = floatArrayOf(
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        2.0f, 0.0f, 0.0f, 1.0f
    )
    private var tTMatrix = floatArrayOf(
        0.2f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.2f, 0.0f, 0.0f,
        0.0f, 0.0f, 0.2f, 0.0f,
        1.25f, 0.4f, 0.0f, 1.0f
    )
    private var tRMatrix = floatArrayOf(
        -1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, -1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    private var changeCube = floatArrayOf(
        1.001f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.002f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.001f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    private var changePerson = floatArrayOf(
        cosval, 0.0f, -sinval, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        sinval, 0.0f, cosval, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    private var changeTeapot = floatArrayOf(
        cosval, 0.0f, -sinval, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        sinval, 0.0f, cosval, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    private var tModelMatrix = FloatArray(16)

    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)
    private var vPMatrix = FloatArray(16)
    private var cMVPMatrix = FloatArray(16)
    private var pMVPMatrix = FloatArray(16)
    private var tMVPMatrix = FloatArray(16)

    //P. object 선언
    private lateinit var myCube: TheObject
    private lateinit var myPerson: TheObject
    private lateinit var myTeapot: TheObject


    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        //P. object 초기화
        myCube = TheObject(mContext, "cube.obj")
        myPerson = TheObject(mContext, "person.obj")
        myTeapot = TheObject(mContext, "teapot.obj")

        //P. model matrix & 매 프레임 변화 matrix 초기화
        Matrix.multiplyMM(tModelMatrix, 0, tTMatrix, 0, tRMatrix, 0)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        //P. 아래 구현한 mySetLookAtM function 으로 수정
        mySetLookAtM(viewMatrix, 0, 1.5f, 1.5f, -9f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
//        Matrix.setLookAtM(viewMatrix, 0, 1.5f, 1.5f, -9f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        //P. 각 object 별 매 프레임 변화 matrix 와 model matrix 를 multiply
        if (cModelMatrix[5] < 3) {
            Matrix.multiplyMM(cModelMatrix, 0, changeCube, 0, cModelMatrix, 0)
        }
        Matrix.multiplyMM(pModelMatrix, 0, changePerson, 0, pModelMatrix, 0)
        Matrix.multiplyMM(tModelMatrix, 0, changeTeapot, 0, tModelMatrix, 0)

        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(cMVPMatrix, 0, vPMatrix, 0, cModelMatrix, 0)
        Matrix.multiplyMM(pMVPMatrix, 0, vPMatrix, 0, pModelMatrix, 0)
        Matrix.multiplyMM(tMVPMatrix, 0, vPMatrix, 0, tModelMatrix, 0)

        //P. object draw
        myCube.draw(cMVPMatrix)
        myPerson.draw(pMVPMatrix)
        myTeapot.draw(tMVPMatrix)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        //P.  아래 구현한 myFrustumM function 으로 수정
        projectionMatrix = myFrustumM(ratio, 60f, 2f, 12f)
//        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 12f)
    }
}

//P. vecNormalize function 구현: 벡터 정규화 함수 (mySetLookAtM function 구현 시 사용)
fun vecNormalize(inputVector: FloatArray) {
    val theMagnitude = sqrt((inputVector[0] * inputVector[0] + inputVector[1] * inputVector[1] + inputVector[2] * inputVector[2]))
    val tempMatrix = floatArrayOf(
        1.0f/theMagnitude, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f/theMagnitude, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f/theMagnitude, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    Matrix.multiplyMV(inputVector, 0, tempMatrix, 0, inputVector, 0)
}

//P. mySetLookAtM function 구현: viewMatrix 구하는 함수 (Matrix library function 중 multiplyMM 만 사용 가능)
fun mySetLookAtM(target: FloatArray, rmOffset: Int, eyeX: Float, eyeY: Float, eyeZ: Float, centerX: Float, centerY: Float, centerZ: Float, upX: Float, upY: Float, upZ: Float) {
    val MoveMatrix = floatArrayOf(
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        -eyeX, -eyeY, -eyeZ, 1.0f,
    )
    val n = floatArrayOf(
        eyeX - centerX, eyeY - centerY, eyeZ - centerZ, 1.0f
    )
    vecNormalize(n)
    val u = floatArrayOf(
        upY * n[2] - upZ * n[1], upZ * n[0] - upX * n[2], upX * n[1] - upY * n[0], 1.0f
    )
    vecNormalize(u)
    val v = floatArrayOf( // n x u
        n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0], 1.0f
    )
    val RotationMatrix = floatArrayOf(
        u[0], v[0], n[0], 0.0f,
        u[1], v[1], n[1], 0.0f,
        u[2], v[2], n[2], 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )
    Matrix.multiplyMM(target, 0, RotationMatrix, 0, MoveMatrix, 0)
}

//P. myFrustumM function 구현: projectionMatrix 구하는 함수 (Matrix library function 중 multiplyMM 만 사용 가능)
fun myFrustumM(aspect: Float, fovy: Float,  near: Float, far: Float): FloatArray {
    val Rfovy = toRadians(fovy.toDouble()).toFloat()
    return floatArrayOf(
        (1/ tan(Rfovy/2))/aspect, 0.0f, 0.0f, 0.0f,
        0.0f, 1/tan(Rfovy/2), 0.0f, 0.0f,
        0.0f, 0.0f, -(far + near)/(far - near), -1.0f,
        0.0f, 0.0f, -(2 * near * far)/(far - near), 0.0f,
    )
}


//PP. cube, person, teapot 모두 포함할 수 있는 Object class 로 수정
class TheObject(context: Context, fileName: String){

    //P. model matrix handle 변수 추가 선언
    private var vPMatrixHandle: Int = 0
    val color = floatArrayOf(1.0f, 0.980392f, 0.980392f, 0.3f)
    private var mProgram = GLES20.glCreateProgram()
    private var vertices = mutableListOf<Float>()
    private var faces = mutableListOf<Short>()
    private lateinit var verticesBuffer: FloatBuffer
    private lateinit var facesBuffer: ShortBuffer
    private val vertexShaderStream = context.resources.openRawResource(R.raw.vertex)
    private val vertexShaderCode = vertexShaderStream.readBytes().toString(Charset.defaultCharset())
    private val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment)
    private val fragmentShaderCode =fragmentShaderStream.readBytes().toString(Charset.defaultCharset())

    init {
        try {
            val scanner = Scanner(context.assets.open(fileName))
            while (scanner.hasNextLine()){
                val line = scanner.nextLine()
                if (line.startsWith("v  ")){
                    val vertex = line.split(" ")
                    val x = vertex[2].toFloat()
                    val y = vertex[3].toFloat()
                    val z = vertex[4].toFloat()
                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z)
                }
                else if (line.startsWith("f ")) {
                    val face = line.split(" ")
                    val vertex1 = face[1].split("/")[0].toShort()
                    val vertex2 = face[2].split("/")[0].toShort()
                    val vertex3 = face[3].split("/")[0].toShort()
                    faces.add(vertex1)
                    faces.add(vertex2)
                    faces.add(vertex3)
                }
            }

            verticesBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    for (vertex in vertices){
                        put(vertex)
                    }
                    position(0)
                }
            }

            facesBuffer = ByteBuffer.allocateDirect(faces.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    for (face in faces){
                        put((face-1).toShort())
                    }
                    position(0)
                }
            }

            val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

            GLES20.glAttachShader(mProgram, vertexShader)
            GLES20.glAttachShader(mProgram, fragmentShader)
            GLES20.glLinkProgram(mProgram)
        } catch (e: Exception){
            Log.e("file_read", e.message.toString())
        }
    }

    val COORDS_PER_VERTEX = 3

    private var positionHandle: Int = 0

    private var colorHandle: Int = 0

    private val vertexStride: Int = COORDS_PER_VERTEX * 4

    //PP. cube, person, teapot 의 world transform 및 매 프레임 변화를 반영할 수 있는 draw function 으로 수정
    fun draw(mvpMatrix: FloatArray){
        GLES20.glUseProgram(mProgram)
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {
            GLES20.glEnableVertexAttribArray(it)
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                verticesBuffer
            )

            colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, faces.size, GLES20.GL_UNSIGNED_SHORT, facesBuffer)

            GLES20.glDisableVertexAttribArray(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}