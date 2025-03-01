package no.hio.jagfw.testing.ecs.components;

import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/28/2025
 */
public class Projectile {

    public Disposition source_disposition = Disposition.NEUTRAL;
    public ProjectileEffect effect;
    public Vector2f direction = new Vector2f();
    public Vector2f position = new Vector2f();
    public Vector2f origin = new Vector2f();
    public float range = 32;
    public float velocity = 1f;
    public float radius = 0.25f;

    public Projectile(ProjectileEffect effect) {
        this.effect = effect;
    }

    public void move(float dt) {
        position.x += direction.x * velocity * dt;
        position.y += direction.y * velocity * dt;
    }

    public boolean outOfBounds() {
        return lengthSquared() > U.square(range);
    }

    public float lengthSquared() {
        return position.distanceSquared(origin);
    }



}
