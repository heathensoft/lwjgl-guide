package io.github.heathensoft.guide.core;

import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;
import static org.lwjgl.opengl.GL30C.GL_INVALID_FRAMEBUFFER_OPERATION;
import static org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BLOCK_SIZE;
import static org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS;

/**
 * Helper class
 * Frederik Dahl 12/5/2024
 */
public class GLInfo {

    public final int version_major;
    public final int version_minor;
    public final int max_texture_units;
    public final int max_draw_buffers;
    public final int max_uniform_buffer_bindings;
    public final int max_uniform_block_size;
    public final int max_vertex_attributes;
    public final boolean core_profile;

    protected GLInfo(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer buffer = stack.mallocInt(1);
            glGetIntegerv(GL_MAJOR_VERSION, buffer);
            version_major = buffer.get(0);
            glGetIntegerv(GL_MINOR_VERSION, buffer);
            version_minor = buffer.get(0);
            glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, buffer);
            max_texture_units = buffer.get(0);
            glGetIntegerv(GL_MAX_DRAW_BUFFERS, buffer);
            max_draw_buffers = buffer.get(0);
            glGetIntegerv(GL_MAX_UNIFORM_BUFFER_BINDINGS, buffer);
            max_uniform_buffer_bindings = buffer.get(0);
            glGetIntegerv(GL_MAX_UNIFORM_BLOCK_SIZE, buffer);
            max_uniform_block_size = buffer.get(0);
            glGetIntegerv(GL_MAX_VERTEX_ATTRIBS, buffer);
            max_vertex_attributes = buffer.get(0);
            core_profile = glfwGetWindowAttrib(window,GLFW_OPENGL_PROFILE) == GLFW_OPENGL_CORE_PROFILE;
        }
        Logger.debug("opengl client limitations:");
        Logger.debug("opengl version: {}.{}",version_major,version_minor);
        Logger.debug("opengl core profile: {}",core_profile);
        Logger.debug("opengl max texture units: {}", max_texture_units);
        Logger.debug("opengl max shader output draw buffers: {}", max_draw_buffers);
        Logger.debug("opengl max uniform buffer bindings: {}", max_uniform_buffer_bindings);
        Logger.debug("opengl max uniform buffer block size: {} Bytes", max_uniform_block_size);
        Logger.debug("opengl max vertex attributes: {}", max_vertex_attributes);
    }

    public String shaderVersionString() {
        String string = "#version " + version_major + version_minor + 0;
        if (core_profile) string += " core\n";
        return string;
    }

    public static void checkError() {
        int code;
        while ((code = glGetError()) != GL_NO_ERROR) { // redundant I think
            String error = switch (code) {
                case GL_INVALID_ENUM                    -> "INVALID_ENUM";
                case GL_INVALID_VALUE                   -> "INVALID_VALUE";
                case GL_INVALID_OPERATION               -> "INVALID_OPERATION";
                case GL_STACK_OVERFLOW                  -> "STACK_OVERFLOW";
                case GL_STACK_UNDERFLOW                 -> "STACK_UNDERFLOW";
                case GL_OUT_OF_MEMORY                   -> "OUT_OF_MEMORY";
                case GL_INVALID_FRAMEBUFFER_OPERATION   -> "INVALID_FRAMEBUFFER_OPERATION";
                default                                 -> "ERROR CODE: " + code;
            };
            Logger.warn("GL_ERROR: {}",error);
        }
    }
}
