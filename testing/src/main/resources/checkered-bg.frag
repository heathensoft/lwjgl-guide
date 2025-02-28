#version 440
layout (location=0) out vec4 f_color;
in vec2 world_pos;
uniform vec2 u_map_size;
void main() {
    vec3 rgb;
    // checkered pattern
    float val = mod(floor(world_pos.x) + mod(floor(world_pos.y), 2.0), 2.0);
    if(val == 0) {
        rgb = vec3(0.75);
    }  else rgb = vec3(0.5);

    if(world_pos.x < 0 || world_pos.y < 0 ||
    world_pos.x > u_map_size.x || world_pos.y > u_map_size.y) {
        rgb *= 0.5;
    }

    rgb *= 0.66;
    f_color = vec4(rgb,1.0);
}