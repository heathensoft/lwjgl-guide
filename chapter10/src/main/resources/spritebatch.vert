#version 440

layout (location=0) in vec2 a_pos;
layout (location=1) in vec2 a_uv;
layout (location=2) in vec4 a_color;
layout (location=3) in float a_data;

out VS_TO_FS {
    vec4 color;
    vec2 uv;
    flat uint texture_slot;
} vs_out;

uniform mat4 u_combined;

void main() {
    vs_out.color = a_color;
    vs_out.color.a *= (255.0/254.0);
    vs_out.uv = a_uv;
    vs_out.texture_slot = floatBitsToUint(a_data) & 0xF;
    gl_Position = u_combined * vec4(a_pos,0.0,1.0);
}