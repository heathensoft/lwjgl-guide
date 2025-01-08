package io.github.heathensoft.jagfw.core.gfx;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.Color;
import io.github.heathensoft.jagfw.utils.Resources;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Frederik Dahl 1/4/2025
 */
public class LineBatch implements Disposable {

    private static final int VERTEX_SIZE_FLOAT = 3; // (1 color + 2 pos)
    private static final int LINE_SIZE_FLOAT = 2 * VERTEX_SIZE_FLOAT;

    private final ShaderProgram program;    // shader program
    private final FloatBuffer vertices;     // cpu vertex buffer
    private final int vao;                  // vertex array object
    private final int vbo;                  // vertex buffer object
    private final int limit;                // batch sprite limit
    private int count;                      // num buffered sprites
    private int draw_calls;                 // draw calls between begin to end
    private int previous_int_color;
    private float color_float_bits;
    private boolean buffering;              // begin has been called


    public LineBatch(int capacity) throws Exception {
        // Load / Compile Shaders
        String vert_shader_source = Resources.asString("line-draw.vert");
        String frag_shader_source = Resources.asString("line-draw.frag");
        Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
        Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
        program = new ShaderProgram("line-draw-shader", vert_shader,frag_shader);
        program.detachShaders(true);
        // **********************************************
        limit = Math.max(capacity,16);
        vertices = MemoryUtil.memAllocFloat(limit * LINE_SIZE_FLOAT);
        // Generate GL Objects
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        {
            // Vertex Buffer
            int line_size_bytes = LINE_SIZE_FLOAT * Float.BYTES;
            int buffer_capacity = line_size_bytes * limit;
            glBindBuffer(GL_ARRAY_BUFFER,vbo);
            glBufferData(GL_ARRAY_BUFFER,buffer_capacity,GL_DYNAMIC_DRAW);
        }
        {
            // Vertex Attributes
            int vertex_size = VERTEX_SIZE_FLOAT * Float.BYTES;
            glVertexAttribPointer(0,2,GL_FLOAT,false,vertex_size,0);
            glVertexAttribPointer(1,4,GL_UNSIGNED_BYTE,true,vertex_size,2 * Float.BYTES);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
        }
        glBindVertexArray(0);
        previous_int_color = Color.WHITE_BITS;
        color_float_bits = Color.intColorToFloat(previous_int_color);
    }

    public void begin(Camera2D camera) {
        if (!buffering) {
            ShaderProgram.useProgram(program);
            ShaderProgram.setUniform("u_combined",camera.combined);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
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
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER,vbo);
            glBufferSubData(GL_ARRAY_BUFFER,0,vertices.flip());
            glDrawArrays(GL_LINES,0,count * 2);
            glBindVertexArray(0);
            vertices.clear();
            draw_calls++;
            count = 0;
        }

    }

    public void drawCircle(Vector2f center, float radius, int resolution, int color) {
        if (!buffering) throw new IllegalStateException("call begin() before rendering");
        if (resolution > 3 && resolution <= limit) {
            if ((count + resolution) >= limit) flush();
            if (color != previous_int_color) {
                previous_int_color = color;
                color_float_bits = Color.intColorToFloat(color);
            }
            Vector2f p0 = U.popSetVec2(center).add(radius,0f);
            Vector2f p1 = U.popVec2();
            float delta = Math.PI_TIMES_2_f / resolution;
            for (int i = 1; i <= resolution; i++) {
                float angle = delta * i;
                float x = radius * Math.cos(angle);
                float y = radius * Math.sin(angle);
                p1.set(center).add(x,y);
                push(p0.x,p0.y,p1.x,p1.y);
                p0.set(p1);
            }
            U.pushVec2(2);
        }
    }

    public void drawRectangle(Rectanglef quad, int color) {
        if (!buffering) throw new IllegalStateException("call begin() before rendering");
        if ((count + 4) >= limit) flush();
        if (color != previous_int_color) {
            previous_int_color = color;
            color_float_bits = Color.intColorToFloat(color);
        }

    }

    public void draw(Vector2f position, float rotation, float radius, int color) {
        if (!buffering) throw new IllegalStateException("call begin() before rendering");
        if (count == limit) flush();
        if (color != previous_int_color) {
            previous_int_color = color;
            color_float_bits = Color.intColorToFloat(color);
        }

    }

    public void draw(Vector2f pos1, Vector2f pos2, int color) {
        if (!buffering) throw new IllegalStateException("call begin() before rendering");
        if (count == limit) flush();
        if (color != previous_int_color) {
            previous_int_color = color;
            color_float_bits = Color.intColorToFloat(color);
        }
    }


    private void push(float x0, float y0, float x1, float y1) {
        vertices.put(x0).put(y0).put(color_float_bits);
        vertices.put(x1).put(y1).put(color_float_bits);
        count++;
    }

    public void setLineWidth(float width) {
        glLineWidth(width);
    }

    public float getLineWidth() {
        return glGetFloat(GL_LINE_WIDTH);
    }

    public void enableSmoothLines(boolean enable) {
        if (enable) glEnable(GL_LINE_SMOOTH);
        else glDisable(GL_LINE_SMOOTH);
    }

    public boolean smoothLinesEnabled() {
        return glIsEnabled(GL_LINE_SMOOTH);
    }

    public int drawCalls() {
        return draw_calls;
    }

    @Override
    public void dispose() {
        if (vertices != null) MemoryUtil.memFree(vertices);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (vbo != 0) glDeleteBuffers(vbo);
    }
}
