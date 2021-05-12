// 1. uniform
//rendering하는 동안 변하지 않는 변수이다.
//OpenGL application으로 부터 값을 전달받을 수 있다.
//vertex shader와 fragment shader에서 모두 공유하여 사용하는 것이 가능하지만, 값을 읽는 것만 가능하다.
//예) light position, light color

//2. atttibute
//uniform과 마찬가지로 OpenGL application으로 부터 값을 전달받을 수 있다.
//vertex shader에서만 사용이 가능하다.
//vertex별로 다른 값을 가질 수 있다.
//uniform과 마찬가지로 값을 읽는 것만 가능하다.
//예) vertex position, vertex normal

//3. varying
//vertex shader에서 fragment shader로 값을 전달하는 경우에 사용한다.
//vertex shader와 fragment shader에 모두 선언하여 사용한다.
//varying type 변수에 vertex shader에서 각 vertex 별로 값을 기록하면, fragment shader에서는 perspective correctly interpolated value를 얻게된다.
//vertex shader에서는 값을 읽고 쓰는 것이 모두 가능하지만, fragment shader에서는 값을 읽는 것만 가능하다.
//예) texture coordinates, normal vectors for per fragment Phong shading, vertex color, light value for Gouraud shading
uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate; // 이미지 텍스쳐는 2차원이기 때문에 vec2로 생성

void main() {
    v_TexCoordinate = a_TexCoordinate;
    gl_Position = uMVPMatrix* vPosition;
}
