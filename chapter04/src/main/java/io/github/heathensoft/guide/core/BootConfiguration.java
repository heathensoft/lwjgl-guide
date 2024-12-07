package io.github.heathensoft.guide.core;


import java.util.ArrayList;
import java.util.List;

/**
 * Configuration used to initialize the GLFW Window
 * Frederik Dahl 12/1/2024
 */
public class BootConfiguration {
    public final List<Resolution> supported_resolutions = new ArrayList<>();
    public String window_title = "";
    public boolean resizable_window = false;
    public boolean vsync_enabled = true;
    public boolean cursor_enabled = true;
    public boolean windowed_mode = false;
    public int windowed_mode_width = 1280;
    public int windowed_mode_height = 720;
    public int target_ups = 60;
}
