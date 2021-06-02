precision mediump float;

uniform sampler2D textureDiff;
uniform sampler2D textureDissolve;

uniform vec3 matSpec, matAmbi, matEmit;
uniform float matSh;
uniform vec3 srcDiffL, srcSpecL, srcAmbiL;
uniform vec3 srcDiffR, srcSpecR, srcAmbiR;
uniform float threshold;

varying vec3 v_normal;
varying vec2 v_texCoord;
varying vec3 v_view, v_lightL, v_lightR;
varying float v_attL, v_attR;

void main() {
    //-------------------------------------------------------
    // Problem 2
    // Put a texture on the teapot.
    // Change the code below to get the texture value.

    vec3 color = vec3(1.0, 1.0, 1.0);

    //-------------------------------------------------------

    //-------------------------------------------------------
    // Problem 3
    // Implement the phong shader using 2 color point lights.

    // diffuse term
    // vec3 matDiff = ;
    // vec3 diffL = ;
    // vec3 diffR = ;
    // vec3 diff = ;

    // specular term
    // vec3 reflL = ;
    // vec3 reflR = ;
    // vec3 specL = ;
    // vec3 specR = ;
    // vec3 spec = ;

    // ambient term
    // vec3 ambiL = ;
    // vec3 ambiR = ;
    // vec3 ambi = ;

    // color = ;

    //-------------------------------------------------------

    float alpha = 1.0;
    //-------------------------------------------------------
    // Problem 4
    // Implement the alpha blending using an extra dissolve texture.


    //-------------------------------------------------------

    // final output color with alpha
    gl_FragColor = vec4(color, alpha);
}