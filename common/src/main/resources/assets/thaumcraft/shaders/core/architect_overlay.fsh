#version 150

// Original REHWandHandler.drawOverlayBlock: GL_TEXTURE_ENV_MODE is GL_ADD while the area is drawn,
// so the colour of the block is added to the rune texture and the alphas are multiplied; the
// alpha test is glAlphaFunc(GL_GREATER, 1/255).
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    vec4 color = vec4(min(texel.rgb + vertexColor.rgb, vec3(1.0)), texel.a * vertexColor.a);
    if (color.a <= 0.003921569) discard;
    fragColor = color * ColorModulator;
}
