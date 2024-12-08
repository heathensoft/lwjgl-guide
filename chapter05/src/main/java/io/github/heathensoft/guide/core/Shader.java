package io.github.heathensoft.guide.core;

import io.github.heathensoft.guide.utils.Disposable;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * Frederik Dahl 12/8/2024
 */
public class Shader implements Disposable {

    public enum Type {
        VERT_SHADER(GL_VERTEX_SHADER),
        FRAG_SHADER(GL_FRAGMENT_SHADER),
        GEOM_SHADER(GL_GEOMETRY_SHADER);
        public final int gl_enum;
        Type(int gl_enum) { this.gl_enum = gl_enum; }
    }

    private final Type type;    // shader type (vert, frag, geom)
    private final int handle;   // opengl shader handle
    private boolean disposed;   // shader has been deleted

    /** will compile the shader -> throws exception if compilation failed */
    protected Shader(String source, Type type) throws Exception {
        if (type == null) throw new RuntimeException("null arg shader type");
        this.handle = glCreateShader(type.gl_enum);
        this.type = type;
        glShaderSource(handle,source == null ? "" : source);
        glCompileShader(handle);
        int compile_status = glGetShaderi(handle,GL_COMPILE_STATUS);
        if (compile_status == GL_FALSE) {
            String error_log = glGetShaderInfoLog(handle);
            throw new Exception(error_log);
        }
    }

    public String sourceCode() { return glGetShaderSource(handle); }
    public Type type() { return type; }
    public int handle() { return handle; }
    public boolean isDisposed() { return disposed; }

    public void dispose() {
        if (!disposed) {
            glDeleteShader(handle);
            disposed = true;
        }
    }
}
