#version 150

// Mojang 1.7.10 assets/minecraft/shaders/program/blur.fsh, GLSL syntax adaptation only.
uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;
out vec4 fragColor;

void main() {
    vec4 blurred = vec4(0.0);
    float totalStrength = 0.0;
    for (float r = -Radius; r <= Radius; r += 1.0) {
        float strength = abs(1.0 - r / Radius);
        strength = strength * strength;
        totalStrength = totalStrength + strength;
        blurred = blurred + texture(DiffuseSampler, texCoord + oneTexel * r * BlurDir) * strength;
    }
    fragColor = vec4(blurred.rgb / totalStrength, texture(DiffuseSampler, texCoord).a);
}
