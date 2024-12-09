package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.Engine;
import io.github.heathensoft.guide.core.Shader;
import io.github.heathensoft.guide.core.ShaderProgram;
import io.github.heathensoft.guide.utils.Disposable;
import io.github.heathensoft.guide.utils.Resources;

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
    private final ShaderProgram shader_program;


    public RendererTest() throws Exception {

        // Loading shader source code files from the project "resources folder"
        String vert_shader_source = Resources.asString("render-test.vert");
        String frag_shader_source = Resources.asString("render-test.frag");

        // Uploading the source code strings to the gpu then compile the code
        // Each shader is compiled separately.
        Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
        Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
        shader_program = new ShaderProgram(vert_shader,frag_shader);
        shader_program.detachShaders(true);

        // ***********************************************************************************************

        float[] vertices = new float[] {

                /*{ V0 }*/0   , 800, 0,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
                /*{ V1 }*/0   ,0   , 0,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
                /*{ V2 }*/1200, 800, 0,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
                /*{ V3 }*/1200, 800, 0,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
                /*{ V4 }*/0   , 0  , 0,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
                /*{ V5 }*/1200, 0  , 0,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/

                /*{ V0 }*/800 , 600, 0,/*position (xyz)*/(193 / 255f), (112 / 255f), (31 / 255f),/*color (rgb)*/
                /*{ V1 }*/800 , 400, 0,/*position (xyz)*/(193 / 255f), (112 / 255f), (31 / 255f),/*color (rgb)*/
                /*{ V2 }*/1000, 600, 0,/*position (xyz)*/(193 / 255f), (112 / 255f), (31 / 255f),/*color (rgb)*/
                /*{ V3 }*/1000, 600, 0,/*position (xyz)*/(193 / 255f), (112 / 255f), (31 / 255f),/*color (rgb)*/
                /*{ V4 }*/800 , 400, 0,/*position (xyz)*/(193 / 255f), (112 / 255f), (31 / 255f),/*color (rgb)*/
                /*{ V5 }*/1000, 400, 0,/*position (xyz)*/(193 / 255f), (112 / 255f), (31 / 255f),/*color (rgb)*/

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
        ShaderProgram.useProgram(shader_program);
        ShaderProgram.setUniform("u_time",
                (float)Engine.get().time().runTimeSeconds());
        glBindVertexArray(vertex_attrib_array);
        glDrawArrays(GL_TRIANGLES,0,12);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteVertexArrays(vertex_attrib_array);
        glDeleteBuffers(vertex_buffer_object);
    }


}
