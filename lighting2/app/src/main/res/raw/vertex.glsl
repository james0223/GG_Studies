uniform mat4 vMatrix;
uniform mat4 pMatrix;
attribute vec4 aPosition;
attribute vec4 aNormal;
//
//uniform vec4 lightPosition;
//uniform vec4 aColor;
//
//varying vec4 vColor;
varying vec4 vPosition;
varying vec4 vNormal;

void main() {
//    float ambient = 0.1;
//
//    float distance = length(lightPosition - vPosition);
//    vec4 lightVector = normalize(lightPosition - vPosition);
//    float diffuse = max(dot(aNormal, lightVector), ambient);
//    diffuse = diffuse * (1.0/(1.0+(0.15 * distance * distance)));
//
//    vColor = aColor * diffuse;
    vNormal = aNormal;
    vPosition = aPosition;
    gl_Position = pMatrix * vMatrix * aPosition;
}
