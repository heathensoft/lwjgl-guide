package io.github.heathensoft.guide.core;

import io.github.heathensoft.guide.utils.Disposable;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * Frederik Dahl 12/8/2024
 */
public class Shader implements Disposable {

    private static final Map<Integer,Program> programs = new HashMap<>();
    private static Program current_program;

    public enum Type { // supported types
        VERT_SHADER(GL_VERTEX_SHADER),
        FRAG_SHADER(GL_FRAGMENT_SHADER),
        GEOM_SHADER(GL_GEOMETRY_SHADER);
        public final int gl_enum;
        Type(int gl_enum) { this.gl_enum = gl_enum; }
    }

    public static final class Program {
        private final Map<String,Integer> uniforms;
        private final String name;
        private final int handle;
        private Shader vert_shader;
        private Shader geom_shader;
        private Shader frag_shader;
        private boolean linked;

        public Program() { this(null); }
        public Program(String name) {
            this.name = name == null ? "unnamed_shader_program" : name;
            this.uniforms = new HashMap<>();
            this.handle = glCreateProgram();
        }

        public Shader attachShader(Shader shader) {
            if (shader == null) throw new RuntimeException("null arg shader");
            if (linked) throw new IllegalStateException("cannot attach shader to linked program");
            switch (shader.type) {
                case VERT_SHADER -> {
                    if (vert_shader != shader) {
                        if (vert_shader != null) {
                          glDetachShader(handle,vert_shader.handle);
                          Shader existing = vert_shader;
                          vert_shader = shader;
                          glAttachShader(handle,vert_shader.handle);
                          return existing;
                        } glAttachShader(handle,shader.handle);
                    }
                }
                case FRAG_SHADER -> {
                    if (frag_shader != shader) {
                        if (frag_shader != null) {
                            glDetachShader(handle,frag_shader.handle);
                            Shader existing = frag_shader;
                            frag_shader = shader;
                            glAttachShader(handle,frag_shader.handle);
                            return existing;
                        } glAttachShader(handle,shader.handle);
                    }
                }
                case GEOM_SHADER -> {
                    if (geom_shader != shader) {
                        if (geom_shader != null) {
                            glDetachShader(handle,geom_shader.handle);
                            Shader existing = geom_shader;
                            geom_shader = shader;
                            glAttachShader(handle,geom_shader.handle);
                            return existing;
                        } glAttachShader(handle,shader.handle);
                    }
                }
            } return null;
        }

        private void compileShaders() throws Exception {
            Logger.debug("compiling shaders for program: {}",name);
            if (vert_shader == null) throw new RuntimeException("missing vertex shader");
            if (frag_shader == null) throw new RuntimeException("missing fragment shader");
            if (!vert_shader.isCompiled()) {
                Logger.debug("compiling vert shader");
                if (!vert_shader.compile()) {
                    String info_log = vert_shader.shaderInfoLog();
                    throw new Exception("vertex shader: " + info_log);
                }
            }
            Logger.debug("vert shader compiled");
            if (!frag_shader.isCompiled()) {
                Logger.debug("compiling frag shader");
                if (!frag_shader.compile()) {
                    String info_log = frag_shader.shaderInfoLog();
                    throw new Exception("fragment shader: " + info_log);
                }
            }
            Logger.debug("frag shader compiled");
            if (geom_shader != null) {
                if (!geom_shader.isCompiled()) {
                    Logger.debug("compiling geom shader");
                    if (!geom_shader.compile()) {
                        String info_log = geom_shader.shaderInfoLog();
                        throw new Exception("geometry shader: " + info_log);
                    }
                }
                Logger.debug("geom shader compiled");
            }
        }

        public void editShader(String source, Type type) throws Exception {
        }

        public void compileAndLinkProgram(boolean delete_shaders) throws Exception {
            if (!linked) {
                compileShaders();

                linked = true;
            }

        }

        public Shader get(Type type) {
            switch (type) {
                case VERT_SHADER -> { return vert_shader; }
                case FRAG_SHADER -> { return frag_shader; }
                case GEOM_SHADER -> { return geom_shader; }
            } return null;
        }


    }

    private String prev_compiled; // previously compiled source code (If editing fails, revert to this)
    private final Type type;    // shader type (vert, frag, geom)
    private final int handle;   // opengl shader handle
    private boolean compiled;   // shader has been compiled
    private boolean disposed;   // shader has been deleted

    public Shader(Type type) { this(null,type); }
    public Shader(String source, Type type) {
        if (type == null) throw new RuntimeException("null arg shader type");
        this.handle = glCreateShader(type.gl_enum);
        this.type = type;
        setSource(source);
    }

    public String getSource() { return glGetShaderSource(handle); }
    public Type type() { return type; }
    public int handle() { return handle; }
    public boolean isCompiled() { return compiled; }

    public void setSource(String source) {
        if (disposed) throw new IllegalStateException("shader has been disposed");
        source = source == null ? "" : source;
        glShaderSource(handle,source);
        compiled = false;
    }

    public void dispose() {
        if (!disposed) {
            glDeleteShader(handle);
            disposed = true;
        }
    }

    protected boolean compile() {
        if (disposed) throw new IllegalStateException("shader has been disposed");
        if (!compiled) {
            glCompileShader(handle);
            int compile_status = glGetShaderi(handle,GL_COMPILE_STATUS);
            if (compile_status == GL_TRUE) {
                String prev = glGetShaderSource(handle);
                if (!prev.isBlank()) prev_compiled = prev;
                compiled = true;
            } else {
                if (tryRevert()) {
                    compiled = true;
                    return false;
                }

            }
        } return compiled;
    }

    protected String shaderInfoLog() {
        if (disposed) throw new IllegalStateException("shader has been disposed");
        return glGetShaderInfoLog(handle);
    }

    private boolean tryRevert() {
        // Try to revert to a previous state where compilation succeeded
        while (!(prev_compiled == null)) {
            glShaderSource(handle, prev_compiled);
            glCompileShader(handle);
            int compile_status = glGetShaderi(handle,GL_COMPILE_STATUS);
            if (compile_status == GL_TRUE) {
                return true;
            } prev_compiled = null;
        } return false;
    }

}
