package io.github.heathensoft.guide.core.gfx;

import io.github.heathensoft.guide.core.Camera2D;
import io.github.heathensoft.guide.core.Disposable;
import io.github.heathensoft.guide.utils.Color;
import io.github.heathensoft.guide.utils.Resources;
import io.github.heathensoft.guide.utils.U;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 *
 *
 * Frederik Dahl 12/30/2024
 */
public class SpriteBatch implements Disposable {

    /*
     * v0------v3
     * |        |
     * |        |
     * v1------v2
     *
     * -----------------------------------
     * vertex: | pos | uv | color | bits |
     * -----------------------------------
     * size:   | 2   | 2  | 1     | 1    |
     * -----------------------------------
     */

    private static final int SPRITE_COUNT_LIMIT = (Short.MAX_VALUE + 1) / 4;
    private static final int SAMPLER_ARRAY_SIZE = 15; // (not 16) using 0x0F for NO_TEXTURE
    private static final int SAMPLER_ARRAY_TEX_UNIT_OFFSET = 16;

    private final ShaderProgram program;    // shader program
    private final SamplerArray samplers;    // shader samplers
    private final FloatBuffer vertices;     // cpu vertex buffer
    private final int vao;                  // vertex array object
    private final int vbo;                  // vertex buffer object
    private final int ebo;                  // element buffer object
    private final int limit;                // batch sprite limit
    private int count;                      // num buffered sprites
    private int draw_calls;                 // draw calls between begin to end
    private boolean buffering;              // begin has been called


    public SpriteBatch(int capacity) throws Exception {

        String vert_shader_source = Resources.asString("spritebatch.vert");
        String frag_shader_source = Resources.asString("spritebatch.frag");
        Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
        Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
        program = new ShaderProgram("spritebatch-shader", vert_shader,frag_shader);
        program.detachShaders(true);

        final int vertex_size = 6;
        final int sprite_size = vertex_size * 4;

        limit = U.clamp(capacity,16,SPRITE_COUNT_LIMIT);
        samplers = new SamplerArray(GL_TEXTURE_2D,SAMPLER_ARRAY_SIZE,SAMPLER_ARRAY_TEX_UNIT_OFFSET);
        vertices = MemoryUtil.memAllocFloat(limit * sprite_size);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            ShortBuffer element_array = generateIndicesBuffer(limit);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,element_array,GL_STATIC_DRAW);
            MemoryUtil.memFree(element_array);
        }
        {
            int sprite_size_bytes = sprite_size * Float.BYTES;
            int buffer_capacity = sprite_size_bytes * limit;
            glBindBuffer(GL_ARRAY_BUFFER,vbo);
            glBufferData(GL_ARRAY_BUFFER,buffer_capacity,GL_DYNAMIC_DRAW);
        }
        {
            int pointer = 0;
            int vertex_size_bytes = vertex_size * Float.BYTES;
            // Position **************************************
            glVertexAttribPointer(0,2,GL_FLOAT,false,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(0);
            pointer += 2 * Float.BYTES;
            // UV Coordinates ********************************
            glVertexAttribPointer(1,2,GL_FLOAT,false,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(1);
            pointer += 2 * Float.BYTES;
            // Color *****************************************
            glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(2);
            pointer += Float.BYTES;
            // Texture unit **********************************
            glVertexAttribPointer(3,1,GL_FLOAT,false,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(3);
        }

        glBindVertexArray(0);


    }


    public void begin(Camera2D camera) {
        if (!buffering) {
            ShaderProgram.useProgram(program);
            ShaderProgram.setUniform("u_combined",camera.combined);
            // No depth test
            // existing colors in the buffer
            // will always be overwritten
            glDisable(GL_DEPTH_TEST);
            // Alpha blending
            // Here we assume the destination color
            // to be fully opaque (alpha == 1)
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
            buffering = true;
        }
    }

    /** returns num draw calls between begin and end*/
    public int end() {
        if (buffering) {
            flush();
            int calls = draw_calls;
            draw_calls = 0;
            buffering = false;
            return calls;
        } else return 0;
    }

    public void flush() {
        if (count > 0) {
            ShaderProgram.useProgram(program);
            samplers.uploadUniform("u_textures");
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER,vbo);
            glBufferSubData(GL_ARRAY_BUFFER,0,vertices.flip());
            glDrawElements(GL_TRIANGLES,6 * count,GL_UNSIGNED_SHORT,0);
            glBindVertexArray(0);
            vertices.clear();
            draw_calls++;
            count = 0;
        }
    }

    public void dispose() {
        if (vertices != null) MemoryUtil.memFree(vertices);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (ebo != 0) glDeleteBuffers(ebo);
    }

    public void draw(Texture texture, Rectanglef quad, Vector4f uv, int color) {
        if (!buffering) throw new IllegalStateException("call begin() before rendering");
        if (count == limit) flush();
        int texture_slot;
        if (texture == null) {
            texture_slot = SAMPLER_ARRAY_SIZE;
        } else if (texture.hasBeenDisposed()) {
            texture_slot = SAMPLER_ARRAY_SIZE;
            color = Color.ERROR_BITS;
        } else {
            texture_slot = samplers.assignSlot(texture);
            if (texture_slot == SAMPLER_ARRAY_SIZE) {
                flush();
                texture_slot = samplers.assignSlot(texture);
            }
        }
        float color_float = Color.intColorToFloat(color);
        float bits_float = Float.intBitsToFloat(texture_slot);
        vertices.put(quad.minX).put(quad.maxY).put(uv.x).put(uv.y).put(color_float).put(bits_float);
        vertices.put(quad.minX).put(quad.minY).put(uv.x).put(uv.w).put(color_float).put(bits_float);
        vertices.put(quad.maxX).put(quad.minY).put(uv.z).put(uv.w).put(color_float).put(bits_float);
        vertices.put(quad.maxX).put(quad.maxY).put(uv.z).put(uv.y).put(color_float).put(bits_float);
        count++;
    }

    private static ShortBuffer generateIndicesBuffer(int sprites) {
        int len = sprites * 6;
        ShortBuffer buffer = MemoryUtil.memAllocShort(len);
        buffer.put(generateIndices(sprites));
        //for (int i = 0, j = 0; i < len; i += 6, j += 4) {
        //    buffer.put(i,          (short)(j    ));
        //    buffer.put(i + 1,(short)(j + 1));
        //    buffer.put(i + 2,(short)(j + 2));
        //    buffer.put(i + 3,(short)(j + 2));
        //    buffer.put(i + 4,(short)(j + 3));
        //    buffer.put(i + 5,(short)(j    ));
        //}

        return buffer.flip();
    }

    private static short[] generateIndices(int sprites) {
        int len = sprites * 6;
        short[] indices = new short[len];
        short j = 0;
        for (int i = 0; i < len; i += 6, j += 4) {
            indices[i] = j;
            indices[i + 1] = (short)(j + 1);
            indices[i + 2] = (short)(j + 2);
            indices[i + 3] = (short)(j + 2);
            indices[i + 4] = (short)(j + 3);
            indices[i + 5] = j;
        } return indices;
    }

}
