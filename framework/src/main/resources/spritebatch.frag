#version 440

//#define ALPHA_DISCARD_THRESHOULD 0.1
//#define DEBUG_DEPTH false
#define NO_TEXTUE 15
#define TEXTURE_SLOTS 15

layout (location=0) out vec4 f_color;

in VS_TO_FS {
    vec4 color;
    vec2 uv;
    flat uint texture_slot;
    bool pixel_art;
} fs_in;

uniform sampler2D[TEXTURE_SLOTS] u_textures;
//uniform int u_depth_enabled;


void main() {

    vec4 color = fs_in.color;

    // if depth is enabled we discard (ignore) transparent fragments.
    //if(u_depth_enabled != 0) {
    //    if(color.a < ALPHA_DISCARD_THRESHOULD) {
    //        discard;
    //    }
    //}

    // sample from texture
    if(fs_in.texture_slot != NO_TEXTUE) {
        if(fs_in.pixel_art) {
            vec2 texture_size = vec2(textureSize(u_textures[fs_in.texture_slot],0).xy);
            vec2 pix = fs_in.uv * texture_size;
            pix = floor(pix) + min(fract(pix) / fwidth(pix), 1.0) - 0.5;
            color *= texture(u_textures[fs_in.texture_slot],pix / texture_size);
        } else color *= texture(u_textures[fs_in.texture_slot],fs_in.uv);
    }

    // draw depth value to color buffer
    //if(DEBUG_DEPTH) {
    //    color.r = gl_FragCoord.z;
    //    color.g = gl_FragCoord.z;
    //    color.b = gl_FragCoord.z;
    //}

    f_color = color;
}