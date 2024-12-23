package io.github.heathensoft.guide.core;

import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL43.*;

/**
 * Frederik Dahl 12/8/2024
 */
public class ShaderProgram {

    private static final Map<Integer,ShaderProgram> programs_by_id = new HashMap<>();
    private static ShaderProgram current_program;

    private final int handle;
    private final String name;
    private Map<String,Integer> uniforms;
    private Shader vert_shader;
    private Shader frag_shader;
    private Shader geom_shader;

    /**
     * failing to link the program will detach the shaders from the program and delete the program.
     * failing to link the program will not delete the shaders
     * @param name optional program name
     * @param vert_shader vertex shader
     * @param frag_shader fragment shader
     * @param geom_shader geometry shader
     * @throws Exception could not link the program
     */
    public ShaderProgram(String name, Shader vert_shader, Shader frag_shader, Shader geom_shader) throws Exception {
        this.name = name == null ? "unnamed_shader_program" : name;
        this.handle = glCreateProgram();
        if (vert_shader != null) glAttachShader(handle,vert_shader.handle());
        if (frag_shader != null) glAttachShader(handle,frag_shader.handle());
        if (geom_shader != null) glAttachShader(handle,geom_shader.handle());
        Logger.debug("creating shader program: \"{}\"",this.name);
        glLinkProgram(handle);
        int status = glGetProgrami(handle,GL_LINK_STATUS);
        if (status == GL_TRUE) {
            this.vert_shader = vert_shader;
            this.frag_shader = frag_shader;
            this.geom_shader = geom_shader;
            this.uniforms = createUniformLocationMap(handle);
            programs_by_id.putIfAbsent(handle,this);
        }  else {
            if (vert_shader != null) glDetachShader(handle,vert_shader.handle());
            if (frag_shader != null) glDetachShader(handle,frag_shader.handle());
            if (geom_shader != null) glDetachShader(handle,geom_shader.handle());
            String error_message = glGetProgramInfoLog(handle);
            glDeleteProgram(handle);
            throw new Exception(error_message);
        }
    }

    public ShaderProgram(Shader vert_shader, Shader frag_shader, Shader geom_shader) throws Exception {
        this(null,vert_shader,frag_shader,geom_shader);
    }

    public ShaderProgram(String name, Shader vert_shader, Shader frag_shader) throws Exception {
        this(name,vert_shader,frag_shader,null);
    }

    public ShaderProgram(Shader vert_shader, Shader frag_shader) throws Exception {
        this(null, vert_shader,frag_shader,null);
    }



    public void detachShaders(boolean delete) {
        if (vert_shader != null) {
            glDetachShader(handle,vert_shader.handle());
            if (delete) vert_shader.dispose();
            vert_shader = null;
        } if (frag_shader != null) {
            glDetachShader(handle,frag_shader.handle());
            if (delete) frag_shader.dispose();
            frag_shader = null;
        } if (geom_shader != null) {
            glDetachShader(handle,geom_shader.handle());
            if (delete) geom_shader.dispose();
            geom_shader = null;
        }
    }

    public Shader get(Shader.Type type) {
        switch (type) {
            case VERT_SHADER -> { return vert_shader; }
            case FRAG_SHADER -> { return frag_shader; }
            case GEOM_SHADER -> { return geom_shader; }
        } return null;
    }

    public String name() {
        return name;
    }

    public int handle() {
        return handle;
    }

    public boolean isUsed() {
        return this == current_program;
    }

    public static ShaderProgram currentProgram() {
        return current_program;
    }

    public static List<ShaderProgram> allPrograms() {
        return programs_by_id.values().stream().toList();
    }

    public static void useProgram(ShaderProgram program) {
        if (program == null) useProgram(GL_NONE);
        else useProgram(program.handle);
    }

    public static void useProgram(int gl_handle) {
        if (gl_handle == GL_NONE) {
            if (current_program != null) {
                glUseProgram(GL_NONE);
                current_program = null; }
        } else if (current_program == null) {
            ShaderProgram program = programs_by_id.get(gl_handle);
            if (program == null) throw new RuntimeException("no such shader program");
            glUseProgram(program.handle);
            current_program = program;
        } else if (current_program.handle != gl_handle) {
            ShaderProgram program = programs_by_id.get(gl_handle);
            if (program == null) throw new RuntimeException("no such shader program");
            glUseProgram(program.handle);
            current_program = program;
        }
    }

    public static void deleteCurrentProgram() {
        deleteProgram(current_program);
    }

    private static void deleteProgram(ShaderProgram program) {
        if (program != null) {
            int program_handle = program.handle;
            programs_by_id.remove(program_handle,program);
            if (program == current_program) {
                current_program = null;
                glUseProgram(GL_NONE);
            } String name = program.name;
            Logger.debug("deleting shader program: \"{}\"",name);
            program.detachShaders(true);
            glDeleteProgram(program_handle);
        }
    }

    public static void deleteAllPrograms() {
        for (var entry : programs_by_id.entrySet()) {
            deleteProgram(entry.getValue());
        }
    }

    public static void setUniform(String name, int i) {
        int uniform_location = getUniformLocation(name);
        glUniform1i(uniform_location,i);
    }

    public static void setUniform(String name, int i0, int i1) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(2);
            buffer.put(i0).put(i1).flip();
            glUniform2iv(uniform_location,buffer);
        }
    }

    public static void setUniform(String name, int i0, int i1, int i2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(3);
            buffer.put(i0).put(i1).put(i2).flip();
            glUniform3iv(uniform_location,buffer);
        }
    }

    public static void setUniform(String name, int i0, int i1, int i2, int i3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(4);
            buffer.put(i0).put(i1).put(i2).put(i3).flip();
            glUniform4iv(uniform_location,buffer);
        }
    }

    public static void setUniform(String name, int[] array) {
        setUniform(name,array,0,array.length);
    }

    public static void setUniform(String name, int[] array, int offset, int count) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(count);
            for (int i = 0; i < count; i++) {
                buffer.put(array[i + offset]);
            } glUniform1iv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, IntBuffer buffer) {
        int uniform_location = getUniformLocation(name);
        glUniform1iv(uniform_location,buffer);
    }


    public static void setUniform(String name, float f) {
        int uniform_location = getUniformLocation(name);
        glUniform1f(uniform_location,f);
    }

    public static void setUniform(String name, float f0, float f1) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(2);
            buffer.put(f0).put(f1).flip();
            glUniform2fv(uniform_location,buffer);
        }
    }

    public static void setUniform(String name, float f0, float f1, float f2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(3);
            buffer.put(f0).put(f1).put(f2).flip();
            glUniform3fv(uniform_location,buffer);
        }
    }

    public static void setUniform(String name, float f0, float f1, float f2, float f3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(4);
            buffer.put(f0).put(f1).put(f2).put(f3).flip();
            glUniform4fv(uniform_location,buffer);
        }
    }

    public static void setUniform(String name, float[] array) {
        setUniform(name,array,0,array.length);
    }

    public static void setUniform(String name, float[] array, int offset, int count) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(count);
            for (int i = 0; i < count; i++) {
                buffer.put(array[i + offset]);
            } glUniform1fv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, FloatBuffer buffer) {
        int uniform_location = getUniformLocation(name);
        glUniform1fv(uniform_location,buffer);
    }

    public static void setUniform(String name, Vector2f vec2) {
        setUniform(name,vec2.x,vec2.y);
    }

    public static void setUniform(String name, Vector3f vec3) {
        setUniform(name,vec3.x,vec3.y,vec3.z);
    }

    public static void setUniform(String name, Vector4f vec4) {
        setUniform(name,vec4.x,vec4.y,vec4.z,vec4.w);
    }

    public static void setUniform(String name, Vector2i vec2) {
        setUniform(name,vec2.x,vec2.y);
    }

    public static void setUniform(String name, Vector3i vec3) {
        setUniform(name,vec3.x,vec3.y,vec3.z);
    }

    public static void setUniform(String name, Vector4i vec4) {
        setUniform(name,vec4.x,vec4.y,vec4.z,vec4.w);
    }

    public static void setUniform(String name, Matrix2f mat2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(4);
            glUniformMatrix2fv(uniform_location,false,mat2.get(buffer));
        }
    }

    public static void setUniform(String name, Matrix3f mat3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(9);
            glUniformMatrix3fv(uniform_location,false,mat3.get(buffer));
        }
    }

    public static void setUniform(String name, Matrix4f mat4) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(16);
            glUniformMatrix4fv(uniform_location,false,mat4.get(buffer));
        }
    }

    public static void setUniform(String name, Vector2f[] vec2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(2 * vec2.length);
            for (Vector2f value : vec2) {
                buffer.put(value.x).put(value.y);
            } glUniform2fv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, Vector3f[] vec3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3 * vec3.length);
            for (Vector3f v : vec3) {
                buffer.put(v.x).put(v.y).put(v.z);
            } glUniform3fv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, Vector4f[] vec4) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * vec4.length);
            for (Vector4f v : vec4) {
                buffer.put(v.x).put(v.y).put(v.z).put(v.w);
            } glUniform4fv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, Vector2i[] vec2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(2 * vec2.length);
            for (Vector2i value : vec2) {
                buffer.put(value.x).put(value.y);
            } glUniform2iv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, Vector3i[] vec3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(3 * vec3.length);
            for (Vector3i v : vec3) {
                buffer.put(v.x).put(v.y).put(v.z);
            } glUniform3iv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, Vector4i[] vec4) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(4 * vec4.length);
            for (Vector4i v : vec4) {
                buffer.put(v.x).put(v.y).put(v.z).put(v.w);
            } glUniform4iv(uniform_location,buffer.flip());
        }
    }

    public static void setUniform(String name, Matrix2f[] mat2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(4 * mat2.length);
            for (int i = 0; i < mat2.length; i++) {
                mat2[i].get(4*i,buffer);
            } glUniformMatrix2fv(uniform_location,false,buffer);
        }
    }

    public static void setUniform(String name, Matrix3f[] mat3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(9 * mat3.length);
            for (int i = 0; i < mat3.length; i++) {
                mat3[i].get(9*i,buffer);
            } glUniformMatrix3fv(uniform_location,false,buffer);
        }
    }

    public static void setUniform(String name, Matrix4f[] mat4) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            FloatBuffer buffer = stack.mallocFloat(16 * mat4.length);
            for (int i = 0; i < mat4.length; i++) {
                mat4[i].get(16*i,buffer);
            } glUniformMatrix4fv(uniform_location,false,buffer);
        }
    }

    public static void setUniformU(String name, int u) {
        int uniform_location = getUniformLocation(name);
        glUniform1ui(uniform_location,u);
    }

    public static void setUniformU(String name, int u0, int u1) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(2);
            buffer.put(u0).put(u1).flip();
            glUniform2uiv(uniform_location,buffer);
        }
    }

    public static void setUniformU(String name, int u0, int u1, int u2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(3);
            buffer.put(u0).put(u1).put(u2).flip();
            glUniform3uiv(uniform_location,buffer);
        }
    }

    public static void setUniformU(String name, int u0, int u1, int u2, int u3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(4);
            buffer.put(u0).put(u1).put(u2).put(u3).flip();
            glUniform4uiv(uniform_location,buffer);
        }
    }

    public static void setUniformU(String name, int[] array) {
        setUniform(name,array,0,array.length);
    }

    public static void setUniformU(String name, int[] array, int offset, int count) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(count);
            for (int i = 0; i < count; i++) {
                buffer.put(array[i + offset]);
            } glUniform1uiv(uniform_location,buffer.flip());
        }
    }

    public static void setUniformU(String name, IntBuffer buffer) {
        int uniform_location = getUniformLocation(name);
        glUniform1uiv(uniform_location,buffer);
    }

    public static void setUniformU(String name, Vector2i vec2) {
        setUniformU(name,vec2.x,vec2.y);
    }

    public static void setUniformU(String name, Vector3i vec3) {
        setUniformU(name,vec3.x,vec3.y,vec3.z);
    }

    public static void setUniformU(String name, Vector4i vec4) {
        setUniformU(name,vec4.x,vec4.y,vec4.z,vec4.w);
    }

    public static void setUniformU(String name, Vector2i[] vec2) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(2 * vec2.length);
            for (Vector2i value : vec2) {
                buffer.put(value.x).put(value.y);
            } glUniform2uiv(uniform_location,buffer.flip());
        }
    }

    public static void setUniformU(String name, Vector3i[] vec3) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(3 * vec3.length);
            for (Vector3i v : vec3) {
                buffer.put(v.x).put(v.y).put(v.z);
            } glUniform3uiv(uniform_location,buffer.flip());
        }
    }

    public static void setUniformU(String name, Vector4i[] vec4) {
        int uniform_location = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(4 * vec4.length);
            for (Vector4i v : vec4) {
                buffer.put(v.x).put(v.y).put(v.z).put(v.w);
            } glUniform4uiv(uniform_location,buffer.flip());
        }
    }

    private static int getUniformLocation(String name) {
        if (current_program == null) throw new RuntimeException("shader program no program bound");
        Integer uniform_location = current_program.uniforms.get(name);
        if (uniform_location == null) {
            String message = "shader program [" + current_program.name +"] no such uniform: \"" + name + "\"";
            throw new RuntimeException(message);
        } return uniform_location;
    }

    private static Map<String,Integer> createUniformLocationMap(int program_handle) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer num_uniform_buffer = stack.mallocInt(1);
            glGetProgramInterfaceiv(program_handle,GL_UNIFORM, GL_ACTIVE_RESOURCES, num_uniform_buffer);
            int num_uniforms = num_uniform_buffer.get(0);
            Map<String,Integer> uniform_location_map = new HashMap<>();
            for (int uniform = 0; uniform < num_uniforms; uniform++) {
                String name = glGetProgramResourceName(program_handle,GL_UNIFORM,uniform);
                int uniform_location = glGetUniformLocation(program_handle,name);
                if (uniform_location >= 0) {
                    // String[] split = name.split("\\[[\\d]+?\\]");
                    String[] split = name.split("\\[\\d+?]");
                    if (split.length > 0) {
                        String uniform_name = split[0];
                        uniform_location_map.putIfAbsent(uniform_name,uniform_location);
                    }
                }
            } return uniform_location_map;
        }
    }
}
