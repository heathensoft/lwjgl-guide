package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;

/**
 * A Surface is a line segment with restitution and friction
 * In terms of physics application, the surface is considered as
 * a static body. It's harder to go through surfaces than other bodies.
 * (It should be impossible to pass through)
 * Frederik Dahl 1/23/2025
 */
public class Surface {
    public final LineSegment segment = new LineSegment();
    public float restitution = 0.5f;
    public float friction = 0.2f;
    public Surface() { /* */ }
    public Surface(float x0, float y0, float x1, float y1) {
        segment.set(x0, y0, x1, y1);
    }
}
