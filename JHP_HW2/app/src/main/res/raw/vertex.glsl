uniform mat4 uMVPMatrix;

attribute vec4 vPosition;
attribute vec3 normal;
attribute vec2 texCoord;

uniform mat4 worldMat;
uniform mat3 tpInvWorldMat;
uniform vec3 eyePos, lightPosL, lightPosR;
uniform vec3 lightAttL, lightAttR;

varying vec3 v_normal;
varying vec2 v_texCoord;
varying vec3 v_view, v_lightL, v_lightR;
varying float v_attL, v_attR;

void main() {
    // world-space position
    vec3 worldPos = (worldMat * vPosition).xyz;

    //-------------------------------------------------------
    // Problem 3
    // Implement the phong shader using 2 color point lights.

    // world-space vertex normal
    // v_normal = transpose(inverse(worldMat)) * normal;

    // view vector
    // v_view = ;

    // light vectors
    // v_lightL = ;
    // v_lightR = ;

    // attenuations
    // float distL = ;
    // float distR = ;
    // v_attL = ;
    // v_attR = ;

    //-------------------------------------------------------

    // texture coordinates
    v_texCoord = texCoord;

    // clip-space position
    gl_Position = uMVPMatrix * vec4(worldPos, 1.0);
}