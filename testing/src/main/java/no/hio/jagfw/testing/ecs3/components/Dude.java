package no.hio.jagfw.testing.ecs3.components;

import io.github.heathensoft.jagfw.physics.Body;
import no.hio.jagfw.testing.ecs3.Disposition;

/**
 * Frederik Dahl 2/27/2025
 */
public class Dude {

    private static Dude PLAYER = null;

    public Disposition disposition = Disposition.NEUTRAL;
    public Body body;
    public boolean flying;
    public boolean invisible;
    public boolean invulnerable;
    public float base_health;
    public float base_max_health;
    public float base_armour;
    public float base_experience_yield;
    public float base_armor_multiplier = 1.0f;
    public float base_damage_multiplier = 1.0f;
    public float base_health_multiplier = 1.0f;
    public int level;

    public boolean isPlayer() {
        return PLAYER == this;
    }

    public void setPlayer() {
        PLAYER = this;
    }

}
