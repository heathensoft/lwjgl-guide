package io.github.heathensoft.guide.core;

import io.github.heathensoft.guide.utils.OS;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.tinylog.Logger;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_TRUE;

/**
 * Frederik Dahl 12/1/2024
 */
public class GLFWWindow {

    private long window;

    private int window_position_x;
    private int window_position_y;
    private int window_width;
    private int window_height;
    private int framebuffer_width;
    private int framebuffer_height;

    protected void initialize(BootConfiguration config) throws Exception {

        if (config.supported_resolutions.isEmpty()) {
            throw new Exception("the application failed provide resolution options");
        }

        glfwSetErrorCallback(new GLFWErrorCallback() {
            public void invoke(int error, long description) {
                if (error != GLFW_NO_ERROR) {
                    Logger.error("GLFW ERROR[{}]: {}",
                    error,GLFWErrorCallback.getDescription(description));
                }
            }
        });

        if (!glfwInit()) {
            freeGLFWErrorCallback();
            throw new Exception("unable to initialize glfw");
        }

        Logger.debug("initialized glfw");


        long monitor = glfwGetPrimaryMonitor();
        if (monitor == 0L) {
            glfwTerminate();
            freeGLFWErrorCallback();
            throw new Exception("unable to detect primary monitor");
        }

        GLFWVidMode display = glfwGetVideoMode(monitor);
        if (display == null) {
            glfwTerminate();
            freeGLFWErrorCallback();
            throw new Exception("unable to get the primary monitors current video mode");
        }

        Logger.debug("current monitor display: {}:{} {}hz",
        display.width(),display.height(),display.refreshRate());

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CENTER_CURSOR, GLFW_TRUE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_RESIZABLE, config.resizable_window ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 0); // 4 for antialiasing
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4); // Require OpenGL version 4.4
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4); // or later to run the program
        glfwWindowHint(GLFW_REFRESH_RATE,display.refreshRate());

        if (OS.name == OS.NAME.MAC) {
            Logger.debug("using forward compatibility for MAC user");
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        } else {
            Logger.debug("using opengl core profile for non-MAC user");
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        }

        if (config.windowed_mode) {
            window = glfwCreateWindow(
                    config.windowed_mode_width,
                    config.windowed_mode_height,
                    config.window_title, 0L,0L
            );
        } else {
            window = glfwCreateWindow(
                    display.width(),
                    display.height(),
                    config.window_title,
                    monitor,0L);
        }

        if (window == 0L) {
            glfwTerminate();
            freeGLFWErrorCallback();
            throw new Exception("unable to create glfw window");
        }



    }



    private void freeGLFWErrorCallback() {
        try (GLFWErrorCallback errorCallback = glfwSetErrorCallback(null)) {
            if (errorCallback != null) errorCallback.free();
        }
    }

}
