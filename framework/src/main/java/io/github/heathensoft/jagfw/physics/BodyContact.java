package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/10/2025
 */
public class BodyContact {

    public Body A;
    public Body B;
    public final Vector2f normal = new Vector2f();
    public float depth;


    public void resolveCollision() {
        if (!normal.isFinite()) normal.zero();
        resolvePenetration();
        // Define elasticity (coefficient of restitution e)
        // we are using the average, but you could use min() or something else instead.
        float e = (A.restitution + B.restitution) * 0.5f;
        // relative velocity of the bodies
        Vector2f relative_velocity = U.popSetVec2(A.velocity).sub(B.velocity);
        float dot_normal = relative_velocity.dot(normal);
        float impulse_magnitude = -(1 + e) * dot_normal / (A.mass_inverse + B.mass_inverse);
        Vector2f impulse = relative_velocity.mul(impulse_magnitude);
        A.applyImpulse(impulse);
        B.applyImpulse(impulse.negate());
        U.pushVec2();
    }


    /**
     * Apply positional correction using the projection method
     * (Move bodies out of collision)
     */
    private void resolvePenetration() {
        // static bodies (inverse mass = 0) will not move
        float d = depth / (A.mass_inverse + B.mass_inverse);
        float da = d * A.mass_inverse;
        float db = d * B.mass_inverse;
        A.position.x -= normal.x * da;
        A.position.y -= normal.y * da;
        B.position.x += normal.x * db;
        B.position.y += normal.y * db;
    }
}
