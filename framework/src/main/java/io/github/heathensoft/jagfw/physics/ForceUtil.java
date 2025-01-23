package io.github.heathensoft.jagfw.physics;

import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.popSetVec2;
import static io.github.heathensoft.jagfw.utils.U.pushVec2;

/**
 * Utility class to calculate various common forces.
 * There are a lot more you could add in later.
 * Apart from the methods of this class
 * you can also simply just call body.addForce(force)
 * or body.addTorque(torque) to apply custom forces
 *
 * @author Frederik Dahl
 * 14/01/2025
 */
public class ForceUtil {

    /**
     * Apply drag force to body
     * (larger k, more drag)
     * (higher velocity, more drag)
     * @param body body
     * @param k drag coefficient
     */
    public static void applyDrag(RigidBody body, float k) {
        /*
            p = fluid/gas density
            Kd = Drag coefficient
            A = cross-sectional area (Airplane wing)
            Fd = [(1/2) * p * Kd * A] * |v|^2 * -v =>
            Fd = [k] * |v|^2 * -v
         */
        if (body.isStatic()) return;
        float mag_squared = body.velocity.lengthSquared();
        if (mag_squared > 0) {
            // Calculate the drag direction (inverse of velocity unit vector)
            Vector2f drag_force = popSetVec2(body.velocity);
            drag_force.normalize().mul(-1.0f);
            // Calculate the drag magnitude, k * |v|^2
            float dragMagnitude = k * mag_squared;
            // Generate the final drag force with direction and magnitude
            drag_force.mul(dragMagnitude);
            body.addForce(drag_force);
            pushVec2();
        }
    }

    /**
     * Apply friction to body
     * @param body body
     * @param k friction coefficient
     */
    public static void applyFriction(RigidBody body, float k) {
        if (body.isStatic()) return;
        // Calculate the drag direction (inverse of velocity unit vector)
        if (body.velocity.x == 0 && body.velocity.y == 0) return;

        Vector2f v = popSetVec2(body.velocity).normalize().mul(-1.0f);
        if (v.lengthSquared() < body.acceleration.lengthSquared()) {
            body.addForce(v.mul(k));

        }
        pushVec2();


    }

    /**
     * Apply Gravitational force between two bodies
     * The universal gravitational constant is 6.673e-11f.
     * For small bodies, that force would be practically non-existent.
     * @param a body a
     * @param b body b
     * @param G A customized gravitational constant
     * @param min_dist clamp the distance to a min dist
     * @param max_dist clamp the distance to a max dist
     */
    public static void applyGravitation(RigidBody a, RigidBody b, float G, float min_dist, float max_dist) {
        if (a.isStatic() && b.isStatic()) return;
        if (max_dist < min_dist) {
            float tmp = max_dist;
            max_dist = min_dist;
            min_dist = tmp;
        } Vector2f v = popSetVec2(b.position).sub(a.position);
        // Clamp the values of the distance (to allow for some interesting visual effects)
        float dist_squared = v.lengthSquared();
        dist_squared = Math.min(max_dist * max_dist, dist_squared);
        dist_squared = Math.max(min_dist * min_dist, dist_squared);
        // Calculate the strength of the attraction force
        float attraction_mag = G * (a.mass * b.mass) / dist_squared;
        // Calculate the direction of the attraction force
        // and the final resulting attraction force vector
        v.normalize().mul(attraction_mag);
        a.addForce( v.x, v.y);
        b.addForce(-v.x,-v.y);
        pushVec2();
    }

    /**
     * Apply Spring force between body and static anchor
     * @param body body
     * @param anchor anchor
     * @param rest_len length of spring at rest
     * @param k "spring stiffness"
     */
    public static void applySpringForce(RigidBody body, Vector2f anchor, float rest_len, float k) {
        // Calculate the distance between the anchor and the object
        Vector2f vec = popSetVec2(body.position).sub(anchor);
        // Find the spring displacement considering the rest length
        float displacement = vec.length() - rest_len;
        // Calculate the magnitude of the spring force
        float spring_magnitude = -k * displacement;
        // Calculate the direction of the spring force
        // and the final resulting spring force vector
        vec.normalize().mul(spring_magnitude);
        body.addForce(vec);
        pushVec2();
    }

    /**
     * Apply Spring force between two bodies
     * @param a body a
     * @param b body b
     * @param rest_len length of spring at rest
     * @param k "spring stiffness"
     */
    public static void applySpringForce(RigidBody a, RigidBody b, float rest_len, float k) {
        // Calculate the distance between the anchor and the object
        Vector2f v = popSetVec2(a.position).sub(b.position);
        // Find the spring displacement considering the rest length
        float displacement = v.length() - rest_len;
        // Calculate the magnitude of the spring force
        float spring_magnitude = -k * displacement;
        // Calculate the direction of the spring force
        // and the final resulting spring force vector
        v.normalize().mul(spring_magnitude);
        a.addForce( v.x, v.y);
        b.addForce(-v.x,-v.y);
        pushVec2();
    }
}
