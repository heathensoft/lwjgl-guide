package no.hio.jagfw.testing.ecs.systems;

import io.github.heathensoft.jagfw.core.Controller;
import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Keyboard;
import io.github.heathensoft.jagfw.core.Mouse;
import io.github.heathensoft.jagfw.core.gfx.Bitmap;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.Global;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import static io.github.heathensoft.jagfw.core.Controller.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

/**
 * Frederik Dahl 2/27/2025
 */
public class PlayerInput extends ECSystem {

    public final Vector2f mouse_position = new Vector2f();
    public final Vector2f move_direction = new Vector2f(0,-1);
    public final Vector2f aim_direction = new Vector2f(0,-1);
    public float aim_magnitude = 0.0f;
    public float move_magnitude = 0.0f;
    public float zoom_amount = 0.0f;    // R2 / L2 / + / -

    public boolean action;              // Cross / Space
    public boolean cancel;              // Circle / Escape
    public boolean interact;            // Triangle / R

    public boolean fire_default;        // R1 / Mouse Left
    public boolean fire_special;        // L1 / Mouse Right
    public boolean reload;              // Right Stick / Mouse Right
    public boolean next_weapon;         // UP / Mouse Scroll Up
    public boolean prev_weapon;         // DOWN / Mouse Scroll Down

    public boolean toggle_menu;         // Start / Escape
    public boolean toggle_editor;       // Select / B

    public boolean use_item;            // Square / E
    public boolean next_item;           // RIGHT / Q
    public boolean prev_item;           // LEFT / N/A
    public boolean use_item_1;          // 1
    public boolean use_item_2;          // 2
    public boolean use_item_3;          // 3
    public boolean use_item_4;          // 4

    public boolean menu_select;         // Square / Enter
    public boolean menu_down;           // DOWN / S
    public boolean menu_up;             // DOWN / W
    public boolean menu_left;           // DOWN / A
    public boolean menu_right;          // DOWN / D

    protected void processSystem(ECS ecs, float dt) {
        Keyboard keys = Engine.get().window().keys();
        Mouse mouse = Engine.get().window().mouse();
        Controller controller = Engine.get().window().controller();
        if (keys.justPressed(GLFW.GLFW_KEY_F1)) {
            ecs.signalToExit();
            return;
        }
        if (keys.justPressed(GLFW_KEY_F2)) {
            Bitmap bitmap = Framebuffer.screenshot();
            bitmap.compressToDisk("screenshot.png",true);
            bitmap.dispose();
        }

        WorldCamera world_camera = ecs.getSystem(WorldCamera.class);
        Global global = ecs.getSharedContext(Global.class);
        PlayerInput input = this;

        Vector2f mouse_position = input.mouse_position;
        mouse_position.set(mouse.position());
        world_camera.camera.unProjectPosition(mouse_position);

        controller.setActiveSlot(0);
        if (controller.isConnected()) {
            global.controller_connected = true;
            input.action = controller.buttonJustPressed(BUTTON_CROSS);
            input.cancel = controller.buttonJustPressed(BUTTON_CIRCLE);
            input.interact = controller.buttonJustPressed(BUTTON_TRIANGLE);

            input.fire_default = controller.buttonPressed(BUTTON_RIGHT_BUMPER);
            input.fire_special = controller.buttonPressed(BUTTON_LEFT_BUMPER);
            input.reload = controller.buttonJustPressed(BUTTON_RIGHT_THUMB);
            input.next_weapon = controller.buttonJustPressed(BUTTON_DPAD_UP);
            input.prev_weapon = controller.buttonJustPressed(BUTTON_DPAD_DOWN);

            input.toggle_menu = controller.buttonJustPressed(BUTTON_START);
            input.toggle_editor = controller.buttonJustPressed(BUTTON_GUIDE);
            input.use_item = controller.buttonJustPressed(BUTTON_SQUARE);
            input.next_item = controller.buttonJustPressed(BUTTON_DPAD_RIGHT);
            input.prev_item = controller.buttonJustPressed(BUTTON_DPAD_LEFT);

            if (controller.leftStickPushed()) {
                input.move_magnitude = controller.leftStickMagnitude();
                input.move_direction.set(controller.leftStickDirection());
            } else input.move_magnitude = 0f;
            if (controller.rightStickPushed()) {
                input.aim_magnitude = controller.rightStickMagnitude();
                input.aim_direction.set(controller.rightStickDirection());
            } else input.aim_magnitude = 0f;

            input.zoom_amount = 0f;
            if (controller.buttonJustPressed(BUTTON_DPAD_DOWN)) {
                input.zoom_amount += 1.0f;
            } if (controller.buttonJustPressed(BUTTON_DPAD_UP)) {
                input.zoom_amount -= 1.0f;
            }

        } else {
            global.controller_connected = false;
            input.action = keys.justPressed(GLFW_KEY_SPACE);
            input.cancel = keys.justPressed(GLFW_KEY_ESCAPE);
            input.interact = keys.justPressed(GLFW_KEY_R);

            input.fire_default = mouse.buttonPressed(Mouse.LEFT);
            input.fire_special = mouse.buttonPressed(Mouse.RIGHT);

            input.next_weapon = false;
            input.prev_weapon = false;
            if (mouse.scrolled()) {
                float scroll_amount = mouse.scrollValue();
                if (scroll_amount < 0) {
                    input.prev_weapon = true;
                } else if (scroll_amount > 0) {
                    input.next_weapon = true;
                }
            }
            input.toggle_menu = keys.justPressed(GLFW_KEY_ESCAPE);
            input.toggle_editor = keys.justPressed(GLFW_KEY_B);
            input.use_item = keys.justPressed(GLFW_KEY_E);
            input.next_item = keys.justPressed(GLFW_KEY_Q);
            input.use_item_1 = keys.justPressed(GLFW_KEY_1);
            input.use_item_2 = keys.justPressed(GLFW_KEY_2);
            input.use_item_3 = keys.justPressed(GLFW_KEY_3);
            input.use_item_4 = keys.justPressed(GLFW_KEY_4);

            float movement_x = 0;
            float movement_y = 0;
            if (keys.pressed(GLFW_KEY_A)) movement_x -= 1;
            if (keys.pressed(GLFW_KEY_S)) movement_y -= 1;
            if (keys.pressed(GLFW_KEY_D)) movement_x += 1;
            if (keys.pressed(GLFW_KEY_W)) movement_y += 1;
            if (movement_x == 0 && movement_y == 0) {
                input.move_magnitude = 0f;
            } else {
                input.move_magnitude = 1f;
                input.move_direction.set(movement_x,movement_y);
                input.move_direction.normalize();
            }

            input.aim_direction.set(mouse_position);
            input.aim_direction.sub(world_camera.target_position);
            input.aim_direction.normalize();
            if (input.aim_direction.isFinite()) {
                input.aim_magnitude = 1.0f;
            } else {
                input.aim_direction.zero();
                input.aim_magnitude = 0.0f;
            }

            input.zoom_amount = 0f;
            if (keys.justPressed(GLFW_KEY_KP_ADD)) {
                input.zoom_amount += 1.0f;
            } if (keys.justPressed(GLFW_KEY_KP_SUBTRACT)) {
                input.zoom_amount -= 1.0f;
            }

        }
        if (input.toggle_menu) {
            global.menu_mode = !global.menu_mode;
        }
        if (!global.menu_mode) {
            if (input.toggle_editor) {
                global.editor_mode = !global.editor_mode;
            }
        }

    }

}
