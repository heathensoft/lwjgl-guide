package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.*;
import io.github.heathensoft.guide.utils.Disposable;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

/**
 * Frederik Dahl 12/5/2024
 */
public class Game implements IGame {

    public static void main(String[] args) {
        Engine.get().run(new Game(),args);
    }

    public static final int game_res_w = 1200;
    public static final int game_res_h = 800;
    private RendererTest renderer;
    private int escape_key_prev = GLFW_RELEASE; // ESCAPE key state from the previous frame
    private int f1_key_prev = GLFW_RELEASE;     // F1 key state from the previous frame

    public void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "lwjgl-guide";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = true;
    }

    public void start(Resolution resolution) throws Exception {
        renderer = new RendererTest();
    }

    public void resize(Resolution resolution) { /* */ }

    public void update(float delta_time) {
        //Engine.Time time = Engine.get().time();
        //System.out.println("FPS: " + time.framesPerSecond() + " - UPS: " + time.updatesPerSecond());
        GLFWWindow window = Engine.get().window();
        int escape_key_state = glfwGetKey(window.handle(),GLFW_KEY_ESCAPE);
        int f1_key_state = glfwGetKey(window.handle(),GLFW_KEY_F1);
        if (escape_key_state == GLFW_PRESS && escape_key_prev == GLFW_RELEASE) {
            Engine.get().exitMainLoop();
        } else if (f1_key_state == GLFW_PRESS && f1_key_prev == GLFW_RELEASE) {
            if (window.isWindowedMode()) window.fullScreen();
            else window.windowedMode(game_res_w,game_res_h);
        } escape_key_prev = escape_key_state;
        f1_key_prev = f1_key_state;
    }

    public void render() {
        Engine.get().window().useWindowViewport();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderer.draw();
    }

    public void exit() {
        Disposable.dispose(renderer);
    }
}
