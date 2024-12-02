package io.github.heathensoft.guide;

import io.github.heathensoft.guide.core.BootConfiguration;
import io.github.heathensoft.guide.core.GLFWWindow;
import io.github.heathensoft.guide.core.Resolution;
import org.tinylog.Logger;

import static org.lwjgl.opengl.GL11.*;

/**
 * Frederik Dahl 12/2/2024
 */
public class Main {

    public void run() {

        GLFWWindow window = new GLFWWindow();

        try {
            BootConfiguration configuration = new BootConfiguration();
            configuration.window_title = "lwjgl guide";
            configuration.supported_resolutions.add(Resolution.R_1280x720);
            configuration.supported_resolutions.add(Resolution.R_1920x1080);
            configuration.windowed_mode = true;
            configuration.resizable_window = true;
            configuration.vsync_enabled = true;
            configuration.logger("writer","console");
            configuration.logger("writer.format","{date: HH:mm:ss.SS} {level}: {message}");
            window.initialize(configuration);
        } catch (Exception e) {
            Logger.error(e);
            System.exit(0);
        }

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        while (!window.shouldClose()) {
            if (!window.isMinimized()) {
                glViewport(
                        window.viewportX(),
                        window.viewportY(),
                        window.viewportWidth(),
                        window.viewportHeight()
                );
                glClear(GL_COLOR_BUFFER_BIT); // clear the framebuffer

                // --> DRAW OPERATIONS HERE <--

                window.swapBuffers();
                window.pollEvents();
            }
        }

        window.terminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }


}
