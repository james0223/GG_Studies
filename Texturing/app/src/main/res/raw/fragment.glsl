precision mediump float;
uniform vec4 vColor;
uniform sampler2D u_Texture; // fragment shader의 uniform의 한 종류인 texture을 받아올 인자

varying vec2 v_TexCoordinate; // vertex shader가 보내주는 texture coordinates를 받기 위해 varying 사용

void main() {
    // texture(colorMap, v_texCoord) 내장함수는 v_texCoord를 사용하여 colorMap을 필터링하여 fragColor를 결정한다
    // Colormap을 필터링하는 방식은 glTexParameteri를 통해 지정된다
    // 여기서 생성된 fragColor은 GPU 파이프라인의 다음 단계인 출력병합기로 전달된다
    gl_FragColor = vColor * texture2D(u_Texture, v_TexCoordinate);
}