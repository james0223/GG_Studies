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

    // image texturing이란 polygon mesh의 각 vertex마다 texture coordinate를 할당하여 mesh에 texture를 입히는 형식이다

    // Texture coordinates define how an image (or portion of an image) gets mapped to a geometry.
    // A texture coordinate is associated with each vertex on the geometry, and it indicates what point within the texture image should be mapped to that vertex.
    // Texture coordinates are not stored with appearance, but on each geometry individually.
    // This allows separate geometries to share an appearance with an image texture, yet display distinct portions of that image on each geometry.
    // This technique is common when texture atlasing is used.
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
    private var mTextureDataHandle = IntArray(2)

    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray){
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(mColorHandle, 1, colorB, 0)


        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)



        // So what the OpenGL API does is to create the notion of an "active texture".
        // Then when we call an OpenGL API function to copy an image into a texture, we must do it this way:
        // 1:  generate a texture and assign its identifier to an unsigned integer variable.
        // 2:  bind the texture to the GL_TEXTURE bind point (or some such bind point).
        // 3:  specify the size and format of the texture bound to GL_TEXTURE target.
        // 4:  copy some image we want on the texture to the GL_TEXTURE target.
        // And if we want to draw an image on another texture, we must repeat that same process.

        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate") // a는 attribute를 의미
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, mTextureCoordinateDataSize*4, texBuffer)

        // 텍스쳐 불러오기(텍스쳐 정의)
        val texFileLocations = intArrayOf(R.drawable.ground1, R.drawable.rocks)
//        mTextureDataHandle = loadTexture(myContext, R.drawable.ground1)
        mTextureDataHandle = loadTexture(myContext, texFileLocations, texFileLocations.size)

        // 먼저 쉐이더를 builProgram(->만든 매서드)를 통해서 컴파일과 프로그램 링크시킨다
        // 그런 다음에 uniform과 attribute 위치를 가져온다.
        // 유니폼과 속성의 위치를 가지고 오면...
        // 이제는 거기다가 데이터를 집어넣는 일을 해야된다.
        // 먼저 유니폼데이터부터 하고,
        // 그다음에 속성데이터를 넣고.
        // 그다음에 그리면 된다.

        // 위에 정점 쉐이더에서 말하기를  uniform 데이터가 한개인데,
        //그게 uMatrixLocation 이다.
        //glUniformMatrix4fv 안에 matrix 데이터를 집어넣는다.
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture") // u는 uniform을 의미


        // Set the active texture unit to texture unit 0.
        // glActiveTexture(GLenum mTexture)
        // Specifies which texture unit to make active.
        // The number of texture units is implementation dependent, but must be at least 80.
        // mTexture must be one of GL_TEXTUREi, where i ranges from zero to the value of GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS minus one. The initial value is GL_TEXTURE0.

        // glUniform 함수를 사용하여 값을 할당하지 않음에도 불구하고 sampler2D 변수가 uniform으로 설정되는 것을 볼 수 있을 것이다.
        // glUniform1i 함수를 사용하여 실제로 텍스처 sampler에 위치 값을 할당하여 fragment shader에서 동시에 여러 텍스처들을 설정할 수 있습니다.
        // 이 텍스처의 위치는 흔히 texture unit이라고 알고 있는 것이며 기본 텍스처 유닛은 0입니다.
        // 이는 기본으로 활성화된 텍스처 유닛이므로 이전의 섹션에서는 위치 값을 할당할 필요가 없었습니다만 모든 그래픽 드라이버가 기본 텍스처 유닛을 할당하는 것은 아니라는 사실을 알고 있어야 합니다.

        // 텍스처 유닛의 주 목적은 shader에서 하나 이상의 텍스처를 사용할 수 있도록 해주는 것입니다.
        // sampler에 텍스처 유닛을 할당함으로써 해당 텍스처 유닛을 활성화하기만 하면 여러 텍스처들을 동시에 바인딩할 수 있습니다.
        // glBindTexture 함수와 마찬가지로 glActiveTexture 함수에 텍스처 유닛을 전달하여 호출함으로써 텍스처 유닛을 활성화할 수 있습니다.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0) // 텍스처를 바인딩하기 전에 먼저 텍스처 유닛을 활성화

        // Bind the texture to this unit.
        // 텍스처 유닛을 활성화한 후에 호출되는 glBindTexture 함수는 해당 텍스처를 현재 활성화된 텍스처 유닛에 바인딩합니다.
        // GL_TEXTURE0 텍스처 유닛은 항상 기본으로 활성화되므로 이전 예제에서 glBindTexture 함수를 사용할 때 어떠한 텍스처 유닛도 활성화 하지 않아도 되었습니다.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[1])
        // You are attaching the texture object ‘tex’ to the texture unit number 0.
        // You are telling OpenGL that the texture object ‘tex’ is a 2D texture, and you will refer to it (while it is bound) with GL_TEXTURE_2D.


        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        // glUniform — specify the value of a uniform variable for the current program object
        GLES20.glUniform1i(
            mTextureUniformHandle, // target location
            0 //
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    // 텍스쳐를 정의해주는 역할을 수행
    fun loadTexture(context: Context, resourceIds: IntArray, IdCounts: Int): IntArray {
        // GLuint myTexture;
        // glGenTextures(n, &myTextures); 1개만 들고옴
        // GLuint myTextures[32];
        // glGenTextures(32, myTextures); - 32: Specifies the number of texture names to be generated. / &myTextures - Specifies an array in which the generated texture names are stored.
        // textureHandle이 여기서의 myTexture이다

        // You have (for all intents and purposes) created a texture.
        // You have created a space that will hold all of the data associated with a texture. And the variable ‘tex’ now stores the reference to this texture.
        val textureHandle = IntArray(IdCounts)
        GLES20.glGenTextures(IdCounts, textureHandle, 0)

        if (textureHandle[0] != 0) { // 0이 아닌지의 의미??--------------------------------------------
            // Bitmap이란?
            // 안드로이드에서 이미지를 표현하기 위해 사용되는 녀석이 Bitmap입니다.
            // Bitmap와 같이 봐야될 녀석은 BtimapFactory클래스 인데 이름에서 알수 있듯이 "Factory" 공장입니다.
            // 바로 Bitmap를 만들어 주는 녀석이죠.
            // 안드로이드에서 사용자가 원하는 이미지를 코드상에 넣을려면 해당 이미지는 /res/drawable-xxxx/ 곳에 넣으시면 됩니다. 권장하는 확장자는 png이지만 jpg도됩니다.
            // 파일을 넣으실때 주의 하셔야 될것은 파일이름은 영어소문자, _(언더바) 만 사용가능하다는거 잊지마세요.
            val options = BitmapFactory.Options()

            // When this flag is set, if inDensity and inTargetDensity are not 0, the bitmap will be scaled to match inTargetDensity when loaded, rather than relying on the graphics system scaling it each time it is drawn to a Canvas.
            // inTargetDensity - The pixel density of the destination this bitmap will be drawn to
            options.inScaled = false // No pre-scaling ------------------------------- 이유?

            // Read in the resource
            for (i in resourceIds.indices) {
                // decodeResource - res 폴더에 저장된 녀석들을 Bitmap으로 만들때 사용합니다.
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceIds[i], options)

                // Bind to the texture in OpenGL
                // GL_TEXTURE_2D에 textureHandle[0]의 요소를 bind함
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[i])

                // set up mipmap - 텍스쳐 확대와 축소의 방법으로 mipmap적용하겠다고 선언
                // Texture Filtering과 Texture Wrapping(텍스쳐 포장)의 설정을 지정하는 것이 glTexParameteri의 역할
                // GL_NEAREST는 근접점 샘플링을, GL_LINEAR은 겹선형보간을 의미
                // GL_TEXTURE_MIN_FILTER는 텍스쳐 축소의 경우의 설정
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
                // GL_TEXTURE_MAG_FILTER는 텍스쳐 확대의 경우의 설정
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

                // Load the bitmap into the bound texture.
                // 텍스처 오브젝트를 실제 텍스쳐로 채우기 위해 GLTexImage2D를 호출
                // 이 때, GLTexImage2D는 텍스처 오브젝트 ID는 파라미터로 필요로 하지 않는다
                // 이는 일단 텍스쳐 오브젝트가 바인딩되면, 그 이후에 호출되는 함수들은 기본적으로 바인딩된 텍스처 오브젝트를 사용하기 때문이다
                // 이때 bitmap데이터는 openGL이 읽을수 있도록 디코딩된 상태여야 한다
                GLUtils.texImage2D(
                    GLES20.GL_TEXTURE_2D, // 첫 번째 파라미터는 텍스처 타겟을 지정합니다. 이를 GL_TEXTURE_2D로 설정한다는 것은 현재 GL_TEXTURE_2D로 바인딩된 텍스처 객체에 텍스처를 생성하겠다는 뜻입니다(그래서 GL_TEXTURE_1D나 GL_TEXTURE_3D로 바인딩된 객체에는 아무런 영향을 끼치지 않습니다)
                    0, // 우리가 생성하는 텍스처의 mipmap 레벨을 수동으로 지정하고 싶을 때 지정합니다. 하지만 우리는 베이스 레벨일 0로 남겨두겠습니다
                    bitmap, // 실제 이미지 데이터
                    0
                )
                // You are telling OpenGL to create image data, associate that image data with the texture that is currently bound to the active texture unit (GL_TEXTURE0) and is bound to the GL_TEXTURE_2D target.
                // When called immediately after our previous functions, that will refer to the texture object ‘tex’.
                // So this function will cause image data to be attached to the texture object ‘tex’.
                // But only because it is the texture currently bound to the current active texture unit and the GL_TEXTURE_2D target.

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                // Android에서 Java로 Bitmap을 다룰 경우 OOM(Out-Of-Memory)를 늘 신경쓰며 작업을 해야한다.
                // 아무리 기기에 램이 많아도 Java로 프로그래밍을 할 경우, 한 앱이 사용할 수있는 메모리의 크기는 제한되어있기 때문에, 조금이라도 큰 Bitmap을 처리할 경우 OOM을 피할 수는 없다.
                // 그래서 Bitmap을 처리할 때, 필요한 최소 사이즈로 불러와야 하며, 사용이 끝난 Bitmap은 즉시 즉시 Recycle을 해줘야 한다.
                bitmap.recycle()

                // Mipmap 설정정
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

            }
            // 텍스쳐 바인딩 해제
            // You are telling OpenGL that the currently bound texture object (aka: ‘tex’) is no longer bound to the current texture unit and GL_TEXTURE_2D target.
            // This means that if you call glTexImage again, it will not affect the texture object ‘tex’. Not unless you bind ‘tex’ again.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        }
        if (textureHandle[0] == 0) {
            throw RuntimeException("Error loading texture.")
        }
        return textureHandle
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