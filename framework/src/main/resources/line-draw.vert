#version 440

layout (location=0) in vec2 a_pos;
layout (location=1) in vec4 a_color;

uniform mat4 u_combined;

out vec4 color;

void main() {
    color = a_color;
    color.a *= (255.0/254.0);
    gl_Position = u_combined * vec4(a_pos,0.0,1.0);
}