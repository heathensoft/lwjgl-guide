package io.github.heathensoft.guide.core;

import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Frederik Dahl 12/26/2024
 */
public class InputProcessor {

    private final GLFWWindow window;
    private final Mouse mouse;
    private final Keyboard keys;
    //private Controller controller;

    public Mouse mouse() { return mouse; }
    public Keyboard keys() { return keys; }
    //public Controller controller() { return controller; }


    InputProcessor(GLFWWindow window) {

        this.window = window;
        this.mouse = new Mouse();
        this.keys = new Keyboard();
    }


    protected void process(float delta) {
        keys.processInput();
        mouse.processInput(delta);
    }

    protected void onKeyEvent(int key, int mods, int action) {
        if (key != GLFW_KEY_UNKNOWN && key < GLFW_KEY_LAST) {
            key = action != GLFW_RELEASE ? key : -key;
            keys.onKeyEvent(key,mods,action);
        }
    }

    protected void onCharPress(int codepoint) {
        // filtering out characters outside the ascii range (> 127)
        // only ascii characters goes through (remapping norwegian letters)
        if ((codepoint & 0x7F) == codepoint) keys.onCharPress(codepoint);
        else if (codepoint == 230) keys.onCharPress(101); // æ -> e
        else if (codepoint == 248) keys.onCharPress(111); // ø -> o
        else if (codepoint == 229) keys.onCharPress(97);  // å -> a
    }

    protected void onMouseEntered(boolean entered) {
        mouse.onCursorEntered(entered);
    }

    protected void onMouseHover(double x, double y) {
        Vector2i window_size = window.windowScreenSize();
        y = window_size.x - y; // inverting y0 to be the bottom instead of top.
        // framebuffer width and height should equal the size
        // of the windows content area as far as I know.
        // But just in case they ar not, I'm attempting to adjust.
        // I have never experienced any issues with this.
        x *= ((double) window.framebufferW() / window_size.x);
        y *= ((double) window.framebufferH() / window_size.y);
        // Adjusting to window viewport and normalizing to [0-1] range.
        x = (x - window.viewportX()) / window.viewportW();
        y = (y - window.viewportY()) / window.viewportH();
        mouse.onCursorHover(x,y);
    }

    protected void onMousePress(int button, boolean press) {
        // We only care about three buttons (left, right, wheel)
        if (button >= 0 && button <= 3) {
            mouse.onPress(button,press);
        }
    }

    protected void onMouseScroll(double amount) {

    }



}
