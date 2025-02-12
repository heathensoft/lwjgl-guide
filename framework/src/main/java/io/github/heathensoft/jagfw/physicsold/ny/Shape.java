package io.github.heathensoft.jagfw.physicsold.ny;

import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 1/30/2025
 */
public abstract class Shape {

    /**
     * Calculates the moment of inertia of shape based on Body mass
     * @param mass the mass of the Body
     * @return moment of inertia
     */
    public abstract float calculateMomentOfInertia(float mass);

    /**
     * Rotates (around center) and translates all vertices
     * @param position world position
     * @param rotation world rotation in radians
     */
    public abstract void update(Vector2f position, float rotation);

    /**
     * The bounding radius of the shape is stored in the shape object.
     * For circles: the circle radius.
     * For boxes: the length of the longest diagonal, halved.
     * For generic polygons: the max distance from a vertex to the local center.
     * The local center is the origin of the vertices in local space
     * (The vertices constructor argument)
     * @return radius
     */
    public abstract float boundingRadius();

    /**
     * Calculates the bounding box.
     * (Not stored in the shape object)
     * @return bounding box
     */
    public abstract Rectanglef calculateBoundingBox();

    public abstract void translate(float x, float y);

    /**
     * Move Shape by translation
     * @param translation translation
     */
    public void translate(Vector2f translation) {
        translate(translation.x,translation.y);
    }

}
