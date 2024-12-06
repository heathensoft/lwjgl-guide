package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.utils.Disposable;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * Frederik Dahl 12/5/2024
 */
public class RendererTest implements Disposable {

    private final int vertex_attrib_array;
    private final int vertex_buffer_object;
    private final int shader_program;

    private final static String vertex_shader_source = """
            #version 440
            layout (location = 0) in vec3 a_pos;
            layout (location = 1) in vec3 a_color;
            out vec3 color;
            void main() {
                color = a_color;
                gl_Position = vec4(a_pos,1.0);
            }""";

    private final static String fragment_shader_source = """
            #version 440
            layout (location=0) out vec4 f_color;
            in vec3 color;
            void main() {
                f_color = vec4(color,1.0);
            }""";

    public RendererTest() throws Exception {

        // create an empty shader program
        shader_program = glCreateProgram();
        // create vertex shader object
        int vertex_shader = glCreateShader(GL_VERTEX_SHADER);
        // copy the shader source characters into the shader object
        glShaderSource(vertex_shader,vertex_shader_source);
        // compile the vertex shader
        glCompileShader(vertex_shader);
        int compile_status = glGetShaderi(vertex_shader,GL_COMPILE_STATUS);
        if (compile_status == GL_FALSE) {
            String error_message = glGetShaderInfoLog(vertex_shader);
            glDeleteShader(vertex_shader);
            glDeleteProgram(shader_program);
            throw new Exception(error_message);
        } // attach the vertex shader to the program
        glAttachShader(shader_program,vertex_shader);

        // create fragment shader object
        int fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
        // copy the shader source characters into the shader object
        glShaderSource(fragment_shader,fragment_shader_source);
        // compile the fragment shader
        glCompileShader(fragment_shader);
        compile_status = glGetShaderi(fragment_shader,GL_COMPILE_STATUS);
        if (compile_status == GL_FALSE) {
            String error_message = glGetShaderInfoLog(fragment_shader);
            glDeleteShader(vertex_shader);
            glDeleteShader(fragment_shader);
            glDeleteProgram(shader_program);
            throw new Exception(error_message);
        } // attach the fragment shader to the program
        glAttachShader(shader_program,fragment_shader);

        // link the program
        // each attached shaders will be used to create an executable
        // that will run for that stage in the graphics pipeline
        glLinkProgram(shader_program);

        // Once the shaders have been linked, there is no need to keep them around
        glDetachShader(shader_program,vertex_shader);
        glDetachShader(shader_program,fragment_shader);
        glDeleteShader(vertex_shader);
        glDeleteShader(fragment_shader);
        int link_status = glGetProgrami(shader_program,GL_LINK_STATUS);
        if (link_status == GL_FALSE) {
            String error_message = glGetProgramInfoLog(shader_program);
            glDeleteProgram(shader_program);
            throw new Exception(error_message);
        }
        // checks to see whether the executables contained in
        // program can execute given the current OpenGL state
        glValidateProgram(shader_program);
        int validate_status = glGetProgrami(shader_program,GL_VALIDATE_STATUS);
        if (validate_status == GL_FALSE) {
            String error_message = glGetProgramInfoLog(shader_program);
            glDeleteProgram(shader_program);
            throw new Exception(error_message);
        }

        // ***********************************************************************************************

        float[] vertices = new float[] {
                /*{ V0 }*/-1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
                /*{ V1 }*/-1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
                /*{ V2 }*/ 1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
                /*{ V3 }*/ 1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
                /*{ V4 }*/-1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
                /*{ V5 }*/ 1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
        };
        vertex_attrib_array = glGenVertexArrays();
        vertex_buffer_object = glGenBuffers();
        glBindVertexArray(vertex_attrib_array);
        glBindBuffer(GL_ARRAY_BUFFER,vertex_buffer_object);
        glBufferData(GL_ARRAY_BUFFER,vertices,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,6 * Float.BYTES,0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,6 * Float.BYTES,3 * Float.BYTES);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void draw() {
        glUseProgram(shader_program);
        glBindVertexArray(vertex_attrib_array);
        glDrawArrays(GL_TRIANGLES,0,6);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void dispose() {
        glDeleteVertexArrays(vertex_attrib_array);
        glDeleteBuffers(vertex_buffer_object);
        glUseProgram(GL_NONE);
        glDeleteProgram(shader_program);
    }


}
