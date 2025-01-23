package io.github.heathensoft.jagfw.physics.ny.shape;

import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * A Polygon is a convex Shape with a number of vertices > 2.
 * Edges should be connected in counter-clockwise order where the edge normal
 * is a vector pointing outwards
 * Frederik Dahl 1/23/2025
 */
public abstract class PolygonShape extends Shape {

    /** @return Vertices of Polygon Shape in world coordinates */
    public abstract Vector2f[] vertices();

    public void translate(Vector2f translation) {
        Vector2f[] vertices = vertices();
        for (Vector2f vertex : vertices) {
            vertex.add(translation);
        }
    }
    /**
     * An axis aligned rectangle from the max and min x and y
     * values of the Polygon vertices. The bounding box will contain the Polygon.
     * @param dst the resulting bounding box
     * @return dst
     */
    public abstract Rectanglef boundingBox(Rectanglef dst);

    /**
     * Get the edge vector of polygon from v[index % len] -> v[(index + 1) % len]
     * The edges are in counter-clockwise order
     * @param index the polygon vertex
     * @param dst the vector to put the values into
     * @return dst
     */
    public Vector2f edgeVector(int index, Vector2f dst) {
        Vector2f[] vertices = vertices();
        int len = vertices.length;
        Vector2f v0 = vertices[index % len];
        Vector2f v1 = vertices[(index + 1) % len];
        return dst.set(v1).sub(v0);
    }

    /**
     * Get the normal of the edge vector of polygon
     * (index % len) -> ((index + 1) % len)
     * The edges are in counter-clockwise order and
     * the normal is pointing outwards
     * @param index the index of the edge
     * @param dst the vector to put the values into
     * @return the dst vector
     */
    public Vector2f edgeNormal(int index, Vector2f dst) {
        Vector2f edge = edgeVector(index,dst);
        return edge.perpendicular().normalize();
    }

    public int numVertices() {
        return vertices().length;
    }
}
