precision mediump float;
uniform vec4 vColor;
uniform vec4 lightPosition;

//varying vec4 vColor;
varying vec4 vPosition;
varying vec4 vNormal;

void main() {
    vec4 lightcolor = vec4(1.0);

    float ambientStrength = 0.1;
    vec4 ambient = ambientStrength * lightcolor;

    vec4 norm = normalize(vNormal);
    vec4 lightVector = normalize(lightPosition - vPosition);
    float diff = max(dot(norm, lightVector), 0.0);
    vec4 diffuse = diff * lightcolor;

    gl_FragColor = (ambient + diffuse) * vColor;
}
