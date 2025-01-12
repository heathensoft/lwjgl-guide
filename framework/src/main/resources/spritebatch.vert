#version 440

#define FAR 2.0
#define NEAR 1.0
#define Z_LAYERS 16.0

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

struct Bits {
    // LSB
    //---------------------------- 0
    uint tex_slot;  // 4 bit value
    uint z_layer;   // 4 bit value
    //---------------------------- 8

    //---------------------------- 32
    // MSB
};

Bits unpackFloat(float float_data) {
    Bits result;
    uint integer = floatBitsToUint(float_data);
    result.tex_slot = integer & 0x0F;
    result.z_layer = (integer >> 4) & 0x0F;
    return result;
}

void main() {
    // alpha correction due to packing RGBA to a single float value in java.
    vs_out.color = a_color;
    vs_out.color.a *= (255.0/254.0);
    // directly pass the uv coordinates to the fragment shader
    vs_out.uv = a_uv;

    // unpack data bits
    Bits data = unpackFloat(a_data);

    // pass on the texture slot to the fragment shader
    vs_out.texture_slot = data.tex_slot;

    // calculate the z value based on the z layer
    // layer 0 is near, layer 15 is far
    float z = -float(data.z_layer);
    z /= ((FAR - NEAR) * (Z_LAYERS - 1.0));
    // make sure far-layer won't get clipped
    // due to rounding errors
    z *= 0.9999999;
    // atp. z will be a value between 0.0 (near) and -1.0 (far)


    gl_Position = u_combined * vec4(a_pos,z,1.0);
}



