uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec2 a_TexCoordinate;
attribute vec4 aColor;

varying vec2 v_TexCoordinate;
varying vec4 vColor;

void main() {
    vColor = aColor;
    v_TexCoordinate = a_TexCoordinate;
    gl_Position = uMVPMatrix* vPosition;
}
