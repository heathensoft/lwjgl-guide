package io.github.heathensoft.jagfw.physics;

import org.joml.Vector2f;

/**
 * Frederik Dahl 2/12/2025
 */
public class GeomContact {

    public Body body;
    public Geometry geometry;
    public final Vector2f normal = new Vector2f();
    public float depth;

    public void resolveCollision(float geometry_restitution) {
        if (!normal.isFinite()) normal.zero();
        // resolve penetration
        body.position.x -= normal.x * depth;
        body.position.y -= normal.y * depth;
        float e = (body.restitution + geometry_restitution) * 0.5f;
        float dot_normal = body.velocity.dot(normal);
        float impulse_magnitude = -(1 + e) * dot_normal / body.mass_inverse;
        body.applyImpulse(body.velocity.x * impulse_magnitude,body.velocity.y * impulse_magnitude);
    }
}
