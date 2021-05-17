uniform mat4 vMatrix;
uniform mat4 pMatrix;
attribute vec4 vPosition;

uniform vec4 lightPosition;
attribute vec4 aNormal;

attribute vec4 aColor;
varying vec4 vColor;

void main() {
    float distance = length(lightPosition - vPosition);
    vec4 lightVector = normalize(lightPosition - vPosition);
    float diffuse = max(dot(aNormal, lightVector), 0.1);
    diffuse = diffuse * (1.0/(1.0+(0.25 * distance * distance)));
    vColor = aColor * diffuse;
    gl_Position = pMatrix * vMatrix * vPosition;
}
