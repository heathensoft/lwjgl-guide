package io.github.heathensoft.jagfw.core.gfx;

import io.github.heathensoft.jagfw.utils.*;
import io.github.heathensoft.jagfw.core.Disposable;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Frederik Dahl 12/30/2024
 */
public class SpriteBatch implements Disposable {

    /*
     * v0------v3
     * |        |
     * |        |
     * v1------v2
     * -----------------------------------
     * vertex: | pos | uv | color | bits |
     * -----------------------------------
     * size:   | 2   | 2  | 1     | 1    |
     * -----------------------------------
     */

    private static final int SPRITE_COUNT_LIMIT = (Short.MAX_VALUE + 1) / 4;
    private static final int SAMPLER_ARRAY_SIZE = 15; // (not 16) using 0x0F for NO_TEXTURE
    private static final int SAMPLER_ARRAY_TEX_UNIT_OFFSET = 16;
    private static final int VERTEX_SIZE_FLOAT = 6;
    private static final int SPRITE_SIZE_FLOAT = VERTEX_SIZE_FLOAT * 4;

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
    private boolean depth_enabled;          // enable depth layers


    public SpriteBatch(int capacity) throws Exception {
        // Load / Compile Shaders
        String vert_shader_source = Resources.asString("spritebatch.vert");
        String frag_shader_source = Resources.asString("spritebatch.frag");
        Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
        Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
        program = new ShaderProgram("spritebatch-shader", vert_shader,frag_shader);
        program.detachShaders(true);
        // **********************************************
        limit = U.clamp(capacity,16,SPRITE_COUNT_LIMIT);
        samplers = new SamplerArray(GL_TEXTURE_2D,SAMPLER_ARRAY_SIZE,SAMPLER_ARRAY_TEX_UNIT_OFFSET);
        vertices = MemoryUtil.memAllocFloat(limit * SPRITE_SIZE_FLOAT);
        // Generate GL Objects
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        glBindVertexArray(vao);
        {
            // Element Buffer
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
            ShortBuffer element_array = generateIndicesBuffer(limit);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,element_array,GL_STATIC_DRAW);
            MemoryUtil.memFree(element_array);
        }
        {
            // Vertex Buffer
            int sprite_size_bytes = SPRITE_SIZE_FLOAT * Float.BYTES;
            int buffer_capacity = sprite_size_bytes * limit;
            glBindBuffer(GL_ARRAY_BUFFER,vbo);
            glBufferData(GL_ARRAY_BUFFER,buffer_capacity,GL_DYNAMIC_DRAW);
        }
        {
            // Vertex Attributes
            int pointer = 0;
            int vertex_size_bytes = VERTEX_SIZE_FLOAT * Float.BYTES;
            glVertexAttribPointer(0,2,GL_FLOAT,false,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(0); pointer += 2 * Float.BYTES;
            glVertexAttribPointer(1,2,GL_FLOAT,false,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(1); pointer += 2 * Float.BYTES;
            glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(2); pointer += Float.BYTES;
            glVertexAttribPointer(3,1,GL_FLOAT,false,vertex_size_bytes,pointer);
            glEnableVertexAttribArray(3);
        }
        glBindVertexArray(0);
    }

    public void setCamera(Camera2D camera) {
        if (buffering) flush();
        ShaderProgram.useProgram(program);
        ShaderProgram.setUniform("u_combined",camera.combined);
    }

    public void enableLayers(boolean enable) {
        if (buffering) {
            if (depth_enabled != enable) {
                flush();
                ShaderProgram.setUniform("u_depth_enabled", depth_enabled ? 1 : 0);
            }
        } depth_enabled = enable;
    }

    public void begin() {
        if (!buffering) {
            ShaderProgram.useProgram(program);
            ShaderProgram.setUniform("u_depth_enabled", depth_enabled ? 1 : 0);
            if (depth_enabled) {
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LEQUAL);
            } else glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
            buffering = true;
            draw_calls = 0;
        }
    }

    public void begin(Camera2D camera) {
        if (!buffering) {
            ShaderProgram.useProgram(program);
            ShaderProgram.setUniform("u_combined",camera.combined);
            ShaderProgram.setUniform("u_depth_enabled", depth_enabled ? 1 : 0);
            if (depth_enabled) {
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LEQUAL);
            } else glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
            buffering = true;
            draw_calls = 0;
        }
    }

    public void end() {
        if (buffering) {
            buffering = false;
            flush();
        }
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

    public void draw(Texture texture, Rectanglef quad, Vector4f uv, int color, int z_layer) {
        if (!buffering) throw new IllegalStateException("call begin() before rendering");
        if (count == limit) flush();
        int texture_slot;
        if (texture == null) { texture_slot = SAMPLER_ARRAY_SIZE;
        } else if (texture.hasBeenDisposed()) {
            texture_slot = SAMPLER_ARRAY_SIZE;
            color = Color.ERROR_BITS;
        } else { texture_slot = samplers.assignSlot(texture);
            if (texture_slot == SAMPLER_ARRAY_SIZE) { flush();
                texture_slot = samplers.assignSlot(texture);
            }
        }

        int bits = 0;

        {
            bits |= texture_slot;
            bits |= ((z_layer & 0x0F) << 4);
        }

        float color_float = Color.intColorToFloat(color);
        float bits_float = Float.intBitsToFloat(bits);


        vertices.put(quad.minX).put(quad.maxY).put(uv.x).put(uv.y).put(color_float).put(bits_float);
        vertices.put(quad.minX).put(quad.minY).put(uv.x).put(uv.w).put(color_float).put(bits_float);
        vertices.put(quad.maxX).put(quad.minY).put(uv.z).put(uv.w).put(color_float).put(bits_float);
        vertices.put(quad.maxX).put(quad.maxY).put(uv.z).put(uv.y).put(color_float).put(bits_float);
        count++;
    }

    public int drawCalls() {
        return draw_calls;
    }

    private static ShortBuffer generateIndicesBuffer(int sprites) {
        int len = sprites * 6;
        ShortBuffer buffer = MemoryUtil.memAllocShort(len);
        for (int i = 0, j = 0; i < len; i += 6, j += 4) {
            buffer.put(i,          (short)(j    ));
            buffer.put(i + 1,(short)(j + 1));
            buffer.put(i + 2,(short)(j + 2));
            buffer.put(i + 3,(short)(j + 2));
            buffer.put(i + 4,(short)(j + 3));
            buffer.put(i + 5,(short)(j    ));
        } return buffer;
    }

}
