package no.hio.jagfw.testing;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.gfx.Shader;
import io.github.heathensoft.jagfw.core.gfx.ShaderProgram;
import io.github.heathensoft.jagfw.core.utils.Camera2D;
import io.github.heathensoft.jagfw.core.utils.Resources;
import org.joml.Vector2f;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL30.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_TRIANGLES;
import static org.lwjgl.opengl.GL30.glDisable;
import static org.lwjgl.opengl.GL30.glDrawArrays;
import static org.lwjgl.opengl.GL30.*;

/**
 * Frederik Dahl 12/28/2024
 */
public class Background implements Disposable {

    public final Vector2f map_size;
    private final int vertex_attrib_array;
    private final int vertex_buffer_object;
    private final ShaderProgram shader_program;

    public Background(int width, int height) throws Exception {

        // Loading shader source code files from the project "resources folder"
        String vert_shader_source = Resources.asString("checkered-bg.vert");
        String frag_shader_source = Resources.asString("checkered-bg.frag");
        // Uploading the source code strings to the gpu then compile the code
        // Each shader is compiled separately.
        Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
        Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
        shader_program = new ShaderProgram("checkered-background",vert_shader,frag_shader);
        shader_program.detachShaders(true);
        map_size = new Vector2f(width,height);

        final float[] vertices = new float[] {
                /*{ V0 }*/-1, 1,/*position (xy)*/
                /*{ V1 }*/-1,-1,/*position (xy)*/
                /*{ V2 }*/ 1, 1,/*position (xy)*/
                /*{ V3 }*/ 1, 1,/*position (xy)*/
                /*{ V4 }*/-1,-1,/*position (xy)*/
                /*{ V5 }*/ 1,-1,/*position (xy)*/
        };

        vertex_attrib_array = glGenVertexArrays();
        vertex_buffer_object = glGenBuffers();
        glBindVertexArray(vertex_attrib_array);
        glBindBuffer(GL_ARRAY_BUFFER,vertex_buffer_object);
        glBufferData(GL_ARRAY_BUFFER,vertices,GL_STATIC_DRAW);
        glVertexAttribPointer(0,2,GL_FLOAT,false,2 * Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void draw(Camera2D camera) {
        ShaderProgram.useProgram(shader_program);
        ShaderProgram.setUniform("u_combined_inv",camera.combined_inv);
        ShaderProgram.setUniform("u_map_size",map_size);
        glDisable(GL_DEPTH_TEST);
        glBindVertexArray(vertex_attrib_array);
        glDrawArrays(GL_TRIANGLES,0,6);
        glBindVertexArray(0);
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(vertex_attrib_array);
        glDeleteBuffers(vertex_buffer_object);
    }
}
