package io.github.heathensoft.jagfw.core;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Frederik Dahl 1/9/2025
 */
public class Controller implements Disposable {

    public static final int SLOT_0 = 0;
    public static final int SLOT_1 = 1;
    public static final int SLOT_2 = 2;
    public static final int SLOT_3 = 3;
    public static final int SLOT_LAST = SLOT_3;
    public static final int SLOT_COUNT = 4;

    public static final int BUTTON_A = GLFW_GAMEPAD_BUTTON_A;
    public static final int BUTTON_B = GLFW_GAMEPAD_BUTTON_B;
    public static final int BUTTON_X = GLFW_GAMEPAD_BUTTON_X;
    public static final int BUTTON_Y = GLFW_GAMEPAD_BUTTON_Y;
    public static final int BUTTON_CROSS = GLFW_GAMEPAD_BUTTON_CROSS;
    public static final int BUTTON_CIRCLE = GLFW_GAMEPAD_BUTTON_CIRCLE;
    public static final int BUTTON_SQUARE = GLFW_GAMEPAD_BUTTON_SQUARE;
    public static final int BUTTON_TRIANGLE = GLFW_GAMEPAD_BUTTON_TRIANGLE;
    public static final int BUTTON_LEFT_BUMPER = GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
    public static final int BUTTON_RIGHT_BUMPER = GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
    public static final int BUTTON_BACK = GLFW_GAMEPAD_BUTTON_BACK;
    public static final int BUTTON_START = GLFW_GAMEPAD_BUTTON_START;
    public static final int BUTTON_GUIDE = GLFW_GAMEPAD_BUTTON_GUIDE;
    public static final int BUTTON_LEFT_THUMB = GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
    public static final int BUTTON_RIGHT_THUMB = GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
    public static final int BUTTON_DPAD_UP = GLFW_GAMEPAD_BUTTON_DPAD_UP;
    public static final int BUTTON_DPAD_RIGHT = GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
    public static final int BUTTON_DPAD_DOWN = GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
    public static final int BUTTON_DPAD_LEFT = GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
    public static final int BUTTON_LAST = GLFW_GAMEPAD_BUTTON_DPAD_LEFT;

    private static final int AXIS_LEFT_X = GLFW_GAMEPAD_AXIS_LEFT_X;
    private static final int AXIS_LEFT_Y = GLFW_GAMEPAD_AXIS_LEFT_Y;
    private static final int AXIS_RIGHT_X = GLFW_GAMEPAD_AXIS_RIGHT_X;
    private static final int AXIS_RIGHT_Y = GLFW_GAMEPAD_AXIS_RIGHT_Y;
    private static final int AXIS_LEFT_TRIGGER = GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
    private static final int AXIS_RIGHT_TRIGGER = GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
    private static final int AXIS_LAST = AXIS_RIGHT_TRIGGER;

    private static final float STICK_DEAD_ZONE = 0.2f;
    private static final int STICK_LEFT = 16;
    private static final int STICK_RIGHT = 17;

    private static final float TRIGGER_DEAD_ZONE = 0.15f;
    private static final int TRIGGER_LEFT = 18;
    private static final int TRIGGER_RIGHT = 19;


    private final GamePad[] gamepads = new GamePad[SLOT_COUNT];
    private int active_slot = SLOT_0;
    private int num_connected = 0;


    Controller() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (glfwJoystickPresent(slot)) {
                String name = glfwGetGamepadName(slot);
                Logger.debug("controller connected: [{}] \"{}\"",slot,name);
                gamepads[slot] = new GamePad(slot,name);
                num_connected++;
            }
        }
    }

    protected void processInput(float dt) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            boolean p_connected = gamepads[slot] != null;
            boolean c_connected = glfwJoystickIsGamepad(slot);
            if (c_connected &! p_connected) { // CONNECT
                String name = glfwGetGamepadName(slot);
                Logger.debug("controller connected: [{}] \"{}\"",slot,name);
                gamepads[slot] = new GamePad(slot,name);
                num_connected++;
            } else if (p_connected &! c_connected) { // DISCONNECT
                String name = gamepads[slot].name;
                Logger.debug("controller disconnected: [{}] \"{}\"",slot,name);
                gamepads[slot].dispose();
                gamepads[slot] = null;
                num_connected--;
            } else if (gamepads[slot] != null) { // UPDATE STATE
                gamepads[slot].updateState(dt);
            }
        }
    }


    public void setActiveSlot(int slot) {
        if (active_slot != slot) {
            if (isValidSlot(slot)) {
                active_slot = slot;
            }
        }
    }

    public int activeSlot() {
        return active_slot;
    }

    public int numConnected() {
        return num_connected;
    }

    public boolean anyConnected() {
        return num_connected > 0;
    }

    public boolean isConnected() {
        return gamepads[active_slot] != null;
    }

    public boolean isConnected(int slot) {
        if (isValidSlot(slot)) {
            return gamepads[slot] != null;
        } return false;
    }

    public boolean buttonPressed(int button) {
        if (isConnected() && isValidButton(button)) {
            return gamepads[button].currentlyPressed(button);
        } return false;
    }

    public boolean buttonJustPressed(int button) {
        if (isConnected() && isValidButton(button)) {
            return gamepads[button].justPressed(button);
        } return false;
    }

    public boolean buttonJustReleased(int button) {
        if (isConnected() && isValidButton(button)) {
            return gamepads[button].justReleased(button);
        } return false;
    }

    public String name() {
        if (isConnected()) {
            return gamepads[active_slot].name;
        } return null;
    }

    public String name(int slot) {
        if (isConnected(slot)) {
            return gamepads[slot].name;
        } return null;
    }

    public boolean leftStickPushed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(STICK_LEFT);
        } return false;
    }

    public boolean leftStickJustPushed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(STICK_LEFT);
        } return false;
    }

    public boolean leftStickJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(STICK_LEFT);
        } return false;
    }

    public boolean rightStickPushed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(STICK_RIGHT);
        } return false;
    }

    public boolean rightStickJustPushed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(STICK_RIGHT);
        } return false;
    }

    public boolean rightStickJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(STICK_RIGHT);
        } return false;
    }

    public Vector2f leftStickDirection() {
        if (isConnected()) {
            return gamepads[active_slot].c_stick_l_dir;
        } return null;
    }

    public Vector2f rightStickDirection() {
        if (isConnected()) {
            return gamepads[active_slot].c_stick_r_dir;
        } return null;
    }

    public float leftStickMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_stick_l_magnitude;
        } return 0;
    }

    public float rightStickMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_stick_r_magnitude;
        } return 0;
    }

    public boolean leftTriggerPressed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(TRIGGER_LEFT);
        } return false;
    }

    public boolean leftTriggerJustPressed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(TRIGGER_LEFT);
        } return false;
    }

    public boolean leftTriggerJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(TRIGGER_LEFT);
        } return false;
    }

    public float leftTriggerMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_trigger_l_magnitude;
        } return 0;
    }

    public boolean rightTriggerPressed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(TRIGGER_RIGHT);
        } return false;
    }

    public boolean rightTriggerJustPressed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(TRIGGER_RIGHT);
        } return false;
    }

    public boolean rightTriggerJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(TRIGGER_RIGHT);
        } return false;
    }

    public float rightTriggerMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_trigger_r_magnitude;
        } return 0;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot <= SLOT_LAST;
    }

    private static boolean isValidButton(int button) {
        return button >= 0 && button <= BUTTON_LAST;
    }

    @Override
    public void dispose() {
        for (GamePad gamepad : gamepads) {
            Disposable.dispose(gamepad);
        }
    }

    @FunctionalInterface
    public interface ConnectionListener {
        void invoke(String name, int slot, boolean connected);
    }

    private static final class GamePad implements Disposable {

        int slot;
        int c_buttons_state;
        int p_buttons_state;
        final String name;
        final ByteBuffer buffer;
        final GLFWGamepadState state;
        float c_trigger_l_magnitude;
        float c_trigger_r_magnitude;
        float p_trigger_l_magnitude;
        float p_trigger_r_magnitude;
        float c_stick_l_magnitude;
        float c_stick_r_magnitude;
        float p_stick_l_magnitude;
        float p_stick_r_magnitude;
        final Vector2f c_stick_l_dir = new Vector2f();
        final Vector2f c_stick_r_dir = new Vector2f();
        final Vector2f p_stick_l_dir = new Vector2f();
        final Vector2f p_stick_r_dir = new Vector2f();

        GamePad(int slot, String name) {
            this.slot = slot;
            this.name = name;
            this.buffer = MemoryUtil.memAlloc(GLFWGamepadState.SIZEOF);
            this.state = new GLFWGamepadState(buffer);
        }

        void updateState(float dt) {

            p_buttons_state = c_buttons_state;
            p_stick_l_dir.set(c_stick_l_dir);
            p_stick_r_dir.set(c_stick_r_dir);
            p_trigger_l_magnitude = c_trigger_l_magnitude;
            p_trigger_r_magnitude = c_trigger_r_magnitude;
            p_stick_l_magnitude = c_stick_l_magnitude;
            p_stick_r_magnitude = c_stick_r_magnitude;

            c_buttons_state = 0;
            c_stick_l_dir.zero();
            c_stick_r_dir.zero();
            c_stick_l_magnitude = 0f;
            c_stick_r_magnitude = 0f;
            c_trigger_l_magnitude = 0f;
            c_trigger_r_magnitude = 0f;

            if (glfwGetGamepadState(slot,state)) {

                for (int btn = 0; btn <= BUTTON_LAST; btn++) {
                    if (state.buttons(btn) == GLFW_PRESS) {
                        c_buttons_state |= (1 << btn);
                    }
                }

                c_stick_l_dir.x = state.axes(AXIS_LEFT_X);
                c_stick_l_dir.y = -state.axes(AXIS_LEFT_Y);
                c_stick_l_magnitude = c_stick_l_dir.length();
                if (c_stick_l_magnitude > STICK_DEAD_ZONE) {
                    c_stick_l_dir.x /= c_stick_l_magnitude;
                    c_stick_l_dir.y /= c_stick_l_magnitude;
                    c_stick_l_magnitude = Math.min(c_stick_l_magnitude,1.0f);
                    c_stick_l_magnitude -= STICK_DEAD_ZONE;
                    c_stick_l_magnitude /= (1.0f - STICK_DEAD_ZONE);
                    c_buttons_state |= (1 << STICK_LEFT);
                } else c_stick_l_magnitude = 0f;

                c_stick_r_dir.x = state.axes(AXIS_RIGHT_X);
                c_stick_r_dir.y = -state.axes(AXIS_RIGHT_Y);
                c_stick_r_magnitude = c_stick_r_dir.length();
                if (c_stick_r_magnitude > STICK_DEAD_ZONE) {
                    c_stick_r_dir.x /= c_stick_r_magnitude;
                    c_stick_r_dir.y /= c_stick_r_magnitude;
                    c_stick_r_magnitude = Math.min(c_stick_r_magnitude,1.0f);
                    c_stick_r_magnitude -= STICK_DEAD_ZONE;
                    c_stick_r_magnitude /= (1.0f - STICK_DEAD_ZONE);
                    c_buttons_state |= (1 << STICK_RIGHT);
                } else c_stick_r_magnitude = 0f;

                c_trigger_l_magnitude = state.axes(AXIS_LEFT_TRIGGER);
                c_trigger_l_magnitude = U.clamp(c_trigger_l_magnitude,-1f,1f);
                c_trigger_l_magnitude = (c_trigger_l_magnitude + 1f) / 2f;
                if (c_trigger_l_magnitude > TRIGGER_DEAD_ZONE) {
                    c_trigger_l_magnitude -= TRIGGER_DEAD_ZONE;
                    c_trigger_l_magnitude /= (1.0f - TRIGGER_DEAD_ZONE);
                    c_buttons_state |= (1 << TRIGGER_LEFT);
                } else c_trigger_l_magnitude = 0f;

                c_trigger_r_magnitude = state.axes(AXIS_RIGHT_TRIGGER);
                c_trigger_r_magnitude = U.clamp(c_trigger_r_magnitude,-1f,1f);
                c_trigger_r_magnitude = (c_trigger_r_magnitude + 1f) / 2f;
                if (c_trigger_r_magnitude > TRIGGER_DEAD_ZONE) {
                    c_trigger_r_magnitude -= TRIGGER_DEAD_ZONE;
                    c_trigger_r_magnitude /= (1.0f - TRIGGER_DEAD_ZONE);
                    c_buttons_state |= (1 << TRIGGER_RIGHT);
                } else c_trigger_r_magnitude = 0f;

            } else {
                Logger.warn("unable to retrieve game-pad state for [{}] \"{}\"",slot,name);
            }
        }

        boolean currentlyPressed(int btn) { return (c_buttons_state & (1 << btn)) > 0; }
        boolean previouslyPressed(int btn) { return (p_buttons_state & (1 << btn)) > 0; }
        boolean justPressed(int btn) { return currentlyPressed(btn) &! previouslyPressed(btn); }
        boolean justReleased(int btn) { return previouslyPressed(btn) &! currentlyPressed(btn); }
        int setButtonState(int state, int btn) { return state | (1 << btn); }
        int clrButtonState(int state, int btn) { return state & ~(1 << btn); }
        public void dispose() { MemoryUtil.memFree(buffer); }
    }
}
