package io.github.heathensoft.guide.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration used to initialize the GLFW Window
 * Frederik Dahl 12/1/2024
 */
public class BootConfiguration {

    public List<Resolution> supported_resolutions = new ArrayList<>();
    public String window_title = "";
    public boolean resizable_window = false;
    public boolean windowed_mode = false;
    public boolean vsync_enabled = true;
    public boolean cursor_enabled = true;
    public int target_ups = 60;
    public int target_fps = 60;
}
