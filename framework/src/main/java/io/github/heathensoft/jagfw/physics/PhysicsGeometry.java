package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 1/26/2025
 */
public class PhysicsGeometry {

    public final Vector2f[] vertices;
    public final Vector2f offset;
    public final LineSegment segment;
    public float restitution = 0.4f;
    public float friction = 0.0f;
    public boolean sleeping;
    public boolean polygon;
    public boolean one_way_collision;

    public PhysicsGeometry(Vector2f[] vertices) {
        this.segment = new LineSegment();
        this.offset = new Vector2f();
        this.vertices = vertices;
    }

    public PhysicsGeometry(List<Vector2f> vertices) {
        this.vertices = new Vector2f[vertices.size()];
        for (int i = 0; i < this.vertices.length; i++) {
            this.vertices[i] = vertices.get(i);
        } this.segment = new LineSegment();
        this.offset = new Vector2f();
    }

    public PhysicsGeometry(int capacity) {
        this.vertices = new Vector2f[capacity];
        for (int i = 0; i < capacity; i++) {
            this.vertices[i] = new Vector2f();
        } this.segment = new LineSegment();
        this.offset = new Vector2f();
    }

    public LineSegment segment(int i0, int i1) {
        Vector2f v0 = vertices[U.modRepeat(i0,vertices.length)];
        Vector2f v1 = vertices[U.modRepeat(i1,vertices.length)];
        segment.set(v0.x + offset.x,v0.y + offset.y,v1.x + offset.x ,v1.y + offset.y);
        return segment;
    }

    public LineSegment segment(int i0, int i1, LineSegment dst) {
        Vector2f v0 = vertices[U.modRepeat(i0,vertices.length)];
        Vector2f v1 = vertices[U.modRepeat(i1,vertices.length)];
        dst.set(v0.x + offset.x,v0.y + offset.y,v1.x + offset.x ,v1.y + offset.y);
        return dst;
    }

    public int closestVertex(Vector2f p) {
        return closestVertex(p.x,p.y);
    }

    public int closestVertex(float x, float y) {
        int index = 0;
        float min = Float.POSITIVE_INFINITY;
        if (offset.x == 0 && offset.y == 0) {
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
                float dx = (vertex.x + offset.x) - x;
                float dy = (vertex.y + offset.y) - y;
                float l2 = dx * dx + dy * dy;
                if (min > l2) {
                    min = l2;
                    index = i;
                }
            }
        }
        return index;
    }

    public Vector2f[] vertices() {
        return vertices;
    }

    public int numVertices() {
        return vertices.length;
    }

    public int numSegments() {
        if (vertices.length > 1) {
            if (vertices.length > 2) {
                if (polygon) {
                    return vertices.length;
                } else return vertices.length - 1;
            } return 1;
        } return 0;
    }

    public float restitution() {
        return restitution;
    }

    public void setRestitution(float restitution) {
        this.restitution = U.clamp(restitution);
    }

    public float friction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = U.clamp(friction);
    }

    public boolean isPolygon() {
        return vertices.length > 2 && polygon;
    }

    public void setPolygon(boolean enable) {
        polygon = enable;
    }

    public boolean isSleeping() {
        return sleeping;
    }

    public void sleep(boolean enable) {
        sleeping = enable;
    }
}
