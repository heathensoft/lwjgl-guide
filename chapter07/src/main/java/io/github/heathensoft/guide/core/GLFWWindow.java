package io.github.heathensoft.guide.core;

import io.github.heathensoft.guide.utils.OS;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glViewport;

/**
 * Frederik Dahl 12/1/2024
 */
public final class GLFWWindow {

    public static final int UPS_MIN = 30;
    public static final int UPS_MAX = 1000;

    private List<Resolution> supported_resolutions; // resolutions supported by our game
    private Resolution game_resolution; // The current resolution
    private boolean game_resolution_changed; // found a better supported resolution for the game
    private long window;                // glfw window pointer
    private int target_ups;             // game logic updates per second
    private int framebuffer_w;          // width of the window framebuffer in pixels
    private int framebuffer_h;          // height of the window framebuffer in pixels
    private int viewport_x;             // viewport x position relative to the framebuffer in pixels
    private int viewport_y;             // viewport y position relative to the framebuffer in pixels
    private int viewport_w;             // width of the viewport
    private int viewport_h;             // height of the viewport
    private boolean minimized;          // whether the window is minimized
    private boolean vsync_enabled;      // limits fps to the display frame rate

    public long handle() { return window; }
    public int targetUps() { return target_ups; }
    public int framebufferW() { return framebuffer_w; }
    public int framebufferH() { return framebuffer_h; }
    public int viewportX() { return viewport_x; }
    public int viewportY() { return viewport_y; }
    public int viewportW() { return viewport_w; }
    public int viewportH() { return viewport_h; }
    public boolean isMinimized() { return minimized; }
    public boolean isVsyncEnabled() { return vsync_enabled; }


    void initialize(BootConfiguration config) throws Exception {
        if (config.supported_resolutions.isEmpty()) throw new Exception("the application failed provide resolution options");
        supported_resolutions = new ArrayList<>(config.supported_resolutions);

        glfwSetErrorCallback(new GLFWErrorCallback() {
            public void invoke(int error, long description) {
                if (error != GLFW_NO_ERROR) {
                    Logger.error("GLFW ERROR[{}]: {}", error,GLFWErrorCallback.getDescription(description));
                }
            }
        });
        if (!glfwInit()) {
            freeGLFWErrorCallback();
            throw new Exception("unable to initialize glfw");
        } Logger.debug("initialized glfw");

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
        } {
            String monitor_name = glfwGetMonitorName(monitor);
            monitor_name = monitor_name == null ? "NULL" : monitor_name;
            Logger.debug("primary monitor: {} - {}:{} {}hz",
                    monitor_name, display.width(),display.height(),display.refreshRate());

        }
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
            Logger.debug("using forward compatibility for mac user");
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        } else {
            Logger.debug("using opengl core profile for non-mac user");
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        }
        if (config.windowed_mode) {
            window = glfwCreateWindow(
                    config.windowed_mode_width,
                    config.windowed_mode_height,
                    config.window_title,0L,0L );
        } else window = glfwCreateWindow(
                    display.width(),
                    display.height(),
                    config.window_title,
                    monitor,0L);

        if (window == 0L) {
            glfwTerminate();
            freeGLFWErrorCallback();
            throw new Exception("unable to create glfw window");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(window,w,h);
            int window_w = w.get(0);
            int window_h = h.get(0);
            if (config.windowed_mode) { // center the window
                glfwSetWindowPos(window,
                        Math.round((display.width() -  window_w) / 2f),
                        Math.round((display.height() - window_h) / 2f));
            }
            glfwGetWindowPos(window,w,h);
            int window_x = w.get(0);
            int window_y = h.get(0);
            glfwGetFramebufferSize(window,w,h);
            framebuffer_w = w.get(0);
            framebuffer_h = h.get(0);
            Logger.debug("created window: {},{},{}:{}",
                    window_x,window_y,window_w,window_h);
            Logger.debug("framebuffer resolution: {}:{}",
                    framebuffer_w, framebuffer_h);

        }
        framebufferResizeEvent(framebuffer_w,framebuffer_h);
        Logger.debug("window viewport: {},{},{}:{}", viewport_x, viewport_y, viewport_w, viewport_h);
        initializeDisplayCallbacks();
        glfwMakeContextCurrent(window);
        Logger.debug("opengl-context current in thread: {}", Thread.currentThread().getName());
        glfwSetInputMode(window, GLFW_CURSOR, config.cursor_enabled ? GLFW_CURSOR_NORMAL: GLFW_CURSOR_DISABLED);
        vsync_enabled = config.vsync_enabled;
        glfwSwapInterval(vsync_enabled ? 1 : 0);
        setTargetUPS(config.target_ups);
        glfwShowWindow(window);
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
    }

    public void toggleMonitors() {
        // todo: switch to the next available connected monitor
    }

    public void windowedMode(int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer w_buffer = stack.mallocInt(1);
            IntBuffer h_buffer = stack.mallocInt(1);
            glfwGetWindowSize(window,w_buffer,h_buffer);
            int current_window_w = w_buffer.get(0);
            int current_window_h = h_buffer.get(0);
            int new_window_x = Math.round((current_window_w - width) / 2f);
            int new_window_y = Math.round((current_window_h - height) / 2f);
            if (isWindowedMode()) { // centered
                glfwSetWindowSize(window, width, height);
                glfwSetWindowPos(window,new_window_x,new_window_y);
            } else glfwSetWindowMonitor(window,0L,new_window_x,new_window_y, width, height,GLFW_DONT_CARE);
        }
    }

    public void windowedMode(Resolution resolution) {
        windowedMode(resolution.width(),resolution.height());
    }

    /** If windowed: Set the window to fullscreen on the primary monitor */
    public void fullScreen() {
        long monitor = glfwGetWindowMonitor(window);
        if (monitor == 0L) { // windowed
            monitor = glfwGetPrimaryMonitor();
            if (monitor != 0L) {
                GLFWVidMode display = glfwGetVideoMode(monitor);
                if (display != null) {
                    glfwSetWindowMonitor(window,monitor,0,0,
                    display.width(),display.height(),display.refreshRate());
                }
            }
        }
    }


    /**
     * Query input / display events.
     * Processing events will cause the window and input callbacks associated with those events to be called.
     */
    void processUserEvents() {
        glfwPollEvents();
        // todo: controller
    }

    /**
     * GLFW windows are by default double buffered.
     * That means that you have two rendering buffers; a front buffer and a back buffer.
     * The front buffer is the one being displayed and the back buffer the one you render to.
     * When the entire frame has been rendered, it is time to swap the back and the front buffers
     * in order to display what has been rendered and begin rendering a new frame.
     */
    void swapRenderBuffers() { glfwSwapBuffers(window); }

    public boolean isWindowedMode() { return glfwGetWindowMonitor(window) == 0L; }

    /** @return true if the window has been signaled to close */
    public boolean shouldClose() { return glfwWindowShouldClose(window); }

    /**
     * The current resolution of the game. Will always be the closest matching of the supported resolutions.
     * The actual window framebuffer resolution is not necessarily equal to the game resolution.
     * But the game logic does not need to worry about that.
     * @return current game resolution
     */
    public Resolution gameResolution() { return game_resolution; }

    /**
     * Note: Will reset the game_resolution_changed flag
     * @return true if the window has found a better suited resolution for the game
     */
    boolean shouldChangeGameResolution() {
        if (game_resolution_changed) {
            game_resolution_changed = false;
            return true;
        } return false;
    }

    public void setTargetUPS(int target_ups) { this.target_ups = Math.clamp(target_ups,UPS_MIN,UPS_MAX); }

    void signalToClose() {
        Logger.info("window signalled to close");
        glfwSetWindowShouldClose(window,true); }
    public void show() { glfwShowWindow(window); }
    public void hide() { glfwHideWindow(window); }
    public void focus() { glfwFocusWindow(window); }
    public void maximize() { glfwMaximizeWindow(window); }
    public void minimize() { glfwIconifyWindow(window); }
    public void restore() { glfwRestoreWindow(window); }
    public void toggleVsync(boolean enable) { vsync_enabled = enable; }
    public void useWindowViewport() { glViewport(viewport_x,viewport_y,viewport_w,viewport_h); }

    @SuppressWarnings("all")
    void terminate() {
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

    private void framebufferResizeEvent(int framebuffer_w, int framebuffer_h) {
        this.framebuffer_w = framebuffer_w; this.framebuffer_h = framebuffer_h;
        Resolution framebuffer_resolution = new Resolution(framebuffer_w,framebuffer_h);
        Resolution.sortByClosest(framebuffer_resolution, supported_resolutions);
        Resolution closest_resolution = supported_resolutions.getFirst();
        if (!closest_resolution.equals(game_resolution)) {
            Logger.debug("game resolution change: {} -> {}", game_resolution, closest_resolution);
            game_resolution = closest_resolution;
            game_resolution_changed = true;
        } fitViewport(framebuffer_w,framebuffer_h);
    }

    private void fitViewport(int framebuffer_w, int framebuffer_h) {
        float game_aspect_ratio = game_resolution.aspectRatio();
        viewport_w = framebuffer_w;
        viewport_h = Math.round(viewport_w / game_aspect_ratio);
        if (viewport_h > framebuffer_h) {
            viewport_h = framebuffer_h;
            viewport_w = Math.round(viewport_h * game_aspect_ratio);
        } viewport_x = Math.round((framebuffer_w / 2f) - (viewport_w / 2f));
        viewport_y = Math.round((framebuffer_h / 2f) - (viewport_h / 2f));
    }


    private void initializeDisplayCallbacks() {
        glfwSetWindowIconifyCallback(window, new GLFWWindowIconifyCallback() {
            public void invoke(long window, boolean iconified) {
                minimized = iconified;
            }
        });
        glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                framebufferResizeEvent(width,height);
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
                    Logger.debug("monitor: {}, disconnected", monitor);
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

    @SuppressWarnings("resource")
    private void freeGLFWErrorCallback() {
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) errorCallback.free();
    }



}
