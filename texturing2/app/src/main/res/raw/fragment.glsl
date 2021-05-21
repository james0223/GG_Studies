precision mediump float;
uniform sampler2D u_Texture1;
uniform sampler2D u_Texture2;
varying vec2 v_TexCoordinate;
varying vec4 vColor;

void main() {
    gl_FragColor = vColor * mix(texture2D(u_Texture1, v_TexCoordinate), texture2D(u_Texture2, v_TexCoordinate), 0.5);
}