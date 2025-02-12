package io.github.heathensoft.jagfw.physicsold.ny;

import io.github.heathensoft.jagfw.utils.LineSegment;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/31/2025
 */
public class RayContact {

    public LineSegment ray;
    public final Vector2f point = new Vector2f();
    public final Vector2f normal = new Vector2f();
    public LineSegment reflect() {
        float a = ray.x1 - point.x;
        float b = ray.y1 - point.y;
        float dot = a * normal.x + b * normal.y;
        a = a - 2 * dot * normal.x + point.x;
        b = b - 2 * dot * normal.y + point.y;
        return new LineSegment(point.x, point.y,a,b);
    }

}
