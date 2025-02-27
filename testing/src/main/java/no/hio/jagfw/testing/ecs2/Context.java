package no.hio.jagfw.testing.ecs2;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Resolution;
import io.github.heathensoft.jagfw.core.utils.Camera2D;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/25/2025
 */
public class Context {

    public static final int TILE_SIZE = 32;
    public static final float FRICTION = 300;
    public static final float DRAG_CONST = 20;

    public final Flags global_flags = new Flags();
    public final PlayerInput player_input = new PlayerInput();
    public final PlayerVariables player_vars = new PlayerVariables();
    public final WorldCamera world_cam = new WorldCamera();


    public static final class PlayerInput {
        // lerp aim towards move direction if aim not active and is moving

        public final Vector2f mouse_position = new Vector2f();
        public final Vector2f move_direction = new Vector2f(0,-1);
        public final Vector2f aim_direction = new Vector2f(0,-1);
        public float aim_strength = 0.0f;
        public float move_strength = 0.0f;
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

        // Menu Specific
        public boolean menu_select;         // Square / Enter
        public boolean menu_down;           // DOWN / S
        public boolean menu_up;             // DOWN / W
        public boolean menu_left;           // DOWN / A
        public boolean menu_right;          // DOWN / D

        // Build Specific
        public boolean build_next;          // RIGHT / D
        public boolean build_prev;          // LEFT / A
        public boolean build_place;         // Square / Mouse Left
        public boolean build_remove;        // Circle / Mouse Right

    }
    public static final class WorldCamera {
        public final Camera2D camera;
        public final Vector2f desired_position;
        WorldCamera() {
            Resolution resolution = Engine.get().window().gameResolution();
            camera = new Camera2D(resolution,TILE_SIZE);
            desired_position = new Vector2f();
        } public void setPosition(float x, float y) {
            camera.position.set(x,y);
            desired_position.set(x,y);
            camera.refresh();
        }
    }

    public static final class Flags {
        public boolean controller_connected;
        public boolean editor_mode;
        public boolean menu_mode;

    }

    public static final class PlayerVariables {
        public float movement_force = 4000f;
        public float dodge_impulse = 6000f;
    }
}
