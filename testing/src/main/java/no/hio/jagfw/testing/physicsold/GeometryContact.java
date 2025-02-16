package no.hio.jagfw.testing.physicsold;

import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.core.utils.U.cross;

/**
 * Frederik Dahl 1/26/2025
 */
public class GeometryContact {

    // + index of edge
    public PhysicsGeometry geometry;
    public PhysicsBody body;
    public final Vector2f point = new Vector2f();   // point of collision on surface
    public final Vector2f normal = new Vector2f();  // surface / collision normal
    public float depth;                             // depth of penetration


    public void resolveCollision() {
        body.colliding = true;
        Vector2f j, jn, jt;
        Vector2f tmp0 = U.popVec2();
        Vector2f tmp1 = U.popVec2();
        Vector2f tmp2 = U.popVec2();
        Vector2f tmp3 = U.popVec2();
        Vector2f r = tmp0.set(point).sub(body.position);
        if (!normal.isFinite()) normal.zero();
        resolvePenetration(); // modifies position directly
        // Define elasticity (coefficient of restitution e) and friction
        float f = (body.friction + geometry.friction()) * 0.5f;
        float e = (body.restitution + geometry.restitution()) * 0.5f;
        // linear + angular velocity of body -> body.v + w x r
        Vector2f v = tmp1.set(-body.angular_velocity * r.y, body.angular_velocity * r.x).add(body.velocity);
        { // impulse along the collision normal
            // the relative velocity along the collision normal
            float dot_normal = v.dot(normal);
            // calculating impulse magnitude
            float r_cross_nor = cross(r,normal);
            float numerator = -(1 + e) * dot_normal;
            float denominator = body.mass_inverse;
            denominator += (r_cross_nor * r_cross_nor) * body.moi_inverse;
            float impulse_magnitude = numerator / denominator;
            jn = tmp2.set(normal).mul(impulse_magnitude);
        }
        { // collision impulse along the tangent (including friction)
            Vector2f tangent = tmp3.set(normal).perpendicular();
            // the relative velocity along the collision normal
            float dot_tangent = v.dot(tangent);
            // calculating impulse magnitude
            float r_cross_tan = cross(r,tangent);
            float numerator = f * -(1 + e) * dot_tangent;
            float denominator = body.mass_inverse;
            denominator += (r_cross_tan * r_cross_tan) * body.moi_inverse;
            float impulse_magnitude = numerator / denominator;
            jt = tangent.mul(impulse_magnitude);
        }
        j = jn.add(jt);
        body.applyImpulse(j,r);
        U.pushVec2(4);
    }

    /**
     * Apply positional correction using the projection method
     * (Move bodies out of collision)
     */
    public void resolvePenetration() {
        body.position.x -= normal.x * depth;
        body.position.y -= normal.y * depth;
    }
}
