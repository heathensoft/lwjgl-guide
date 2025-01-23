#version 440
layout (location = 0) in vec2 a_pos; // ndc
uniform mat4 u_combined_inv;
out vec2 world_pos;
void main() {
    // convert normalized device coordinates to world coordinates
    // and pass it on to the fragment shader
    world_pos = (u_combined_inv * vec4(a_pos,0.0,1.0)).xy;
    gl_Position = vec4(a_pos,0.0,1.0);
}