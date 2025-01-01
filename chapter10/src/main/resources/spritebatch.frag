#version 440

#define NO_TEXTUE 15
#define TEXTURE_SLOTS 15

layout (location=0) out vec4 f_color;

in VS_TO_FS {
    vec4 color;
    vec2 uv;
    flat uint texture_slot;
} fs_in;

uniform sampler2D[TEXTURE_SLOTS] u_textures;

void main() {
    vec4 color = fs_in.color;
    if(fs_in.texture_slot != NO_TEXTUE) {
        color *= texture(u_textures[fs_in.texture_slot],fs_in.uv);
    } f_color = color;
}