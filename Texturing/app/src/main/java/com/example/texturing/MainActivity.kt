package com.example.texturing

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

    val colorR = floatArrayOf(1.0f, 0f, 0f, 1f)
    val colorG = floatArrayOf(0f, 1.0f, 0f, 1f)
    val colorB = floatArrayOf(0f, 0f, 1.0f, 1f)

    private var rectangleCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f,
        -0.5f, 0.5f, 0.0f,
        0.5f, -0.5f, 0.0f,
        0.5f, 0.5f, 0.0f
    )

    private var texCoord = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f
    )

    private var vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(rectangleCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(rectangleCoords)
                position(0)
            }
        }

    private var texBuffer = ByteBuffer.allocateDirect(texCoord.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            for (texture in texCoord){
                put(texture)
            }
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
    private val vertexCount: Int = 18 / COORDS_PER_VERTEX
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    //    /** This will be used to pass in the texture.  */
    private var mTextureUniformHandle = 0
    //
//    /** This will be used to pass in model texture coordinate information.  */
    private var mTextureCoordinateHandle = 0
    //
//    /** Size of the texture coordinate data in elements.  */
    private val mTextureCoordinateDataSize = 2
    //
//    /** This is a handle to our texture data.  */
    private var mTextureDataHandle = 0

    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray){
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(mColorHandle, 1, colorR, 0)


        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)



        // So what the OpenGL API does is to create the notion of an "active texture".
        // Then when we call an OpenGL API function to copy an image into a texture, we must do it this way:
        // 1:  generate a texture and assign its identifier to an unsigned integer variable.
        // 2:  bind the texture to the GL_TEXTURE bind point (or some such bind point).
        // 3:  specify the size and format of the texture bound to GL_TEXTURE target.
        // 4:  copy some image we want on the texture to the GL_TEXTURE target.
        // And if we want to draw an image on another texture, we must repeat that same process.

        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate")
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, mTextureCoordinateDataSize*4, texBuffer)

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")

        // 텍스쳐 불러오기
        mTextureDataHandle = loadTexture(myContext, R.drawable.ground1)

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle)

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0)


        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    fun loadTexture(context: Context, resourceId: Int): Int {
        // GLuint myTexture;
        // glGenTextures(1, &myTexture); // generate just one texture
        // GLuint myTextures[32];
        // glGenTextures(32, myTextures); // generate 32 textures
        // textureHandle이 여기서의 myTexture이다
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0) // 1개의 텍스처 오브젝트를 textureHandle에 저장

        if (textureHandle[0] != 0) { // 0이 아닌지의 의미??--------------------------------------------
            val options = BitmapFactory.Options()
            options.inScaled = false // No pre-scaling

            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Bind to the texture in OpenGL
            // GL_TEXTURE_2D에 textureHandle[0]의 요소를 bind함
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // Texture Filtering과 Texture Wrapping(텍스쳐 포장)의 설정을 지정하는 것이 glTexParameteri의 역할
            // GL_NEAREST는 근접점 샘플링을, GL_LINEAR은 겹선형보간을 의미
            // GL_TEXTURE_MIN_FILTER는 텍스쳐 축소의 경우의 설정
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            // GL_TEXTURE_MAG_FILTER는 텍스쳐 확대의 경우의 설정
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

            // Load the bitmap into the bound texture.
            // 텍스처 오브젝트를 실제 텍스쳐로 채우기 위해 GLTexImage2D를 호출
            // 이 때, GLTexImage2D는 텍스처 오브젝트 ID는 파라미터로 필요로 하지 않는다
            // 이는 일단 텍스쳐 오브젝트가 바인딩되면, 그 이후에 호출되는 함수들은 기본적으로 바인딩된 텍스처 오브젝트를 사용하기 때문이다
            GLUtils.texImage2D(
                GLES20.GL_TEXTURE_2D, // target
                0, // mipmap level
                bitmap, // 
                0
            )

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            // Android에서 Java로 Bitmap을 다룰 경우 OOM(Out-Of-Memory)를 늘 신경쓰며 작업을 해야한다.
            // 아무리 기기에 램이 많아도 Java로 프로그래밍을 할 경우, 한 앱이 사용할 수있는 메모리의 크기는 제한되어있기 때문에, 조금이라도 큰 Bitmap을 처리할 경우 OOM을 피할 수는 없다.
            // 그래서 Bitmap을 처리할 때, 필요한 최소 사이즈로 불러와야 하며, 사용이 끝난 Bitmap은 즉시 즉시 Recycle을 해줘야 한다.
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