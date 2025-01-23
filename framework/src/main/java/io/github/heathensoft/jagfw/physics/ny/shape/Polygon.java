package io.github.heathensoft.jagfw.physics.ny.shape;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 1/23/2025
 */
public class Polygon extends PolygonShape {

    protected Vector2f[] vertices_local;
    protected Vector2f[] vertices_world;
    protected Rectanglef bounding_box;

    /**
     * Construct Polygon.
     * Polygon must be convex for collision detection.
     * Num vertices > 2, in counter-clockwise order.
     * @param vertices local vertices (origin centered)
     */
    public Polygon(Vector2f[] vertices) {
        if (vertices.length <= 2) {
            throw new IllegalStateException("A Polygon needs > 2 vertices");
        } vertices_local = vertices;
        vertices_world = new Vector2f[vertices.length];
        float min_x = Float.POSITIVE_INFINITY;
        float min_y = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY;
        float max_y = Float.NEGATIVE_INFINITY;
        float max_len = 0;
        for (int i = 0; i < vertices.length; i++) {
            Vector2f vertex = vertices[i];
            max_len = Math.max(max_len,vertex.length());
            min_x = Math.min(min_x, vertex.x);
            min_y = Math.min(min_y, vertex.y);
            max_x = Math.max(max_x, vertex.x);
            max_y = Math.max(max_y, vertex.y);
            vertices_world[i] = new Vector2f(vertex);
        } bounding_box = new Rectanglef(min_x,min_y,max_x,max_y);
        radius = max_len;

    }

    public Vector2f[] vertices() { return vertices_world; }

    public Rectanglef boundingBox(Rectanglef dst) { return dst.set(bounding_box); }

    public float calculateMomentOfInertia(float mass) {
        /*
            Har det her fra
            https://stackoverflow.com/questions/41592034/computing-tensor-of-inertia-in-2d/41618980#41618980
            We loop all the vertices finding the total area
            and combining the centroids (center of gravity)
            of all the triangles of the original polygon.
            Since the polygon center (local vertices)
            is in the origin 0,0. We can simplify
            thw stack overflow version
         */
        float acc0 = 0;
        float acc1 = 0;
        final int n = numVertices();
        for (int i = 0; i < n; i++) {
            Vector2f v0 = vertices_local[i];
            Vector2f v1 = vertices_local[(i + 1) % n];
            // The cross product in 2D is effectively a
            // third vector with a magnitude the same
            // as the rectangular area of the 2 vectors.
            float cross_product = Math.abs(U.cross(v0,v1));
            float dot = v0.dot(v0) + v1.dot(v1) + v0.dot(v1);
            acc0 += cross_product * dot;
            acc1 += cross_product;
        } return acc0 / 6 / acc1;
    }


    public void translate(Vector2f translation) {
        super.translate(translation);
        bounding_box.translate(translation);
    }

    public void updateVertices(Vector2f position, float rotation) {
        float min_x = Float.POSITIVE_INFINITY;
        float min_y = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY;
        float max_y = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertices_world.length; i++) {
            Vector2f vertex = vertices_world[i];
            vertex.set(vertices_local[i]);
            U.rotate2D(vertex,rotation).add(position);
            min_x = Math.min(min_x, vertex.x);
            min_y = Math.min(min_y, vertex.y);
            max_x = Math.max(max_x, vertex.x);
            max_y = Math.max(max_y, vertex.y);
        }
        bounding_box.minX = min_x;
        bounding_box.minY = min_y;
        bounding_box.maxX = max_x;
        bounding_box.maxY = max_y;
    }
}
