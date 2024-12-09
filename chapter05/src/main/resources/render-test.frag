#version 440

layout (location=0) out vec4 f_color;

in vec3 color;
uniform float u_time;

void main() {
    float r = (sin(u_time) + 1.0) / 2.0;
    float g = color.g;
    float b = color.b;
    float a = 1.0;
    f_color = vec4(r,g,b,a);
}