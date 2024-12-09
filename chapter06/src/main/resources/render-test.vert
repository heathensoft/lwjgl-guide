#version 440
layout (location = 0) in vec3 a_pos;
layout (location = 1) in vec3 a_color;

const vec2 resolution = vec2(1200.0,800.0);

out vec3 color;

void main() {
    color = a_color;
    vec2 position_xy = vec2(a_pos.xy);
    position_xy /= resolution;
    position_xy = position_xy * 2.0 - 1.0;
    gl_Position = vec4(position_xy, a_pos.z, 1.0);
}