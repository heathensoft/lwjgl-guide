package io.github.heathensoft.guide.core;

import org.joml.Vector2d;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Use to query cursor position
 * cursor coordinates are normalized window viewport coordinates 0,0 (bottom left) -> 1,1 (top right)
 * Frederik Dahl 12/26/2024
 */
public class Mouse {


    public static final int LEFT  = GLFW_MOUSE_BUTTON_LEFT;
    public static final int RIGHT = GLFW_MOUSE_BUTTON_RIGHT;
    public static final int WHEEL = GLFW_MOUSE_BUTTON_MIDDLE;

    private static final float FRAME_TIME = 0.01666667f;
    private static final float DRAG_TIME = 6 * FRAME_TIME;
    private static final int NUM_BUTTONS = 3;

    private final float[] timer = new float[NUM_BUTTONS];
    private final boolean[] current_dragging = new boolean[NUM_BUTTONS];
    private final boolean[] last_dragging = new boolean[NUM_BUTTONS];
    private final boolean[] last_button = new boolean[NUM_BUTTONS];
    private final boolean[] current_button = new boolean[NUM_BUTTONS];
    private final boolean[] callback_button = new boolean[NUM_BUTTONS];

    private final Vector2d delta_vector = new Vector2d();
    private final Vector2d last_position = new Vector2d();
    private final Vector2d current_position = new Vector2d();
    private final Vector2d callback_position = new Vector2d();
    private final Vector2d normalized_device = new Vector2d();

    private final Vector2d[] drag_origin = new Vector2d[NUM_BUTTONS];
    private final Vector2d[] drag_vector = new Vector2d[NUM_BUTTONS];

    private boolean mouse_left_window;
    private double callback_scroll;
    private double current_scroll;

    Mouse(Vector2d cursor) {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            drag_origin[i] = new Vector2d();
            drag_vector[i] = new Vector2d();
        } last_position.set(cursor);
        current_position.set(last_position);
        callback_position.sub(current_position);
    }


    protected void processInput(float delta) {
        last_position.set(current_position);
        current_position.set(callback_position);
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
                    Vector2d d_vec = drag_vector[b];
                    d_vec.set(current_position).sub(drag_origin[b]);
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

    // CALLBACKS

    protected void onCursorHover(double x, double y) { callback_position.set(x,y); }

    protected void onScroll(double amount) { callback_scroll += amount; }

    protected void onPress(int button, boolean press) { callback_button[button] = press; }

    protected void onCursorEntered(boolean entered) { /* no use for this yet */ }

    public double scrollValue() {
        return current_scroll;
    }


    // GETTERS

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

    public Vector2d dragOrigin(int button) { return drag_origin[button]; }

    public boolean scrolled() {
        return current_scroll != 0;
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


}
