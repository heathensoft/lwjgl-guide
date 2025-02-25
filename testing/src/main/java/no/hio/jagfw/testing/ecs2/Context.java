package no.hio.jagfw.testing.ecs2;

import org.joml.Vector2f;

/**
 * Frederik Dahl 2/25/2025
 */
public class Context {


    public final Flags flags = new Flags();
    public final Constants constants = new Constants();
    public final PlayerInput player_input = new PlayerInput();


    public static final class PlayerInput {

        // IN GAME
        // lerp aim towards move direction if aim not active
        public final Vector2f move_direction = new Vector2f(0,-1);
        public final Vector2f aim_direction = new Vector2f(0,-1);
        public float aim_strength = 0.0f;
        public float move_strength = 0.0f;
        public float zoom_amount = 0.0f;    // R2 / L2 / + / -
        public boolean fire;                // R1 / Mouse Left
        public boolean reload;              // Right Stick / Mouse Right
        public boolean fire_mode_swap;      // Left Stick / F
        public boolean use_item;            // Square / E
        public boolean action;              // Cross / Space
        public boolean cancel;              // Circle / Escape
        public boolean interact;            // Triangle / R
        public boolean mode_toggle;         // L1 / B
        public boolean menu_toggle;         // Start / Escape
        public boolean next_item;           // RIGHT / Q
        public boolean prev_item;           // LEFT
        public boolean next_weapon;         // UP / Mouse Scroll Up
        public boolean prev_weapon;         // DOWN / Mouse Scroll Down
        public boolean item_1;              // 1
        public boolean item_2;              // 2
        public boolean item_3;              // 3
        public boolean item_4;              // 4
        public boolean menu_down;           // DOWN / KEYS DOWN
        public boolean menu_up;             // DOWN / KEYS DOWN
        public boolean menu_left;           // DOWN / KEYS DOWN
        public boolean menu_right;          // DOWN / KEYS DOWN
    }

    public static final class Constants {

    }

    public static final class Flags {
        public boolean controller_connected;
        public boolean menu_mode;
        public boolean build_mode;

    }
}
