package io.github.heathensoft.jagfw.physics;

import org.joml.Vector2f;

/**
 * A Surface between 2 points.
 * Bodies only collide on the side of the surface normal.
 * Let's say p0 is left and p1 is to the right. The surface
 * normal would be pointing up.
 * Frederik Dahl 1/23/2025
 */
public class SurfaceSegment {

    public final Vector2f p0 = new Vector2f(0,0);
    public final Vector2f p1 = new Vector2f(1,0);
    public float friction = 0.5f;
    public float restitution = 0.5f;

}
