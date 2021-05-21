uniform mat4 vMatrix;
uniform mat4 pMatrix;
attribute vec4 vPosition;

void main() {
    gl_Position = pMatrix * vMatrix * vPosition;
}