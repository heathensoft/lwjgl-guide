package io.github.heathensoft.jagfw.core.utils;

import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * A line segment is a line between two points.
 * The segment is directed from p0 to p1*
 * Frederik Dahl 1/24/2025
 */
public class LineSegment {

    public float x0;
    public float y0;
    public float x1;
    public float y1;

    public LineSegment() {
        this.x0 = 0;
        this.y0 = 0;
        this.x1 = 1;
        this.y1 = 0;
    }

    public LineSegment(Vector2f p0, Vector2f p1) {
        set(p0.x,p0.y,p1.x,p1.y);
    }

    public LineSegment(float x0, float y0, float x1, float y1) {
        set(x0,y0,x1,y1);
    }

    public void set(float x0, float y0, float x1, float y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    public void set(LineSegment l) {
        this.x0 = l.x0;
        this.y0 = l.y0;
        this.x1 = l.x1;
        this.y1 = l.y1;
    }

    public void set(Vector2f p0, Vector2f p1) {
        this.x0 = p0.x;
        this.y0 = p0.y;
        this.x1 = p1.x;
        this.y1 = p1.y;
    }

    public void setP0(Vector2f p0) {
        this.x0 = p0.x;
        this.y0 = p0.y;
    }

    public void setP1(Vector2f p1) {
        this.x1 = p1.x;
        this.y1 = p1.y;
    }

    public void flip() {
        float tmp = x0;
        x0 = x1;
        x1 = tmp;
        tmp = y0;
        y0 = y1;
        y1 = tmp;
    }


    public Vector2f p0(Vector2f dst) {
        return dst.set(x0,y0);
    }

    public Vector2f p0() { return p0(new Vector2f()); }

    public Vector2f p1(Vector2f dst) {
        return dst.set(x1,y1);
    }

    public Vector2f p1() { return p1(new Vector2f()); }

    public Vector2f normal(Vector2f dst) {
        return direction(dst).perpendicular();
    }

    public Vector2f normal() { return normal(new Vector2f()); }

    public Vector2f vector(Vector2f dst) {
        return dst.set(x1,y1).sub(x0,y0);
    }

    public Vector2f vector() { return vector(new Vector2f()); }

    public Vector2f direction(Vector2f dst) {
        return dst.set(x1,y1).sub(x0,y0).normalize();
    }

    public Vector2f direction() { return direction(new Vector2f()); }

    public Vector2f center(Vector2f dst) {
        return dst.set((x0 + x1) * 0.5f,(y0 + y1) * 0.5f);
    }

    public Vector2f center() { return center(new Vector2f()); }

    public Vector2f interpolate(float t, Vector2f dst) {
        dst.x = U.lerp(x0,x1,t);
        dst.y = U.lerp(y0,y1,t);
        return dst;
    }

    public Rectanglef boundingBox(Rectanglef dst) {
        float minX = Math.min(x0,x1);
        float maxX = Math.max(x0,x1);
        float minY = Math.min(y0,y1);
        float maxY = Math.max(y0,y1);
        dst.setMin(minX,minY);
        dst.setMax(maxX,maxY);
        return dst;
    }

    public Vector2f closestPoint(float px, float py, Vector2f dst) {
        return closestPoint(this, px, py, dst);
    }

    public Vector2f closestPoint(Vector2f point, Vector2f dst) {
        return closestPoint(this, point.x, point.y, dst);
    }

    public float centerX() {
        return (x0 + x1) * 0.5f;
    }

    public float centerY() {
        return (y0 + y1) * 0.5f;
    }

    public float lengthSquared() {
        final float dx = x1 - x0;
        final float dy = y1 - y0;
        return dx * dx + dy * dy;
    }

    public float length() {
        final float dx = x1 - x0;
        final float dy = y1 - y0;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isValid() {
        return  !(x0 == x1 && y0 == y1);
    }

    public boolean intersects(LineSegment l, Vector2f dst) {
        return intersects(x0,y0,x1,y1,l.x0,l.y0,l.x1,l.y1,dst);
    }


    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x0);
        result = prime * result + Float.floatToIntBits(y0);
        result = prime * result + Float.floatToIntBits(x1);
        result = prime * result + Float.floatToIntBits(y1);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LineSegment other = (LineSegment) obj;
        if (Float.floatToIntBits(x0) != Float.floatToIntBits(other.x0))
            return false;
        if (Float.floatToIntBits(y0) != Float.floatToIntBits(other.y0))
            return false;
        if (Float.floatToIntBits(x1) != Float.floatToIntBits(other.x1))
            return false;
        if (Float.floatToIntBits(y1) != Float.floatToIntBits(other.y1))
            return false;
        return true;
    }

    public static Vector2f closestPoint(final LineSegment segment, float px, float py, Vector2f dst) {
        final float a = px - segment.x0;
        final float b = py - segment.y0;
        final float c = segment.x1 - segment.x0;
        final float d = segment.y1 - segment.y0;
        final float dot = a * c + b * d;
        final float l2 = c * c + d * d;
        float param = l2 == 0 ? -1 : dot / l2;
        if (param < 0) return segment.p0(dst);
        else if (param > 1) return segment.p1(dst);
        else return dst.set(segment.x0 + param * c, segment.y0 + param * d);
    }

    public static boolean intersects(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, Vector2f dst) {
        // https://www.youtube.com/watch?v=fHOLQJo0FjQ&t=8s
        float den = ((dy - cy) * (bx - ax)) - ((dx - cx) * (by - ay));
        if (den == 0) return false; // parallel
        float num_t = ((dx - cx) * (ay - cy)) - ((dy - cy) * (ax - cx));
        float t = num_t / den;
        if (t >= 0 && t <= 1) {
            float num_u = ((cy - ay) * (ax - bx)) - ((cx - ax) * (ay - by));
            float u = num_u / den;
            if (u >= 0 && u <= 1) {
                dst.x = ax * (1-t) + bx * t;
                dst.y = ay * (1-t) + by * t;
                return true;
            } return false;
        } return false;
    }

}
