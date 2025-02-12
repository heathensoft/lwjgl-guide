package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
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
        float a = ray.x1 - point.x;
        float b = ray.y1 - point.y;
        float dot = a * normal.x + b * normal.y;
        a = a - 2 * dot * normal.x + point.x;
        b = b - 2 * dot * normal.y + point.y;
        return new LineSegment(point.x, point.y,a,b);
    }

}
