package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import org.joml.Math;
import org.joml.Vector2f;

/**
 * Contact between a Ray and the environment
 * Frederik Dahl 1/31/2025
 */
public class RayContact {

    public LineSegment ray;
    public final Vector2f point = new Vector2f();
    public final Vector2f normal = new Vector2f();

    /**
     * Reflect the Ray of the contact surface.
     * The new Ray length is the remainder
     * of the original length before the contact.
     * (It's shorter than the original)
     * Could be useful for bouncing bullets, lightning bolts etc.
     * @return new Ray
     */
    public LineSegment reflect() {
        return reflect(new LineSegment());
    }

    /**
     * Reflect the Ray of the contact surface.
     * The new Ray length is the remainder
     * of the original length before the contact.
     * (It's shorter than the original)
     * Could be useful for bouncing bullets, lightning bolts etc.
     * @param dst resulting reflection
     * @return dst
     */
    public LineSegment reflect(LineSegment dst) {
        float a = ray.x1 - point.x;
        float b = ray.y1 - point.y;
        float dot = a * normal.x + b * normal.y;
        a = a - 2 * dot * normal.x + point.x;
        b = b - 2 * dot * normal.y + point.y;
        dst.set(point.x, point.y,a,b);
        return dst;
    }

    /**
     * Get ray from origin to contact.
     * ("cutting of" anything after contact)
     * @return new Ray
     */
    public LineSegment originToContact() {
        return originToContact(new LineSegment());
    }

    /**
     * Get ray from origin to contact.
     * ("cutting of" anything after contact)
     * @param dst resulting ray
     * @return dst
     */
    public LineSegment originToContact(LineSegment dst) {
        dst.x0 = ray.x0;
        dst.y0 = ray.y0;
        dst.x1 = point.x;
        dst.y1 = point.y;
        return dst;
    }

    /**
     * @return (Length)^2 from ray origin to contact point
     */
    public float lengthSquared() {
        float dx = point.x - ray.x0;
        float dy = point.y - ray.y0;
        return (dx * dx + dy * dy);
    }

    /**
     * @return Length from ray origin to contact point
     */
    public float length() {
        return Math.sqrt(lengthSquared());
    }

}
