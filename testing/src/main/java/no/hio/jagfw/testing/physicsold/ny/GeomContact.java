package no.hio.jagfw.testing.physicsold.ny;

import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.core.utils.U.cross;

/**
 * Frederik Dahl 1/31/2025
 */
public class GeomContact {

    public Body body;
    public Geometry geometry;
    public final Vector2f point = new Vector2f();
    public final Vector2f normal = new Vector2f();
    public float depth;

    public void resolveCollision(float friction, float restitution) {
        Vector2f jn, jt;
        if (!normal.isFinite()) normal.zero();
        Vector2f r = new Vector2f(point).sub(body.position);
        body.position.x -= normal.x * depth;
        body.position.y -= normal.y * depth;
        // Define elasticity (coefficient of restitution e) and friction
        float f = (body.friction + friction) * 0.5f;
        float e = (body.restitution + restitution) * 0.5f;
        // linear + angular velocity of body -> body.v + w x r
        Vector2f v = new Vector2f(
                -body.angular_velocity * r.y,
                body.angular_velocity * r.x).add(body.velocity);
        { // impulse along the collision normal
            // the relative velocity along the collision normal
            float dot_normal = v.dot(normal);
            // calculating impulse magnitude
            float r_cross_nor = cross(r,normal);
            float numerator = -(1 + e) * dot_normal;
            float denominator = body.mass_inverse;
            denominator += (r_cross_nor * r_cross_nor) * body.moi_inverse;
            float impulse_magnitude = numerator / denominator;
            jn = new Vector2f(normal).mul(impulse_magnitude);
        }{ // collision impulse along the tangent (including friction)
            Vector2f tangent = new Vector2f(normal).perpendicular();
            // the relative velocity along the collision normal
            float dot_tangent = v.dot(tangent);
            // calculating impulse magnitude
            float r_cross_tan = cross(r,tangent);
            float numerator = f * -(1 + e) * dot_tangent;
            float denominator = body.mass_inverse;
            denominator += (r_cross_tan * r_cross_tan) * body.moi_inverse;
            float impulse_magnitude = numerator / denominator;
            jt = tangent.mul(impulse_magnitude);
        } body.applyImpulse(jn.add(jt),r);
    }
}
