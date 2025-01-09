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

    // ALLOWS FOR 4 GAMEPADS TO BE CONNECTED AT THE SAME TIME
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
    private static final int LEFT_STICK = 16;
    private static final int RIGHT_STICK = 17;

    private static final float TRIGGER_DEAD_ZONE = 0.15f;
    private static final int LEFT_TRIGGER = 18;
    private static final int RIGHT_TRIGGER = 19;


    private final GamePad[] gamepads = new GamePad[SLOT_COUNT];
    private int active_slot = SLOT_0;
    private int num_connected = 0;


    Controller() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (glfwJoystickPresent(slot)) {
                onJoystickConnect(slot);
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
            return gamepads[active_slot].currentlyPressed(LEFT_STICK);
        } return false;
    }

    public boolean leftStickJustPushed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(LEFT_STICK);
        } return false;
    }

    public boolean leftStickJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(LEFT_STICK);
        } return false;
    }

    public boolean rightStickPushed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(RIGHT_STICK);
        } return false;
    }

    public boolean rightStickJustPushed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(RIGHT_STICK);
        } return false;
    }

    public boolean rightStickJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(RIGHT_STICK);
        } return false;
    }

    public Vector2f leftStickDirection() {
        if (isConnected()) {
            return gamepads[active_slot].c_axis_left_dir;
        } return null;
    }

    public Vector2f rightStickDirection() {
        if (isConnected()) {
            return gamepads[active_slot].c_axis_right_dir;
        } return null;
    }

    public float leftStickMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_axis_left_magnitude;
        } return 0;
    }

    public float rightStickMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_axis_right_magnitude;
        } return 0;
    }



    public boolean leftTriggerPressed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(LEFT_TRIGGER);
        } return false;
    }

    public boolean leftTriggerJustPressed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(LEFT_TRIGGER);
        } return false;
    }

    public boolean leftTriggerJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(LEFT_TRIGGER);
        } return false;
    }

    public float leftTriggerMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_axis_left_trigger;
        } return 0;
    }



    public boolean rightTriggerPressed() {
        if (isConnected()) {
            return gamepads[active_slot].currentlyPressed(RIGHT_TRIGGER);
        } return false;
    }

    public boolean rightTriggerJustPressed() {
        if (isConnected()) {
            return gamepads[active_slot].justPressed(RIGHT_TRIGGER);
        } return false;
    }

    public boolean rightTriggerJustReleased() {
        if (isConnected()) {
            return gamepads[active_slot].justReleased(RIGHT_TRIGGER);
        } return false;
    }

    public float rightTriggerMagnitude() {
        if (isConnected()) {
            return gamepads[active_slot].c_axis_right_trigger;
        } return 0;
    }



    protected void processInput(float dt) {

        if (num_connected < 0 || num_connected > SLOT_LAST) {
            // TODO: Remove
            Logger.error("illegal num controllers: [{}]. Should not occur",num_connected);
            Engine.get().exitMainLoop();
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (gamepads[slot] != null) {
                gamepads[slot].updateState(dt);
            }
        }
    }

    protected void onJoystickConnect(int slot) {
        if (isValidSlot(slot)) {
            if (glfwJoystickIsGamepad(slot)) {
                if (gamepads[slot] != null) {
                    gamepads[slot].dispose();
                    num_connected--;
                } String name = glfwGetGamepadName(slot);
                Logger.debug("game-pad connected: [{}] \"{}\"",slot,name);
                gamepads[slot] = new GamePad(slot,name);
                num_connected++;

            }
        }
    }

    protected void onJoystickDisconnect(int slot) {
        if (isValidSlot(slot)) {
            if (gamepads[slot] != null) {
                String name = gamepads[slot].name;
                Logger.debug("game-pad disconnected: [{}] \"{}\"",slot,name);
                gamepads[slot].dispose();
                gamepads[slot] = null;
                num_connected--;
            }
        }
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

    private static final class GamePad implements Disposable {

        int slot;

        int c_buttons;
        int p_buttons;

        String name;
        ByteBuffer buffer;
        GLFWGamepadState state;

        float c_axis_left_trigger;
        float c_axis_right_trigger;
        float p_axis_left_trigger;
        float p_axis_right_trigger;

        float c_axis_left_magnitude;
        float c_axis_right_magnitude;
        float p_axis_left_magnitude;
        float p_axis_right_magnitude;

        final Vector2f c_axis_left_dir = new Vector2f();
        final Vector2f c_axis_right_dir = new Vector2f();
        final Vector2f p_axis_left_dir = new Vector2f();
        final Vector2f p_axis_right_dir = new Vector2f();

        GamePad(int slot, String name) {
            this.slot = slot;
            this.name = name;
            this.buffer = MemoryUtil.memAlloc(GLFWGamepadState.SIZEOF);
            this.state = new GLFWGamepadState(buffer);
        }

        void updateState(float dt) {

            p_buttons = c_buttons;
            p_axis_left_dir.set(c_axis_left_dir);
            p_axis_right_dir.set(c_axis_right_dir);
            p_axis_left_trigger = c_axis_left_trigger;
            p_axis_right_trigger = c_axis_right_trigger;
            p_axis_left_magnitude = c_axis_left_magnitude;
            p_axis_right_magnitude = c_axis_right_magnitude;

            c_buttons = 0;
            c_axis_left_dir.zero();
            c_axis_right_dir.zero();
            c_axis_left_magnitude = 0f;
            c_axis_right_magnitude = 0f;
            c_axis_left_trigger = 0f;
            c_axis_right_trigger = 0f;

            if (glfwGetGamepadState(slot,state)) {

                for (int btn = 0; btn <= BUTTON_LAST; btn++) {
                    if (state.buttons(btn) == GLFW_PRESS) {
                        c_buttons |= (1 << btn);
                    }
                }

                c_axis_left_dir.x = state.axes(AXIS_LEFT_X);
                c_axis_left_dir.y = state.axes(AXIS_LEFT_Y);
                c_axis_left_magnitude = c_axis_left_dir.length();
                if (c_axis_left_magnitude > STICK_DEAD_ZONE) {
                    c_axis_left_dir.x /= c_axis_left_magnitude;
                    c_axis_left_dir.y /= c_axis_left_magnitude;
                    c_axis_left_magnitude = Math.min(c_axis_left_magnitude,1.0f);
                    c_axis_left_magnitude -= STICK_DEAD_ZONE;
                    c_axis_left_magnitude /= (1.0f - STICK_DEAD_ZONE);
                    c_buttons |= (1 << LEFT_STICK);
                } else c_axis_left_magnitude = 0f;

                c_axis_right_dir.x = state.axes(AXIS_RIGHT_X);
                c_axis_right_dir.y = state.axes(AXIS_RIGHT_Y);
                c_axis_right_magnitude = c_axis_right_dir.length();
                if (c_axis_right_magnitude > STICK_DEAD_ZONE) {
                    c_axis_right_dir.x /= c_axis_right_magnitude;
                    c_axis_right_dir.y /= c_axis_right_magnitude;
                    c_axis_right_magnitude = Math.min(c_axis_right_magnitude,1.0f);
                    c_axis_right_magnitude -= STICK_DEAD_ZONE;
                    c_axis_right_magnitude /= (1.0f - STICK_DEAD_ZONE);
                    c_buttons |= (1 << RIGHT_STICK);
                } else c_axis_right_magnitude = 0f;

                c_axis_left_trigger = state.axes(AXIS_LEFT_TRIGGER);
                c_axis_left_trigger = U.clamp(c_axis_left_trigger,-1f,1f);
                c_axis_left_trigger = (c_axis_left_trigger + 1f) / 2f;
                if (c_axis_left_trigger > TRIGGER_DEAD_ZONE) {
                    c_axis_left_trigger -= TRIGGER_DEAD_ZONE;
                    c_axis_left_trigger /= (1.0f - TRIGGER_DEAD_ZONE);
                    c_buttons |= (1 << LEFT_TRIGGER);
                } else c_axis_left_trigger = 0f;

                c_axis_right_trigger = state.axes(AXIS_RIGHT_TRIGGER);
                c_axis_right_trigger = U.clamp(c_axis_right_trigger,-1f,1f);
                c_axis_right_trigger = (c_axis_right_trigger + 1f) / 2f;
                if (c_axis_right_trigger > TRIGGER_DEAD_ZONE) {
                    c_axis_right_trigger -= TRIGGER_DEAD_ZONE;
                    c_axis_right_trigger /= (1.0f - TRIGGER_DEAD_ZONE);
                    c_buttons |= (1 << RIGHT_TRIGGER);
                } else c_axis_right_trigger = 0f;

            } else {
                Logger.warn("unable to retrieve game-pad state for [{}] \"{}\"",slot,name);
            }
        }

        boolean currentlyPressed(int btn) { return (c_buttons & (1 << btn)) > 0; }
        boolean previouslyPressed(int btn) { return (p_buttons & (1 << btn)) > 0; }
        boolean justPressed(int btn) { return currentlyPressed(btn) &! previouslyPressed(btn); }
        boolean justReleased(int btn) { return previouslyPressed(btn) &! currentlyPressed(btn); }
        int setButtonState(int state, int btn) { return state | (1 << btn); }
        int clrButtonState(int state, int btn) { return state & ~(1 << btn); }
        public void dispose() { MemoryUtil.memFree(buffer); }
    }
}
