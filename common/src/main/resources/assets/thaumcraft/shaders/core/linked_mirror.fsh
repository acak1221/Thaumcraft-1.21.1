#version 150
uniform sampler2D Sampler0;
in vec3 projectedTexture;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    fragColor = textureProj(Sampler0, projectedTexture) * vertexColor;
}
