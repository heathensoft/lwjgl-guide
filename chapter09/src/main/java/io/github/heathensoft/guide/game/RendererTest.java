package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.*;
import io.github.heathensoft.guide.core.gfx.*;
import io.github.heathensoft.guide.utils.Resources;
import org.joml.Vector2d;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * Frederik Dahl 12/5/2024
 */
public class RendererTest implements Disposable {

    private final int vertex_attrib_array;
    private final int vertex_buffer_object;
    private final ShaderProgram shader_program;
    private final Texture texture;


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

        ByteBuffer png = Resources.readToBuffer("texture-test.png",512);
        Bitmap bitmap = new Bitmap(png,false); // decode the png to a bitmap
        texture = bitmap.asTexture(); // create texture from bitmap
        texture.bindToSlot(0);  // bind to texture slot 0
        texture.filterNearest(); // sample nearest pixel (as opposed to linear filtering)
        texture.textureRepeat(); // UV repeats
        bitmap.dispose(); // free the bitmap

        // ***********************************************************************************************
        // VERTICES

        vertex_attrib_array = glGenVertexArrays();
        vertex_buffer_object = glGenBuffers();
        glBindVertexArray(vertex_attrib_array);
        glBindBuffer(GL_ARRAY_BUFFER,vertex_buffer_object);
        // size of buffer = 6 vertices * 5 floats * 4 bytes
        glBufferData(GL_ARRAY_BUFFER,(long) 6 * 5 * Float.BYTES,GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,5 * Float.BYTES,0);
        glVertexAttribPointer(1,2,GL_FLOAT,false,5 * Float.BYTES,3 * Float.BYTES);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void draw(Vector2d cursor) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            Resolution app_res = Engine.get().window().gameResolution();
            float cx = (float) cursor.x() * app_res.width();
            float cy = (float) cursor.y() * app_res.height();
            float x1 = cx - 32;
            float x2 = cx + 32;
            float y1 = cy - 32;
            float y2 = cy + 32;
            float u1 = 0.0f;
            float v1 = 0.0f;
            float u2 = 1.0f;
            float v2 = 1.0f;
            FloatBuffer vertices = stack.mallocFloat(6 * 5);
            vertices.put(x1).put(y2).put(0).put(u1).put(v1);
            vertices.put(x1).put(y1).put(0).put(u1).put(v2);
            vertices.put(x2).put(y2).put(0).put(u2).put(v1);
            vertices.put(x2).put(y2).put(0).put(u2).put(v1);
            vertices.put(x1).put(y1).put(0).put(u1).put(v2);
            vertices.put(x2).put(y1).put(0).put(u2).put(v2);
            glBindBuffer(GL_ARRAY_BUFFER,vertex_buffer_object);
            glBufferSubData(GL_ARRAY_BUFFER,0,vertices.flip());
        }
        ShaderProgram.useProgram(shader_program);
        glBindVertexArray(vertex_attrib_array);
        glDrawArrays(GL_TRIANGLES,0,6);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteVertexArrays(vertex_attrib_array);
        glDeleteBuffers(vertex_buffer_object);
        Disposable.dispose(texture);
    }


}
