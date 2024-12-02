package io.github.heathensoft.guide.core;

import org.tinylog.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration used to initialize the GLFW Window
 * Frederik Dahl 12/1/2024
 */
public class BootConfiguration {

    /**
     * By default, the logger prints all severity levels to console.
     * There are many options, and they are easy to configure (See reference link)
     * @param key key
     * @param value value
     * @see <a href="https://tinylog.org/v2/configuration/">tinylog</a>
     */
    public void logger(final String key, final String value) {
        Configuration.set(key, value);
    }

    public List<Resolution> supported_resolutions = new ArrayList<>();
    public String window_title = "";
    public boolean resizable_window = false;
    public boolean vsync_enabled = true;
    public boolean limit_framerate = false;
    public boolean cursor_enabled = true;
    public boolean windowed_mode = false;
    public int windowed_mode_width = 1280;
    public int windowed_mode_height = 720;
    public int target_ups = 60;
    public int target_fps = 60;


}
