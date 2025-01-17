package io.github.heathensoft.jagfw.physics;

import org.joml.Vector2f;
import static io.github.heathensoft.jagfw.utils.U.*;

/**
 * Frederik Dahl 1/16/2025
 */
public class Contact {

    public RigidBody bodyA, bodyB;
    public final Vector2f start = new Vector2f();
    public final Vector2f end = new Vector2f();
    public final Vector2f normal = new Vector2f();
    public float depth;


    public void resolveCollision() {


        Vector2f ra = popSetVec2(end).sub(bodyA.position);
        Vector2f rb = popSetVec2(start).sub(bodyB.position);
        resolvePenetration();
        // Define elasticity (coefficient of restitution e) and friction
        float e = (bodyA.restitution + bodyB.restitution) * 0.5f;
        float f = (bodyA.friction + bodyB.friction) * 0.5f;
        // linear + angular velocity of a -> a.v + w x ra
        // linear + angular velocity of b -> b.v + w x rb
        Vector2f va = popSetVec2(-bodyA.angular_vel * ra.y, bodyA.angular_vel * ra.x).add(bodyA.velocity);
        Vector2f vb = popSetVec2(-bodyB.angular_vel * rb.y, bodyB.angular_vel * rb.x).add(bodyB.velocity);
        // v_rel is the linear + angular velocity of body a
        // minus the linear + angular velocity of body b
        Vector2f v_rel = popSetVec2(va).sub(vb);
        Vector2f jn, jt;

        // collision impulse along the normal
        {
            // the relative velocity along the collision normal
            float v_rel_dot_normal = v_rel.dot(normal);
            // calculating impulse magnitude
            float ra_cross_nor = cross(ra,normal);
            float rb_cross_nor = cross(rb,normal);
            float numerator = -(1 + e) * v_rel_dot_normal;
            float denominator = (bodyA.mass_inv + bodyB.mass_inv);
            denominator += (ra_cross_nor * ra_cross_nor) * bodyA.I_inv;
            denominator += (rb_cross_nor * rb_cross_nor) * bodyB.I_inv;
            float impulse_magnitude = numerator / denominator;
            jn = popSetVec2(normal).mul(impulse_magnitude);
        }
        // collision impulse along the tangent (including friction)
        {
            Vector2f tangent = popSetVec2(normal).perpendicular();
            // the relative velocity along the collision normal
            float v_rel_dot_tangent = v_rel.dot(tangent);
            // calculating impulse magnitude
            float ra_cross_tan = cross(ra,tangent);
            float rb_cross_tan = cross(rb,tangent);
            float numerator = f * -(1 + e) * v_rel_dot_tangent;
            float denominator = (bodyA.mass_inv + bodyB.mass_inv);
            denominator += (ra_cross_tan * ra_cross_tan) * bodyA.I_inv;
            denominator += (rb_cross_tan * rb_cross_tan) * bodyB.I_inv;
            float impulse_magnitude = numerator / denominator;
            jt = popSetVec2(tangent).mul(impulse_magnitude);
        }

        Vector2f j = jn.add(jt);
        bodyA.applyImpulse(j,ra);
        bodyB.applyImpulse(j.negate(),rb);
        pushVec2(8);
    }


    /**
     * Apply positional correction using the projection method
     */
    private void resolvePenetration() {
        // static bodies (inverse mass = 0) will not move
        float d = depth / (bodyA.mass_inv + bodyB.mass_inv);
        float da = d * bodyA.mass_inv;
        float db = d * bodyB.mass_inv;
        bodyA.position.x -= normal.x * da;
        bodyA.position.y -= normal.y * da;
        bodyB.position.x += normal.x * db;
        bodyB.position.y += normal.y * db;
    }

}
