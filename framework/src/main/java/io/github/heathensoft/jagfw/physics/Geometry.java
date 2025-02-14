package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

import java.util.List;

/**
 * Collidable environment.
 * Geometry is "static" (Not affected by forces)
 * Frederik Dahl 1/30/2025
 */
public class Geometry {

    public static final float DEFAULT_FRICTION = 0.05f;
    public static final float DEFAULT_RESTITUTION = 0.3f;

    /**
     * Polygon vertices in "local space" (untranslated)
     */
    public final Vector2f[] vertices;
    /**
     * Geometry translation (Offset in position, added to all vertices)
     */
    public final Vector2f translation;
    /**
     * The first vertex is also the last (loops around).
     * Only works for vertices length > 2.
     */
    public boolean should_treat_as_polygon;
    /**
     * IF true, bodies only collide with the geometry in one direction.
     */
    public boolean one_way_collision;

    /**
     * "Bounciness" of geometry (should be in the range of 0 to 1)
     */
    public float restitution = DEFAULT_RESTITUTION;

    /**
     * Friction of geometry (should be in the range of 0 to 1)
     * (0 is like "ice")
     */
    public float friction = DEFAULT_FRICTION;

    public Geometry(Vector2f[] vertices) {
        this.translation = new Vector2f();
        this.vertices = vertices;
    }

    public Geometry(List<Vector2f> vertices) {
        this.vertices = new Vector2f[vertices.size()];
        for (int i = 0; i < this.vertices.length; i++) {
            this.vertices[i] = vertices.get(i);
        } this.translation = new Vector2f();
    }

    public Geometry(int capacity) {
        this.vertices = new Vector2f[capacity];
        for (int i = 0; i < capacity; i++) {
            this.vertices[i] = new Vector2f();
        } this.translation = new Vector2f();
    }

    /**
     * Get line segment between vertices
     * (translation applied)
     * @param i0 index of vertex 0
     * @param i1 index of vertex 1
     * @return new line segment
     */
    public LineSegment segment(int i0, int i1) {
        return segment(i0,i1,new LineSegment());
    }

    /**
     * Get line segment between vertices
     * (translation applied)
     * @param i0 index of vertex 0
     * @param i1 index of vertex 1
     * @param dst line segment
     * @return dst
     */
    public LineSegment segment(int i0, int i1, LineSegment dst) {
        Vector2f v0 = vertices[U.modRepeat(i0,vertices.length)];
        Vector2f v1 = vertices[U.modRepeat(i1,vertices.length)];
        dst.set(v0.x + translation.x,v0.y + translation.y,v1.x + translation.x ,v1.y + translation.y);
        return dst;
    }

    /**
     * Get the index of the closest vertex for x and y
     * (translation applied)
     * @param x x
     * @param y y
     * @return the vertex index
     */
    public int closestVertex(float x, float y) {
        int index = 0;
        float min = Float.POSITIVE_INFINITY;
        if (translation.x == 0 && translation.y == 0) {
            for (int i = 0; i < vertices.length; i++) {
                Vector2f vertex = vertices[i];
                float dx = vertex.x - x;
                float dy = vertex.y - y;
                float l2 = dx * dx + dy * dy;
                if (min > l2) {
                    min = l2;
                    index = i;
                }
            }
        } else {
            for (int i = 0; i < vertices.length; i++) {
                Vector2f vertex = vertices[i];
                float dx = (vertex.x + translation.x) - x;
                float dy = (vertex.y + translation.y) - y;
                float l2 = dx * dx + dy * dy;
                if (min > l2) {
                    min = l2;
                    index = i;
                }
            }
        }
        return index;
    }

    /**
     * Get the index of the closest vertex for p
     * (translation applied)
     * @param p p
     * @return the vertex index
     */
    public int closestVertex(Vector2f p) {
        return closestVertex(p.x,p.y);
    }

    /**
     * @return true if geometry should be treated as a polygon (closed geometry)
     * and vertices.length() > 2
     */
    public boolean isPolygon() {
        return vertices.length > 2 && should_treat_as_polygon;
    }

    /**
     * @return length of vertices array (vertices.length())
     */
    public int numVertices() {
        return vertices.length;
    }

    /**
     * @return the number of individual line segments making up the geometry.
     * 0 if vertices.length() <= 1
     * 1 if vertices.length() == 2
     * else:
     * vertices.length() if treated as polygon
     * else vertices.length() - 1
     */
    public int numSegments() {
        if (vertices.length > 1) {
            if (vertices.length > 2) {
                if (should_treat_as_polygon) {
                    return vertices.length;
                } else return vertices.length - 1;
            } return 1;
        } return 0;
    }

}
