//P. vertex.glsl code 작성
uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
void main() {
    gl_Position = uMVPMatrix * vPosition;
}