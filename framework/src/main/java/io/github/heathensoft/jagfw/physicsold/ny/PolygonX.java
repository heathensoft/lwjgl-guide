package io.github.heathensoft.jagfw.physicsold.ny;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/30/2025
 */
public class PolygonX extends Polygon {

    public final Vector2f[] vertices_local;
    public final Vector2f[] vertices_world;
    public final float radius;

    /**
     * Construct Polygon.
     * Polygon must be convex for collision detection.
     * Num vertices > 2, in counter-clockwise order.
     * @param vertices local vertices (origin centered)
     */
    public PolygonX(Vector2f[] vertices) {
        if (vertices.length <= 2) {
            throw new IllegalStateException("A Polygon needs > 2 vertices");
        } vertices_local = vertices;
        vertices_world = new Vector2f[vertices.length];
        float max_len = 0;
        for (int i = 0; i < vertices.length; i++) {
            Vector2f vertex = vertices[i];
            max_len = Math.max(max_len,vertex.length());
            vertices_world[i] = new Vector2f(vertex);
        } radius = max_len;
    }

    public Vector2f[] vertices() { return vertices_world; }
    public float boundingRadius() { return radius; }
    public float calculateMomentOfInertia(float mass) {
        // TODO: https://stackoverflow.com/questions/41592034/computing-tensor-of-inertia-in-2d/41618980#41618980
        return 5000;
    }

    public void update(Vector2f position, float rotation) {
        rotation %= Math.PI_TIMES_2_f;
        if (rotation < 0) rotation += Math.PI_TIMES_2_f;
        if (rotation == 0) {
            for (int i = 0; i < vertices_world.length; i++) {
                Vector2f vertex = vertices_world[i];
                vertex.set(vertices_local[i]).add(position); }
        } else for (int i = 0; i < vertices_world.length; i++) {
            Vector2f vertex = vertices_world[i];
            vertex.set(vertices_local[i]);
            U.rotate2D(vertex,rotation).add(position);
        }
    }




}
