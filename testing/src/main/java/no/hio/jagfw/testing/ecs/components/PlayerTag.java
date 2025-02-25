package no.hio.jagfw.testing.ecs.components;


import org.joml.Vector2f;

/**
 * Frederik Dahl 2/23/2025
 */
public class PlayerTag {

    public float input_movement_force = 4000f;
    public float input_dodge_impulse = 6000f;

    public float aim_range_limit = 8f;
    public Vector2f aim_vector = new Vector2f();

}
