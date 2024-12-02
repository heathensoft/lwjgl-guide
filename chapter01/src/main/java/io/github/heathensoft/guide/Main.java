package io.github.heathensoft.guide;

import io.github.heathensoft.guide.core.BootConfiguration;
import io.github.heathensoft.guide.core.GLFWWindow;
import io.github.heathensoft.guide.core.Resolution;
import org.tinylog.Logger;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Frederik Dahl 12/2/2024
 */
public class Main {

    public void run() {

        // Initialize our GLFWWindow
        GLFWWindow window = new GLFWWindow();
        try { BootConfiguration configuration = new BootConfiguration();
            configuration.window_title = "lwjgl-guide";
            configuration.supported_resolutions.add(Resolution.R_1280x720);
            configuration.supported_resolutions.add(Resolution.R_1920x1080);
            configuration.windowed_mode = true;
            configuration.resizable_window = true;
            configuration.vsync_enabled = true;
            org.tinylog.configuration.Configuration.set("writer","console");
            org.tinylog.configuration.Configuration.set("writer.format","{date: HH:mm:ss.SS} {level}: {message}");
            window.initialize(configuration);
        } catch (Exception e) {
            Logger.error(e);
            System.exit(0);
        }
        // We have not yet configured callbacks for user input events
        // We'll cover user input in future chapters
        int escape_key_prev = GLFW_RELEASE; // escape key state from the previous frame
        int toggle_key_prev = GLFW_RELEASE; // toggle key state from the previous frame (F1)

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f); // set clear color to RED

        // Loop until the window closes:
        while (!window.shouldClose()) {
            if (!window.isMinimized()) {

                // ESCAPE exits the program and F1 toggles between fullscreen and windowed mode
                int escape_key_state = glfwGetKey(window.handle(),GLFW_KEY_ESCAPE);
                int toggle_key_state = glfwGetKey(window.handle(),GLFW_KEY_F1);
                if (escape_key_state == GLFW_PRESS && escape_key_prev == GLFW_RELEASE) {
                    window.signalToClose();
                }  else if (toggle_key_state == GLFW_PRESS && toggle_key_prev == GLFW_RELEASE) {
                    if (window.isWindowedMode()) window.fullScreen();
                    else window.windowedMode(Resolution.R_1280x720);
                } escape_key_prev = escape_key_state;
                toggle_key_prev = toggle_key_state;


                // Define the render area
                glViewport(window.viewportX(),window.viewportY(),window.viewportW(),window.viewportH());
                // clear the windows back buffer to RED
                glClear(GL_COLOR_BUFFER_BIT);

                // ---> DRAW OPERATIONS HERE <---

                //When the entire frame has been rendered,
                //it is time to swap the back and the front buffers in order to
                //display what has been rendered and begin rendering a new frame
                //(Swapping the windows' front and back buffers)
                window.swapRenderBuffers();
            } window.processUserEvents();
        } window.terminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
