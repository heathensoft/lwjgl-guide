package io.github.heathensoft.guide.core;

import io.github.heathensoft.guide.utils.OS;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_TRUE;

/**
 * Frederik Dahl 12/1/2024
 */
public class GLFWWindow {

    private Resolution game_resolution; // The game resolution
    private long window;                // glfw window handle
    private int target_fps;             // requested target FPS without vsync and fps limit
    private int target_ups;             // game logic updates per second
    private int framebuffer_width;      // width of the window framebuffer in pixels
    private int framebuffer_height;     // height of the window framebuffer in pixels
    private int viewport_width;         // width of the viewport
    private int viewport_height;        // height of the viewport
    private int viewport_position_x;    // viewport x position relative to the framebuffer in pixels
    private int viewport_position_y;    // viewport y position relative to the framebuffer in pixels
    private boolean minimized;          // whether the window is minimized
    private boolean vsync_enabled;      // limits fps to the display frame rate
    private boolean limit_fps;          // limits fps to the target fps if vsync is disabled


    public boolean isLimitFps() { return limit_fps; }
    public boolean isMinimized() { return minimized; }
    public boolean isVsyncEnabled() { return vsync_enabled; }
    public int targetFps() { return target_fps; }
    public int targetUps() { return target_ups; }
    public int framebufferWidth() { return framebuffer_width; }
    public int framebufferHeight() { return framebuffer_height; }
    public int viewportX() { return viewport_position_x; }
    public int viewportY() { return viewport_position_y; }
    public int viewportWidth() { return viewport_width; }
    public int viewportHeight() { return viewport_height; }
    public Resolution gameResolution() { return game_resolution; }

    public void pollEvents() {
        glfwPollEvents();
    }
    public void swapBuffers() {
        glfwSwapBuffers(window);
    }
    public boolean shouldClose() { return glfwWindowShouldClose(window); }


    public void initialize(BootConfiguration config) throws Exception {

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

        // Initialize GLFW. Most GLFW functions will not work before doing this.
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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(window,w,h);
            int window_width = w.get(0);
            int window_height = h.get(0);
            if (config.windowed_mode) {
                // center the window
                glfwSetWindowPos(window,
                        Math.round((display.width() - window_width) / 2f),
                        Math.round((display.height() - window_height) / 2f));
            }
            glfwGetWindowPos(window,w,h);
            int window_position_x = w.get(0);
            int window_position_y = h.get(0);
            glfwGetFramebufferSize(window,w,h);
            framebuffer_width = w.get(0);
            framebuffer_height = h.get(0);
            Logger.debug("created window: {},{},{},{}",
                    window_position_x,window_position_y,window_width,window_height);
            Logger.debug("framebuffer resolution: {}:{}",
                    framebuffer_width,framebuffer_height);
            DoubleBuffer mx = stack.mallocDouble(1);
            DoubleBuffer my = stack.mallocDouble(1);
            glfwGetCursorPos(window,mx,my);

        }

        Resolution framebuffer_resolution = new Resolution(framebuffer_width,framebuffer_height);
        Resolution.sortByClosest(framebuffer_resolution,config.supported_resolutions);
        game_resolution = config.supported_resolutions.getFirst();
        Logger.debug("game resolution: {}:{}",
                game_resolution.width(),game_resolution.height());
        fitViewport(framebuffer_resolution.width(),framebuffer_resolution.height());
        Logger.debug("window viewport: {},{},{},{}",
                viewport_position_x,viewport_position_y,viewport_width,viewport_height);

        initializeDisplayCallbacks();

        glfwMakeContextCurrent(window);
        Logger.debug("opengl-context current in thread: {}",
                Thread.currentThread().getName());

        glfwSetInputMode(window, GLFW_CURSOR,
                config.cursor_enabled ? GLFW_CURSOR_NORMAL: GLFW_CURSOR_DISABLED);

        limit_fps = config.limit_framerate;
        vsync_enabled = config.vsync_enabled;
        glfwSwapInterval(vsync_enabled ? 1 : 0);

        target_fps = Math.max(1,config.target_fps);
        target_ups = Math.max(1,config.target_ups);

        glfwShowWindow(window);
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
    }

    public void terminate() {
        Logger.debug("clearing opengl capabilities");
        GL.setCapabilities(null); // this IS nullable
        Logger.debug("freeing glfw display callbacks");
        freeDisplayCallbacks();
        Logger.debug("destroying the glfw window");
        glfwDestroyWindow(window);
        Logger.debug("terminating glfw");
        glfwTerminate();
        Logger.debug("freeing glfw error callback");
        freeGLFWErrorCallback();
    }





    private void fitViewport(int framebuffer_width, int framebuffer_height) {
        float game_aspect_ratio = game_resolution.aspect_ratio();
        viewport_width = framebuffer_width;
        viewport_height = Math.round(viewport_width / game_aspect_ratio);
        if (viewport_height > framebuffer_height) {
            viewport_height = framebuffer_height;
            viewport_width = Math.round(viewport_height * game_aspect_ratio);
        } viewport_position_x = Math.round((framebuffer_width / 2f) - (viewport_width / 2f));
        viewport_position_y = Math.round((framebuffer_height / 2f) - (viewport_height / 2f));
    }


    private void initializeDisplayCallbacks() {

        glfwSetWindowIconifyCallback(window, new GLFWWindowIconifyCallback() {
            public void invoke(long window, boolean iconified) {
                minimized = iconified;
            }
        });

        glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                framebuffer_width = width;
                framebuffer_height = height;
                fitViewport(width,height);
            }
        });

        glfwSetMonitorCallback(new GLFWMonitorCallback() {
            /*
            If a monitor is disconnected, all windows that are full screen on it
            will be switched to windowed mode before the callback is called.
            Only glfwGetMonitorName and glfwGetMonitorUserPointer will return useful values
            for a disconnected monitor and only before the monitor callback returns.
             */
            public void invoke(long monitor, int event) {
                if (event == GLFW_DISCONNECTED) {
                    Logger.debug("monitor: {}, disconnected");
                } else if (event == GLFW_CONNECTED) {
                    Logger.debug("monitor: {}, connected");
                }
            }
        });
    }

    private void freeDisplayCallbacks() {
        List<Callback> list = new ArrayList<>();
        list.add(glfwSetMonitorCallback(null));
        list.add(glfwSetWindowSizeCallback(window,null));
        list.add(glfwSetWindowPosCallback(window,null));
        list.add(glfwSetWindowIconifyCallback(window,null));
        list.add(glfwSetFramebufferSizeCallback(window,null));
        for (Callback c : list) if (c != null) c.free();
    }

    private void freeGLFWErrorCallback() {
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) errorCallback.free();
    }



}
