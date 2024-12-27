package io.github.heathensoft.guide.core;

import org.joml.Vector2d;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

/**
 * use to query cursor position.
 * cursor coordinates are normalized window viewport coordinates 0,0 (bottom left) -> 1,1 (top right)
 * Frederik Dahl 12/26/2024
 */
public class Mouse {


    public static final int NUM_BUTTONS = 3;
    public static final int LEFT  = GLFW_MOUSE_BUTTON_LEFT;
    public static final int RIGHT = GLFW_MOUSE_BUTTON_RIGHT;
    public static final int WHEEL = GLFW_MOUSE_BUTTON_MIDDLE;

    private static final float FRAME_TIME = 0.01666667f;
    private static final float DRAG_TIME = 6 * FRAME_TIME;

    private final float[] timer = new float[NUM_BUTTONS];
    private final boolean[] current_dragging = new boolean[NUM_BUTTONS];
    private final boolean[] last_dragging = new boolean[NUM_BUTTONS];
    private final boolean[] last_button = new boolean[NUM_BUTTONS];
    private final boolean[] current_button = new boolean[NUM_BUTTONS];
    private final boolean[] callback_button = new boolean[NUM_BUTTONS];

    private final Vector2i window_size = new Vector2i();
    private final Vector2d delta_vector = new Vector2d();
    private final Vector2d last_position = new Vector2d();
    private final Vector2d current_position = new Vector2d();
    private final Vector2d callback_position = new Vector2d();
    private final Vector2d normalized_device = new Vector2d();

    private final Vector2d[] drag_origin = new Vector2d[NUM_BUTTONS];
    private final Vector2d[] drag_vector = new Vector2d[NUM_BUTTONS];

    private boolean cursor_in_window;
    private boolean cursor_just_left;
    private double callback_scroll;
    private double current_scroll;

    Mouse() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            drag_origin[i] = new Vector2d();
            drag_vector[i] = new Vector2d();
        } GLFWWindow window = Engine.get().window();
        window.cursorScreenPosition(last_position);
        screenToViewportCoordinates(last_position);
        clampToViewport(last_position);
        current_position.set(last_position);
        callback_position.sub(current_position);
    }


    protected void processInput(float delta) {

        last_position.set(current_position);

        if (cursor_in_window) {
            // Note: Good to know.
            // The cursor does not exit the window
            // if a button is still pressed.
            current_position.set(callback_position);
        } else if (cursor_just_left) {
            GLFWWindow window = Engine.get().window();
            window.cursorScreenPosition(current_position);
            screenToViewportCoordinates(current_position);
            cursor_just_left = false;
        }

        // cursor position is clamped to viewport
        clampToViewport(current_position);

        delta_vector.set(current_position).sub(last_position);
        normalized_device.set(current_position).mul(2).sub(1,1);
        current_scroll = callback_scroll;
        callback_scroll = 0;
        for (int b = 0; b < NUM_BUTTONS; b++) {
            last_dragging[b] = current_dragging[b];
            last_button[b] = current_button[b];
            current_button[b] = callback_button[b];
            if (current_button[b]) {
                timer[b] += delta;
                if (!last_button[b]) {
                    drag_origin[b].set(current_position);
                    drag_vector[b].zero();
                } else {
                    drag_vector[b].set(current_position).sub(drag_origin[b]);
                    if (!current_dragging[b]) {
                        if (timer[b] > DRAG_TIME && drag_vector[b].length() > 0.008) {
                            current_dragging[b] = true;
                        }
                    }
                }
            } else {
                timer[b] = 0f;
                if (current_dragging[b]) {
                    current_dragging[b] = false;
                    drag_vector[b].zero();
                }
            }
        }
    }

    // CALLBACKS ************************************************************

    protected void onCursorHover(double x, double y) {
        screenToViewportCoordinates(callback_position.set(x,y));
    }

    protected void onScroll(double amount) {
        callback_scroll += amount;
    }

    protected void onPress(int button, boolean press) {
        // we only care for three buttons (left, right, wheel)
        if (button >= 0 && button <= 3) {
            callback_button[button] = press;
        }
    }

    protected void onCursorEntered(boolean entered) {
        cursor_in_window = entered;
        cursor_just_left = !cursor_in_window;
    }


    // GETTERS **************************************************************

    public double scrollValue() {
        return current_scroll;
    }

    public Vector2d prevPosition() {
        return last_position;
    }

    public Vector2d position() {
        return current_position;
    }

    public Vector2d deltaVector() {
        return delta_vector;
    }

    public Vector2d ndc() {
        return normalized_device;
    }

    public Vector2d dragVector(int button) {
        return drag_vector[button];
    }

    public Vector2d dragOrigin(int button) {
        return drag_origin[button];
    }

    public boolean scrolled() {
        return current_scroll != 0;
    }

    public boolean cursorInsideWindow() {
        return cursor_in_window;
    }

    public boolean anyButtonPressed() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            if (current_button[i]) return true;
        } return false;
    }

    public boolean anyButtonDragging() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            if (current_dragging[i]) return true;
        } return false;
    }

    public boolean justClicked(int button) {
        return current_button[button] && !last_button[button];
    }

    public boolean justReleased(int button) {
        return !current_button[button] && last_button[button];
    }

    public boolean buttonPressed(int button) {
        return current_button[button];
    }

    public boolean isDragging(int button) {
        return current_dragging[button];
    }

    public boolean justStartedDrag(int button) {
        return current_dragging[button] && !last_dragging[button];
    }

    public boolean justReleasedDrag(int button) {
        return !current_dragging[button] && last_dragging[button];
    }


    private void clampToViewport(Vector2d cursor) {
        cursor.x = cursor.x > 1 ? 1 : cursor.x < 0 ? 0 : cursor.x;
        cursor.y = cursor.y > 1 ? 1 : cursor.y < 0 ? 0 : cursor.y;
    }

    private void screenToViewportCoordinates(Vector2d cursor) {
        GLFWWindow window = Engine.get().window();
        window.windowScreenSize(window_size);
        // Inverting y to bottom instead of top.
        cursor.y = window_size.y - cursor.y;
        // Framebuffer width and height should equal the size
        // of the windows content area as far as I know.
        // But just in case they ar not, I'm attempting to adjust.
        cursor.x *= ((double) window.framebufferW() / window_size.x);
        cursor.y *= ((double) window.framebufferH() / window_size.y);
        // Adjusting to window viewport and normalizing to [0-1] range.
        cursor.x = (cursor.x - window.viewportX()) / window.viewportW();
        cursor.y = (cursor.y - window.viewportY()) / window.viewportH();
    }
}
