#version 440

layout (location=0) out vec4 f_color;

uniform sampler2D u_texture;

in vec2 uv;

void main() {

    f_color = texture(u_texture,uv);
}