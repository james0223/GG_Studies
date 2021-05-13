uniform mat4 vMatrix;
uniform mat4 pMatrix;
attribute vec4 vPosition;

attribute vec4 aColor;
varying vec4 vColor;

void main() {
    vColor = aColor;
    gl_Position = pMatrix * vMatrix * vPosition;
}
