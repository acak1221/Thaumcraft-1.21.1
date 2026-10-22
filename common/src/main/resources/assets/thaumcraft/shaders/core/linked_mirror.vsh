#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 PortalTextureMat;
uniform float PortalQ;
out vec3 projectedTexture;
out vec4 vertexColor;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec4 generated = PortalTextureMat * vec4(UV0, 1.0, PortalQ);
    projectedTexture = vec3(generated.xy, generated.w);
    vertexColor = Color;
}
