package io.github.heathensoft.jagfw.physics.old;


import org.joml.Math;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.*;

/**
 * @author Frederik Dahl
 * 13/01/2025
 */


public class BodyContact {
    
    public Body a, b;
    public final Vector2f start = new Vector2f();
    public final Vector2f end = new Vector2f();
    public final Vector2f normal = new Vector2f();
    public float depth;
    
    /**
     * Resolves the collision using the impulse method
     * Applying an impulse along the normal vector of
     * the collision to both bodies.
     * Does not check whether the bodies are static
     */
    public void resolveCollision() {
        resolvePenetration();
        // Define elasticity (coefficient of restitution e)
        float e = Math.min(a.restitution,b.restitution);
        // Calculate the relative velocity between the two objects
        float relative_velocity_x = a.velocity.x - b.velocity.x;
        float relative_velocity_y = a.velocity.y - b.velocity.y;
        // Calculate the relative velocity along the normal collision vector
        float dot = relative_velocity_x * normal.x + relative_velocity_y * normal.y;
        // Now we proceed to calculate the collision impulse
        float magnitude = -(1 + e) * dot / (a.mass_inv + b.mass_inv);
        float jx = magnitude * normal.x;
        float jy = magnitude * normal.y;
        // Apply the impulse vector to both
        // objects in opposite direction
        a.applyImpulse( jx, jy);
        b.applyImpulse(-jx,-jy);
    }
    
    public void resolveCollision2() {
        
        resolvePenetration();
        // Define elasticity (coefficient of restitution e) and friction
        // Todo: play around with these (max , average)
        float e = Math.min(a.restitution,b.restitution);
        float f = Math.min(a.friction,b.friction);
        // Calculate the relative velocity between the two objects
        // Todo: This might not be exactly correct (ra and rb). Adjust later.
        // Could be we need to take the middle point between
        // between start and end for both.
        // Also remember that the penetration has already been resolved
        Vector2f ra = popSetVec2(end).sub(a.position);
        Vector2f rb = popSetVec2(start).sub(b.position);
        // linear + angular velocity of a -> a.v + w x ra
        // linear + angular velocity of b -> b.v + w x rb
        Vector2f va = popSetVec2(-a.angular_vel * ra.y, a.angular_vel * ra.x).add(a.velocity);
        Vector2f vb = popSetVec2(-b.angular_vel * rb.y, b.angular_vel * rb.x).add(b.velocity);
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
            float denominator = (a.mass_inv + b.mass_inv);
            denominator += (ra_cross_nor * ra_cross_nor) * a.I_inv;
            denominator += (rb_cross_nor * rb_cross_nor) * b.I_inv;
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
            float denominator = (a.mass_inv + b.mass_inv);
            denominator += (ra_cross_tan * ra_cross_tan) * a.I_inv;
            denominator += (rb_cross_tan * rb_cross_tan) * b.I_inv;
            float impulse_magnitude = numerator / denominator;
            jt = popSetVec2(tangent).mul(impulse_magnitude);
        }
        
        Vector2f j = jn.add(jt);
        a.applyImpulse(j,ra);
        b.applyImpulse(j.negate(),rb);
        pushVec2(8);
        
    }
    
    /**
     * Apply positional correction using the projection method
     */
    private void resolvePenetration() {
        // static bodies (inverse mass = 0) will not move
        float d = depth / (a.mass_inv + b.mass_inv);
        float da = d * a.mass_inv;
        float db = d * b.mass_inv;
        a.position.x -= normal.x * da;
        a.position.y -= normal.y * da;
        b.position.x += normal.x * db;
        b.position.y += normal.y * db;
    }
    
    
}
