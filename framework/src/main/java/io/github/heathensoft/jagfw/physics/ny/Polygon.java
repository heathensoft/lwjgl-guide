package io.github.heathensoft.jagfw.physics.ny;

import io.github.heathensoft.jagfw.utils.LineSegment;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 1/30/2025
 */
public abstract class Polygon extends Shape {

    public float rotation;

    /** @return Vertices of Polygon in world coordinates */
    public abstract Vector2f[] vertices();


    public void translate(float x, float y) {
        Vector2f[] vertices = vertices();
        for (Vector2f vertex : vertices) {
            vertex.add(x,y);
        }
    }

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

    public Vector2f edgeVector(int index) {
        return edgeVector(index,new Vector2f());
    }

    public LineSegment edgeSegment(int index, LineSegment dst) {
        Vector2f[] vertices = vertices();
        int len = vertices.length;
        Vector2f v0 = vertices[index % len];
        Vector2f v1 = vertices[(index + 1) % len];
        dst.set(v0,v1);
        return dst;
    }

    public LineSegment edgeSegment(int index) {
        Vector2f[] vertices = vertices();
        int len = vertices.length;
        Vector2f v0 = vertices[index % len];
        Vector2f v1 = vertices[(index + 1) % len];
        return new LineSegment(v0,v1);
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

    public Vector2f edgeNormal(int index) {
        Vector2f edge = edgeVector(index);
        return edge.perpendicular().normalize();
    }

    public Vector2f calculateCenter() {
        Vector2f[] vertices = vertices();
        Vector2f center = new Vector2f();
        for (Vector2f vertex : vertices) {
            center.add(vertex);
        } return center.div(vertices.length);
    }

    public Rectanglef calculateBoundingBox() {
        float min_x = Float.POSITIVE_INFINITY;
        float min_y = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY;
        float max_y = Float.NEGATIVE_INFINITY;
        Vector2f[] vertices = vertices();
        for (Vector2f vertex : vertices) {
            min_x = org.joml.Math.min(min_x, vertex.x);
            min_y = org.joml.Math.min(min_y, vertex.y);
            max_x = org.joml.Math.max(max_x, vertex.x);
            max_y = Math.max(max_y, vertex.y);}
        return new Rectanglef(min_x,min_y,max_x,max_y);
    }

    public int numVertices() {
        return vertices().length;
    }

}
