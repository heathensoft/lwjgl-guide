package no.hio.jagfw.testing.ecs.components;

import io.github.heathensoft.jagfw.physics.Body;

/**
 * Frederik Dahl 2/27/2025
 */
public class Dude extends Body {

    public static Dude PLAYER = null;
    public Disposition disposition;
    public PhysicsResolution resolution;
    public boolean dying;
    public boolean flying;
    public boolean invisible;
    public boolean invulnerable;
    public float base_health;
    public float base_max_health;
    public float base_armour;
    public float base_experience_yield;
    public float base_movement_force = 1000f;
    public float base_dodge_impulse = 2000f;


    public Dude(float x, float y, float radius, float mass, Disposition disposition) {
        super(x, y, radius, mass);
        this.disposition = disposition;
        this.resolution = PhysicsResolution.HIGH;
    }

    public boolean isPlayer() {
        return PLAYER == this;
    }

    public void setPlayer() {
        PLAYER = this;
    }

}
