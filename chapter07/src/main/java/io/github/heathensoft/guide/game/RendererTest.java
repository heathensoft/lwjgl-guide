package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.*;
import io.github.heathensoft.guide.core.gfx.*;
import io.github.heathensoft.guide.utils.Resources;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL42.glTexStorage2D;

/**
 * Frederik Dahl 12/5/2024
 */
public class RendererTest implements Disposable {

    private final int vertex_attrib_array;
    private final int vertex_buffer_object;
    private final ShaderProgram shader_program;
    private final Texture tex;


    public RendererTest() throws Exception {

        // ***********************************************************************************************

        // SHADER

        // Loading shader source code files from the project "resources folder"
        String vert_shader_source = Resources.asString("render-test.vert");
        String frag_shader_source = Resources.asString("render-test.frag");

        // Uploading the source code strings to the gpu then compile the code
        // Each shader is compiled separately.
        Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
        Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
        shader_program = new ShaderProgram(vert_shader,frag_shader);
        shader_program.detachShaders(true);

        // Upload the "u_texture" uniform (using texture slot 0) to the shader program.
        ShaderProgram.useProgram(shader_program);
        ShaderProgram.setUniform("u_texture",0);

        // ***********************************************************************************************

        // TEXTURE

        // Load a png file from the resources folder
        ByteBuffer png = Resources.readToBuffer("texture-test.png",512);
        Bitmap bitmap = new Bitmap(png,false); // decode the png to a bitmap
        tex = bitmap.asTexture(); // create texture from bitmap
        tex.filterNearest(); // sample nearest pixel (as opposed to linear filtering)
        tex.textureRepeat(); // UV repeats
        bitmap.dispose(); // free the bitmap


        // ***********************************************************************************************

        // VERTICES

        Resolution app_res = Engine.get().window().gameResolution();
        final float x1 = app_res.width() / 2f - bitmap.width() / 2f;
        final float y1 = app_res.height() / 2f - bitmap.height() / 2f;
        final float x2 = x1 + bitmap.width();
        final float y2 = y1 + bitmap.height();
        final float[] vertices = new float[] {

                /*{ V0 }*/x1, y2, 0,/*position (xyz)*/0.0f, 0.0f,/*texture coordinate (uv)*/
                /*{ V1 }*/x1, y1, 0,/*position (xyz)*/0.0f, 1.0f,/*texture coordinate (uv)*/
                /*{ V2 }*/x2, y2, 0,/*position (xyz)*/1.0f, 0.0f,/*texture coordinate (uv)*/
                /*{ V3 }*/x2, y2, 0,/*position (xyz)*/1.0f, 0.0f,/*texture coordinate (uv)*/
                /*{ V4 }*/x1, y1, 0,/*position (xyz)*/0.0f, 1.0f,/*texture coordinate (uv)*/
                /*{ V5 }*/x2, y1, 0,/*position (xyz)*/1.0f, 1.0f,/*texture coordinate (uv)*/

        };
        vertex_attrib_array = glGenVertexArrays();
        vertex_buffer_object = glGenBuffers();
        glBindVertexArray(vertex_attrib_array);
        glBindBuffer(GL_ARRAY_BUFFER,vertex_buffer_object);
        glBufferData(GL_ARRAY_BUFFER,vertices,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,5 * Float.BYTES,0);
        glVertexAttribPointer(1,2,GL_FLOAT,false,5 * Float.BYTES,3 * Float.BYTES);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void draw() {
        ShaderProgram.useProgram(shader_program);
        glBindVertexArray(vertex_attrib_array);
        glDrawArrays(GL_TRIANGLES,0,6);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteVertexArrays(vertex_attrib_array);
        glDeleteBuffers(vertex_buffer_object);
        Disposable.dispose(tex);
    }


}
