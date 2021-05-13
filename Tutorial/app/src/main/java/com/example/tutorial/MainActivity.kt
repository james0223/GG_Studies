package com.example.tutorial

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
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// Activity 란? 사용자에게 UI가 있는 화면을 제공하는 app component
// 일반적으로 app 은 하나의 main activity 를 갖는데, 이것이 앱을 처음 실행했을 때 사용자에게 보여지는 activity
// 여러 activity 를 가질 수 있고, activity 들은 "back stack"이라는 스택에 저장된다. Last in First
class MainActivity : AppCompatActivity() {

    // GLSurfaceView 정의
    // late init 은 initialize 를 나중에 하겠다는 의미
    private lateinit var gLView: GLSurfaceView

    // override란?
    // 상위 클래스의 매소드를 재정의 하는 것
    // 메소드 이름은 물론 인자 갯수나 타입도 동일해야 한다
    // 주로 상위 클래스의 동작을 상속받은 하위클래스에서 메소드의 동작을 변경하기 위해 사용된다
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bundles are generally used for passing data between various Android activities.
        // It depends on you what type of values you want to pass, but bundles can hold all types of values and pass them to the new activity.
        super.onCreate(savedInstanceState)
        //그냥 onCreate override 하게 되면 텅 빈 onCreate 로 실행되기 때문에, Activity 클래스 내부 onCreate 의 기본적인 Activity 생성 코드를 실행해야 한다.
        //savedInstanceState: Activity 가 종료될 때 데이터를 저장할 수 있는 객체, Activity 가 종료되는 상황은 아주 다양합니다. 사용자가 뒤로 가기를 눌렀거나, activity 가 백그라운드에 있는데 시스템 메모리가 부족한 경우,
        //언어 설정 변경할 때, 화면이 가로/세로로 회전할 때, 폰트 크기나 폰트를 변경했을 때 등의 다양한 상황에서 activity 의 기존 데이터를 유지할 수 있게 해줍니다. 더 정확한 것은 activity 의 life-cycle 을 알면 쉬운데 시간이 길어지기 때문에 궁금하신 분들을 검색해보시길 바랍니다.

        //Main Activity 의 context 를 인자로 받는다.
        gLView = MyGLSurfaceView(this)

        // onCreate 에서는 activity 의 필수 구성 요소를 초기화해야하는데, 그 중 하나가 User Interface layout 이다.
        // setContentView 는 layout 내용을 파싱하여 뷰를 생성하고, 뷰에 정의된 속성을 설정한다.
        // 그 중 R(res 폴더).layout(R의 내부 클래스).activity_main(activity_main.xml 을 가리키는 ID)
        // setContentView(R.layout.activity_main)

        // User Interface layout 을 OpenGL ES 그래픽을 그릴 수 있는 GLSurfaceView 로 설정
        setContentView(gLView)
    }
}

//context 란?
//context 는 말그대로 application 이나 activity 의 현재 상태를 의미
//context 는 application 환경에 대한 전역적 정보에 접근하기 위한 인터페이스이다.
//abstract class 이며 실제 구현은 android system 에 의해 제공된다.
//주요 역할은 두가지로,
//application 에 특화된 resource, DB, class 에 접근할 수 있고,
//application 레벨의 작업(ex. activity 실행)을 수행하기 위한 API 를 호출할 수 있다.
//더 자세한 내용은 첨부 파일 참조. 읽어보면 안드로이드 시스템에 대한 이해가 훨씬 잘 될 것입니다. 권장!!

// It's the context of current state of the application/object.
// It lets newly-created objects understand what has been going on.
// Typically you call it to get information regarding another part of your program (activity and package/application).

//Android framework API 를 통해 OpenGL을 지원한다.
//Android framework 에는 OpenGL ES API 로 그래픽을 만들고 조작할 수 있는 두 가지 기본 클래스가 존재한다.
//첫번째가 아래 해당하는 GLSurfaceView, 두번째가 GLSurfaceView.Renderer

//1. GLSurfaceView 는 OpenGL API 를 호출하여 object 를 그리고 조작할 수 있는 view class 이다.
// renderer 를 부착해야 GLSurfaceView 에 object 를 그리고 렌더링할 수 있다.

class MyGLSurfaceView(context: Context): GLSurfaceView(context) {
    private var renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2) //create an OpenGL ES 2.0 context
        renderer = MyGLRenderer()
        setRenderer(renderer) //Set the Renderer for drawing on the GLSurfaceView
    }
}

//2. GLSurfaceView 에 그래픽을 그리는 데 필요한 method 가 정의되어 있는 render class 이다.
//총 3가지의 method 가 기본적으로 필요하다.
//1) onSurfaceCreated(): GLSurfaceView 를 만들 때 시스템에서 한 번 호출한다.
//2) OnDrawFrame(): GLSurfaceView 를 매 frame 마다 그릴 때 호출한다.
//3) onSurfaceChanged(): GLSurfaceView 의 크기 변경, 또는 기기 화면의 방향 변경 등의 GLSurfaceView 의 geometry 의 변경이 일어날 때 호출한다.
class MyGLRenderer : GLSurfaceView.Renderer {
    // Projection and camera views allow you to display drawn objects in a way that more closely resembles how you see physical objects with your eyes.

    // Projection - This transformation adjusts the coordinates of drawn objects based on the width and height of the GLSurfaceView where they are displayed.
    // Without this calculation, objects drawn by OpenGL ES are skewed by the unequal proportions of the view window.
    // A projection transformation typically only has to be calculated when the proportions of the OpenGL view are established or changed in the onSurfaceChanged() method of your renderer.
    // Define a projection
    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    // Initialize shapes
    // Before you do any drawing, you must initialize and load the shapes you plan to draw.
    // Unless the structure (the original coordinates) of the shapes you use in your program change during the course of execution, you should initialize them in the onSurfaceCreated() method of your renderer for memory and processing efficiency.
    private lateinit var myModel: TargetModel

    // EGLConfig import 위치 주의
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?){

        //color buffer 를 clear 할 때 사용할 색 지정
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // initialize a shape you wish to draw
        myModel = TargetModel()
    }

    override fun onDrawFrame(p0: GL10?) {
        // glClear method 로 clear 할 수 있는 buffer 에는 color, depth, stencil 가 존재한다.
        // 그 중 color buffer 를 clear 한다.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)


        // Camera View - This transformation adjusts the coordinates of drawn objects based on a virtual camera position.
        // It’s important to note that OpenGL ES does not define an actual camera object, but instead provides utility methods that simulate a camera by transforming the display of drawn objects.
        // A camera view transformation might be calculated only once when you establish your GLSurfaceView, or might change dynamically based on user actions or your application’s function.
        // Define a camera

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Draw shape
        myModel.draw(vPMatrix)
    }

    // The data for a projection transformation is calculated in the onSurfaceChanged() method of your GLSurfaceView.Renderer class
    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        // Viewport 를 설정한다. 즉 화면에 보이는 좌표계를 설정한다.
        // normalized device coordinates 에서 screen coordinates 로 x, y 를 affine transform 한다.
        // glViewport(x, y, width, height)
        // 여기서 width 와 height 는 화면의 가로, 세로 치수이다.

        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }
}

// OpenGL ES allows you to define drawn objects using coordinates in three-dimensional space.
// So, before you can draw a model, you must define its coordinates.
// In OpenGL, the typical way to do this is to define a vertex array of floating point numbers for the coordinates.
// Coordinates = 좌표 라고 해석하자

const val COORDS_PER_VERTEX = 3 // number of coordinates per vertex in this array - 3D 이미지이므로 각 꼭지점당 좌표가 (x , y, z) 형태이므로 3개의 coordinate를 갖게 되는 것

val triangleCoords = floatArrayOf( // in counterclockwise order:
    0.0f, 0.622008459f, 0.0f,      // top
    -0.5f, -0.311004243f, 0.0f,    // bottom left
    0.5f, -0.311004243f, 0.0f      // bottom right
)

// Drawing a defined shape using OpenGL ES 2.0 requires a significant amount of code, because you must provide a lot of details to the graphics rendering pipeline.
// Specifically, you must define the following:
//     Vertex Shader - OpenGL ES graphics code for rendering the vertices of a shape.
//     Fragment Shader - OpenGL ES code for rendering the face of a shape with colors or textures.
//     Program - An OpenGL ES object that contains the shaders you want to use for drawing one or more shapes.
// You need at least one vertex shader to draw a shape and one fragment shader to color that shape.
// These shaders must be compiled and then added to an OpenGL ES program, which is then used to draw the shape.

// A program object is an object to which shader objects can be attached.
// This provides a mechanism to specify the shader objects that will be linked to create a program.
// It also provides a means for checking the compatibility of the shaders that will be used to create a program (for instance, checking the compatibility between a vertex shader and a fragment shader).
// When no longer needed as part of a program object, shader objects can be detached.


// Shaders contain OpenGL Shading Language (GLSL) code that must be compiled prior to using it in the OpenGL ES environment.
// To compile this code, create a utility method in your renderer class:
fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
    // also가 선 실행되어 상세 사항을 추가하고 최종본이 return 되는 형식

    // glCreateShader creates an empty shader object and returns a non-zero value by which it can be referenced - The ID of the shader object
    return GLES20.glCreateShader(type).also { shader -> //lambda function - 'it' is named into shader

        // add the source code to the shader and compile it

        // glShaderSource sets the source code in shader to the source code in the array of strings specified by string.
        // Any source code previously stored in the shader object is completely replaced
        GLES20.glShaderSource(shader, shaderCode)

        // glCompileShader compiles the source code strings that have been stored in the shader object specified by shader
        // The compilation status will be stored as part of the shader object's state.
        // This value will be set to GL_TRUE if the shader was compiled without errors and is ready for use, and GL_FALSE otherwise
        GLES20.glCompileShader(shader)
    }
}

class TargetModel {

    // Apply projection and camera transformations
    // In order to use the combined projection and camera view transformation matrix shown in the previews sections, first add a matrix variable to the vertex shader previously defined in the MyModel class

    // 모든 object는 각각의 model matrix(World matrix)를 보유하고 view matrix와 projection matrix는 모든 오브젝트가 공유한다


    // Object의 vertex shader(정점 쉐이더) 코드 생성
    // vertex shader는 두 가지 종류의 입력을 받아들이는데, 하나는 정점 배열에 저장된 vertex별 attribute들로, 이는 position(위치), normal(노멀), texCoord(테스처 좌표)로 구성된다
    // 나머지 하나는 모든 정점에 공유되는 입력 데이터로, uniform이라고 칭한다
    private val vertexShaderCode = // 도형의 꼭짓점을 렌더링하는 OpenGL ES 그래픽 코드
        "uniform mat4 uMVPMatrix;" + // uniform데이터를 받기 위한 uMVPMatrix 생성
                "attribute vec4 vPosition;" + // 정점별 attribute 중 position값을 받기 위한 vPosition 생성
                "void main() {" +
                // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    private val fragmentShaderCode = // 색상 또는 질감으로 도형의 면을 렌더링하는 OpenGL ES 코드
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0

    // Set color with red, green, blue and alpha (opacity) values
    val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)


    // Buffer란?
    // 처리속도가 빠른 장치와 처리속도가 느린 장치사이의 속도 차이를 개선하여 빠른장치의 노는 시간을 줄여주는 (일효율을 높여주는) 역할을 해주는 중간 장치
    // 버퍼는 데이터 나열 이외에도 총 4가지의 중요한 정보를 관리한다.
    //
    //- 위치 (Position)
    //  버퍼에서 읽거나 쓸 다음 위치를 의미한다. 즉 버퍼를 읽을 때 시작되는 지점을 의미하고 시작점은 0이다.
    //
    //- 용량 (Capacity)
    //  버퍼가 보유할 수 있는 최대 요소의 수이다. 한번 설정이 되면 변경이 불가능 하기 떄문에 메소드를 통해 읽기만 가능하다.
    //
    //- 한도 (Limit)
    //  버퍼에서 접근할 수 있는 데이터의 마지막을 의미한다. 위에 있는 용량이 한도보다 더 크다면 한도 뒤에 데이터를 읽거나 쓸 수 없으므로 실질적 사용 용량은 이 한도로 결정 된다.
    //
    //- 표시 (Mark)
    //  버퍼에서 클라이언트에 제한된 인덱스를 의미한다. 만약 위치가 표시 이하로 설정된다면 표시는 삭제된다.

    // Two Methods of Buffer Access Data
    // put(): Put data into a buffer
    // get(): Get data from the buffer

    // Indirect buffers
    // When using indirect buffers (allocate()), data passes through the kernel address space and user address space, and there is a process of duplication between them, which results in low performance.
    // Buffers are allocated by allocate(), which is built in the memory of the JVM

    // Direct buffers
    // Cached data directly in memory, fast to fast, high performance
    // However, memory files are controlled by the system and the process is uncontrollable and unsafe.
    // It is recommended to allocate large, persistent buffers that are vulnerable to the local IO operation of the underlying system.
    // By allocateDirect() method, the buffer is built in physical memory.


    // 원래의 glsl 코드
    // GLuint abo - C++에서의 포인터값을 생성한다고 보면 댈듯

    // glGenBuffers(GLsizei n, &abo) - n개의 버퍼 오브젝트를 생성하여 abo 위치에 저장한다

    // glBindBuffer(GL_ARRAY_BUFFER, abo) - 생성된 버퍼 오브젝트를 GL_ARRAY_BUFFER에 바인드한다 (b = 3처럼 GL_ARRAY_BUFFER = abo 라고 생각해도 좋다)

    // glBufferData(GL_ARRAY_BUFFER, - 원래는 obj겠지만 여기서는 하드코딩으로 생성된 triangleCoords에 속한 삼각형의 vertex 데이터를 위에서 GL_ARRAY_BUFFER에 바인드한 버퍼에 담는다
    //     (GLsizei) objData.vertices.size() * sizeof(Vertex), - 버퍼의 크기를 설정한다 - 각 vertex의 coordinates(좌표값 수)에 전체 vertex의 수를 곱한 것
    //      objData.vertices.data(), GLSTATIC_DRAW) - 담을 데이터
    private var vertexBuffer: FloatBuffer =
        // For maximum efficiency, you write these coordinates into a ByteBuffer, that is passed into the OpenGL ES graphics pipeline for processing.
        ByteBuffer.allocateDirect(triangleCoords.size * 4).run {

            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates(float array) to the FloatBuffer
                put(triangleCoords)
                // set the buffer to read from the first coordinate
                position(0)
            }
        }
    // In order to draw your shape, you must compile the shader code, add them to a OpenGL ES program object and then link the program.
    // Do this in your drawn object’s constructor, so it is only done once.

    // Important Note: Compiling OpenGL ES shaders and linking programs is expensive in terms of CPU cycles and processing time, so you should avoid doing this more than once.
    // If you do not know the content of your shaders at runtime, you should build your code such that they only get created once and then cached for later use.

    // glCreateProgram creates an empty program object and returns a non-zero value by which it can be referenced. - ID of the program object
    // A program object is an object to which shader objects can be attached.
    // This provides a mechanism to specify the shader objects that will be linked to create a program.
    // It also provides a means for checking the compatibility of the shaders that will be used to create a program (for instance, checking the compatibility between a vertex shader and a fragment shader).
    // When no longer needed as part of a program object, shader objects can be detached.
    private var mProgram: Int

    init {

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        // 원래의 GLSL버전의 코드는 아래와 같다
        // GLuint mProgram = glCreateProgram()
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            // glAttachShader(mProgram, shader)
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            // glAttachShader(mProgram, shader)
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            // glLinkProgram(mProgram) - 여기서의 파라미터값은 Specifies the handle of the program object to be linked. 이다
            // If any shader objects of type GL_VERTEX_SHADER are attached to program, they will be used to create an executable that will run on the programmable vertex processor
            GLES20.glLinkProgram(it)
        }
    }

    // Drawing shapes with OpenGL ES requires that you specify several parameters to tell the rendering pipeline what you want to draw and how to draw it.
    // Since drawing options can vary by shape, it's a good idea to have your shape classes contain their own drawing logic.


    // Create a draw() method for drawing the shape.
    // This code sets the position and color values to the shape’s vertex shader and fragment shader, and then executes the drawing function.
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexCount: Int = triangleCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw() {

        // Add program to OpenGL ES environment
        // glUseProgram installs the program object specified by program as part of current rendering state.
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member

        // handle이란?
        // 운영체제 입장에서는 응용프로그램에게 여러가지 서비스나 정보를 전달해주는 역활도 해야하지만 반대로 응용프로그램으로부터 자신을 보호해야하는 기능도 있어야 합니다.
        // 운영체제는 자신이 관리하는 자원이나 정보를 보호하기 위해서 자신이 관리하는 자원이나 정보의 실질적인 주소를 응용프로그램에게 알려주지 않고 그것을 암시하는 값만 전달하는 방식을 사용하는데 이것이 바로 핸들(handle)입니다.

        // glGetAttribLocation queries the previously linked program object specified by program for the attribute variable specified by name and returns the **index** of the generic vertex attribute that is bound to that attribute variable.
        // If name is a matrix attribute variable, the index of the first column of the matrix is returned.
        // If the named attribute variable is not an active attribute in the specified program object or if name starts with the reserved prefix "gl_", a value of -1 is returned.
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the object vertices
            // To enable and disable a generic vertex attribute array, call glEnableVertexAttribArray and glDisableVertexAttribArray with index.
            // If enabled, the generic vertex attribute array is used when glDrawArrays, glDrawArraysInstanced, glDrawElements, glDrawElementsIntanced, or glDrawRangeElements is called.
            GLES20.glEnableVertexAttribArray(it)


            // Prepare the triangle coordinate data
            // glVertexAttribPointer and glVertexAttribIPointer specify the location and data format of the array of generic vertex attributes at index 'index' to use when rendering.
            // size specifies the number of components per attribute and must be 1, 2, 3 or 4.
            // type specifies the data type of each component, and stride specifies the byte stride from one attribute to the next, allowing vertices and attributes to be packed into a single array or stored in separate arrays.
            GLES20.glVertexAttribPointer(
                it, // GLuint index
                COORDS_PER_VERTEX, // GLint size
                GLES20.GL_FLOAT, // GLenum type,
                false, // GLboolean normalized,
                vertexStride, // GLsizei stride,
                vertexBuffer // const void * pointer);
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }

    // Overloaded draw function
    fun draw(mvpMatrix: FloatArray) { // pass in the calculated transformation matrix

        // init부분에서 이미 link까지의 절차를 마쳐두었으므로 use만 하면 된다
        // glUseProgram은 파라미터로 받는 프로그램 오브젝트를 렌더링에 사용하겠다는 의미이다
        GLES20.glUseProgram(mProgram)

        // glGetAttribLocation(GLuint mProgram, const GLchar *varName);
        // glGetAttribLocation queries the previously linked program object specified by mProgram for the attribute variable specified by varName and returns the **index** of the generic vertex attribute that is bound to that attribute variable.
        // If name is a matrix attribute variable, the index of the **first column** of the matrix is returned
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // GlEnableVertexAttribArray(position)는 position에 해당하는 값을 활성화시킨다
            // 여기서는 glGetAttribLocation으로 불러와진 vPosition vector attribute의 첫 인자를 가리키게 된다
            GLES20.glEnableVertexAttribArray(it)

            // 각 postion에 대한 자세한 정보를 제공한다, 즉 GL프로그램의 중요한 역할 중 하나인 vertex shader가 attribute와 uniform을 사용할 수 있도록 넘겨주고 그 구조를 설명해주는 것
            GLES20.glVertexAttribPointer(
                it, // GLuint index - 원래는 position, normal, texture coordinates값을 받는데 여기선 position 뿐이므로 index값은 하나 뿐이다
                COORDS_PER_VERTEX, // GLint size - 각 정점이 몇개의 좌표(coordinates)로 구성되어있는지 말해준다
                GLES20.GL_FLOAT, // GLenum type, - 각 정점이 어떤 데이터타입으로 설정되어있는지 말해준다
                false, // GLboolean normalized, - normalize되어있는지 말해준다
                vertexStride, // GLsizei stride, - Buffer에는 position, normal, texture 구분 없이 데이터가 일렬로 정렬되어 있으므로, 가령 position을 읽고 다음 position을 읽으려면 몇 개의 인자를 건너뛰어야 하는지를 말해주는 것
                vertexBuffer // const void * pointer); - offset(in bytes) to the first occurrence of the attribute
            )

            // get handle to shape's transformation matrix
            // mvpMatrix를 매 프레임마다 정점 쉐이더 유니폼인 uMVPMatrix에 할당해야 하는데 이를 위해 uMVPMatrix의 위치를 알아낸다
            vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

            // Pass the projection and view transformation to the shader
            // 4x4 매트릭스이므로 glUniformMatrix4fv를 사용하여 값을 할당한다
            GLES20.glUniformMatrix4fv(
                vPMatrixHandle, // 값을 할당할 uniform variable의 위치값
                1, // the number of matrices to be modified
                false, // GL_boolean transpose - Must be False!
                mvpMatrix, // pointer to an array of 16 GL float values
                0 // offset값
            )

            // 폴리곤 메시를 그리는 명령 - DrawCall

            // Draw the triangle
            // 메시가 인덱스 없이 표현되었다면, 즉 정점 배열로만 정의되었다면 glDrawArrays를 호출하고
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLES, // 삼각형 메시를 사용하여 그릴 것이므로 GL_TRIANGLES
                0, // Start index in the vertex array
                vertexCount // the number of vertices to draw
            )

            // 메시가 인덱스가 존재한다면 glDrawElements를 호출한다
            // glDrawElements()

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }
}

class ObjCube2(context: Context){

    private val vertexShaderCode = //꼭짓점 셰이더 - 도형의 꼭짓점을 렌더링하는 OpenGL ES 그래픽 코드
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    private var vPMatrixHandle: Int = 0

    private val fragmentShaderCode =//조각 셰이더 - 색상 또는 질감으로 도형의 면을 렌더링하는 OpenGL ES 코드
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    // Set color with red, green, blue and alpha (opacity) values
    val color = floatArrayOf(1.0f, 0.980392f, 0.980392f, 0.3f)

    //프로그램 - 하나 이상의 도형을 그리는 데 사용할 셰이더가 포함된 OpenGL ES 객체
    private var mProgram: Int

    private var vertices = mutableListOf<Float>()
    private var faces = mutableListOf<Short>()
    private lateinit var verticesBuffer: FloatBuffer
    private lateinit var facesBuffer: ShortBuffer

    init {
        try {
            val scanner = Scanner(context.assets.open("cube.obj"))
            Log.d("check1","ok")
            //fill the vertices & faces string list
            while (scanner.hasNextLine()){
                val line = scanner.nextLine()
                if (line.startsWith("v ")){
                    val vertex = line.split(" ")
                    val x = vertex[1].toFloat()
                    val y = vertex[2].toFloat()
                    val z = vertex[3].toFloat()
                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z) //vertices 는 한 줄에 3개, 총 3*8=24
                }
                else if (line.startsWith("f ")) {
                    val face = line.split(" ")
                    val vertex1 = face[1].split("/")[0].toShort()
                    val vertex2 = face[2].split("/")[0].toShort()
                    val vertex3 = face[3].split("/")[0].toShort()
                    val vertex4 = face[4].split("/")[0].toShort()
                    faces.add(vertex1)
                    faces.add(vertex2)
                    faces.add(vertex3)
                    faces.add(vertex3)
                    faces.add(vertex4)
                    faces.add(vertex1) //faces 는 한 줄에 6개, 총 6*6=36
                }
            }
            //initialize vertices buffer
            verticesBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    for (vertex in vertices){
                        put(vertex)
                    }
                    position(0)
                }
            }

            //initialize faces buffer
            facesBuffer = ByteBuffer.allocateDirect(faces.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    for (face in faces){
                        put((face-1).toShort())
                    }
                    position(0)
                }
            }
        } catch (e: Exception){
            Log.e("file_read", e.message.toString())
        }

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)//도형을 그리려면 하나 이상의 꼭짓점 셰이더가 있어야 하고 도형의 색상을 지정하려면 하나의 조각 셰이더가 있어야 함.
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)
            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX = 3

    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexCount: Int = vertices.size
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray){
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                verticesBuffer
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // get handle to shape's transformation matrix
            vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)


            // Draw the triangle
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, faces.size, GLES20.GL_UNSIGNED_SHORT, facesBuffer)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
