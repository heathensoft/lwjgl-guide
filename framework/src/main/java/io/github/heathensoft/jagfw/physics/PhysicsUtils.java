package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.utils.LineSegment;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.core.utils.U.popSetVec2;
import static io.github.heathensoft.jagfw.core.utils.U.pushVec2;

/**
 * Frederik Dahl 1/31/2025
 */
public class PhysicsUtils {

    public static int CIRCLE_RESOLUTION = 32;
    public static int COLOR_RAY = 0xFF00FFFF;
    public static int COLOR_STATIC = 0xFFFFF000;
    public static int COLOR_DYNAMIC = 0xFF00FF00;
    public static int COLOR_HURT_BOX = 0xFFFF00FF;


    public static void drawBody(Body body, LineBatch batch) {
        int color = body.isStatic() ? COLOR_STATIC : COLOR_DYNAMIC;
        batch.drawCircle(body.position,body.radius,CIRCLE_RESOLUTION,color);
    }

    public static void drawGeometry(Geometry geometry, LineBatch batch) {
        int num_segments = geometry.numSegments();
        for (int i = 0; i < num_segments; i++) {
            Vector2f v0 = geometry.vertices[i];
            Vector2f v1 = geometry.vertices[(i + 1) % geometry.vertices.length];
            batch.drawLine(v0,v1,COLOR_STATIC);
        }
    }

    public static void drawRay(LineSegment ray, LineBatch batch) {
        batch.drawLine(ray,COLOR_RAY);
    }

    public static void applyDownwardsGravity(Body body, float gravity) {
        body.addForce(0,-gravity * body.mass());
    }

    /**
     * Apply drag force to body
     * (larger k, more drag)
     * (higher velocity, more drag)
     * @param body body
     * @param k drag coefficient
     */
    public static void applyDrag(Body body, float k) {
        /*  p = fluid/gas density
            Kd = Drag coefficient
            A = cross-sectional area (Airplane wing)
            Fd = [(1/2) * p * Kd * A] * |v|^2 * -v =>
            Fd = [k] * |v|^2 * -v */
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
    public static void applyFriction(Body body, float k) {
        // Calculate the drag direction (inverse of velocity unit vector)
        if (body.velocity.x == 0 && body.velocity.y == 0) return;
        Vector2f velocity_normalized = popSetVec2(body.velocity);
        velocity_normalized.normalize().mul(-1.0f);
        if (velocity_normalized.lengthSquared() < body.acceleration.lengthSquared()) {
            body.addForce(velocity_normalized.mul(k));
        } pushVec2();
    }

    /**
     * Apply Spring force between body and static anchor
     * @param body body
     * @param anchor anchor
     * @param rest_len length of spring at rest
     * @param k "spring stiffness"
     */
    public static void applySpringForce(Body body, Vector2f anchor, float rest_len, float k) {
        // Calculate the distance between the anchor and the object
        Vector2f anchor_to_body = popSetVec2(body.position).sub(anchor);
        // Find the spring displacement considering the rest length
        float displacement = anchor_to_body.length() - rest_len;
        // Calculate the magnitude of the spring force
        float spring_magnitude = -k * displacement;
        // Calculate the direction of the spring force
        // and the final resulting spring force vector
        anchor_to_body.normalize().mul(spring_magnitude);
        body.addForce(anchor_to_body);
        pushVec2();
    }

    /**
     * Apply Spring force between two bodies
     * @param A body a
     * @param B body b
     * @param rest_len length of spring at rest
     * @param k "spring stiffness"
     */
    public static void applySpringForce(Body A, Body B, float rest_len, float k) {
        // Calculate the distance between the anchor and the object
        Vector2f ba = popSetVec2(A.position).sub(B.position);
        // Find the spring displacement considering the rest length
        float displacement = ba.length() - rest_len;
        // Calculate the magnitude of the spring force
        float spring_magnitude = -k * displacement;
        // Calculate the direction of the spring force
        // and the final resulting spring force vector
        ba.normalize().mul(spring_magnitude);
        A.addForce( ba.x, ba.y);
        B.addForce(-ba.x,-ba.y);
        pushVec2();
    }

}
