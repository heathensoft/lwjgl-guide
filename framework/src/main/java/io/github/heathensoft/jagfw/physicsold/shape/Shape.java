package io.github.heathensoft.jagfw.physicsold.shape;

import org.joml.Vector2f;

/**
 * Shapes are used for collision detection.
 * Hit-boxes and Physics bodies
 * Frederik Dahl 1/23/2025
 */
public abstract class Shape {

    protected float radius;

    /**
     * Calculates the moment of inertia of shape based on Body mass
     * @param mass the mass of the Body
     * @return moment of inertia
     */
    public abstract float calculateMomentOfInertia(float mass);

    /**
     * For Circle shapes, this is the circle radius.
     * For polygons, this is the max distance from polygon center
     * to the furthest vertex. The radius contains the Shape.
     * @return the shape radius (bounding radius)
     */
    public float radius() { return radius; }

    /**
     * Rotates (around center) and translates all vertices
     * (Won't affect Circle Shape)
     * @param position world position
     * @param rotation world rotation in radians
     */
    public void updateVertices(Vector2f position, float rotation) { }

    /**
     * Move Shape by translation
     * (Won't affect Circle Shape)
     * @param translation translation
     */
    public void translate(Vector2f translation) { }
}
