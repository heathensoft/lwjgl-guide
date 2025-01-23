package io.github.heathensoft.jagfw.physics.ny;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.cross;

/**
 * Container for Body / Body - collision information
 * After a collision has occurred, the collision needs to be resolved.
 * Collisions are resolved by first moving the body out of collision.
 * Then based on various factors like collision depth, normal, frictions etc.
 * Each body is applied an impulse. An instant change in velocity.
 * Frederik Dahl 1/23/2025
 */
public class BodyContact {

    public PhysicsBody A;
    public PhysicsBody B;
    public final Vector2f start = new Vector2f();
    public final Vector2f end = new Vector2f();
    public final Vector2f normal = new Vector2f();
    public float depth;

    /**
     * If a collision has occurred between two bodies in the system,
     * use this to resolve that collision.
     */
    public void resolveCollision() {
        // impulse, impulse along normal, impulse along tangent
        Vector2f j, jn, jt;
        Vector2f tmp0 = U.popVec2();
        Vector2f tmp1 = U.popVec2();
        Vector2f tmp2 = U.popVec2();
        Vector2f tmp3 = U.popVec2();
        Vector2f tmp4 = U.popVec2();
        Vector2f ra = tmp0.set(end).sub(A.position);
        Vector2f rb = tmp1.set(start).sub(B.position);
        resolvePenetration(); // modifies position directly
        // Define elasticity (coefficient of restitution e) and friction
        float f = (A.friction + B.friction) * 0.5f;
        float e = (A.restitution + B.restitution) * 0.5f;
        // linear + angular velocity of a -> a.v + w x ra
        // linear + angular velocity of b -> b.v + w x rb
        Vector2f va = tmp2.set(-A.angular_velocity * ra.y, A.angular_velocity * ra.x).add(A.velocity);
        Vector2f vb = tmp3.set(-B.angular_velocity * rb.y, B.angular_velocity * rb.x).add(B.velocity);
        // relative velocity is the linear + angular velocity of body a
        // minus the linear + angular velocity of body b
        Vector2f relative_velocity = tmp4.set(va).sub(vb);
        { // impulse along the collision normal
            // the relative velocity along the collision normal
            float dot_normal = relative_velocity.dot(normal);
            // calculating impulse magnitude
            float ra_cross_nor = cross(ra,normal);
            float rb_cross_nor = cross(rb,normal);
            float numerator = -(1 + e) * dot_normal;
            float denominator = (A.mass_inverse + B.mass_inverse);
            denominator += (ra_cross_nor * ra_cross_nor) * A.moi_inverse;
            denominator += (rb_cross_nor * rb_cross_nor) * B.moi_inverse;
            float impulse_magnitude = numerator / denominator;
            jn = tmp2.set(normal).mul(impulse_magnitude);
        }
        { // collision impulse along the tangent (including friction)
            Vector2f tangent = tmp3.set(normal).perpendicular();
            // the relative velocity along the collision normal
            float dot_tangent = relative_velocity.dot(tangent);
            // calculating impulse magnitude
            float ra_cross_tan = cross(ra,tangent);
            float rb_cross_tan = cross(rb,tangent);
            float numerator = f * -(1 + e) * dot_tangent;
            float denominator = (A.mass_inverse + B.mass_inverse);
            denominator += (ra_cross_tan * ra_cross_tan) * A.moi_inverse;
            denominator += (rb_cross_tan * rb_cross_tan) * B.moi_inverse;
            float impulse_magnitude = numerator / denominator;
            jt = tangent.mul(impulse_magnitude);
        }
        j = jn.add(jt);
        A.applyImpulse(j,ra);
        B.applyImpulse(j.negate(),rb);
        U.pushVec2(5);
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
