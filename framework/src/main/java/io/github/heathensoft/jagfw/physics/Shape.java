package io.github.heathensoft.jagfw.physics;

import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * Physics Body Shape
 * Frederik Dahl 1/16/2025
 */
public abstract class Shape {

    /*
        Possible Todo: LineSegment (extends shape), Triangle (extends Polygon)

     */

    /**
     * Calculates the moment of inertia of shape based on Body mass
     * @param mass the mass of the Body
     * @return moment of inertia
     */
    public abstract float momentOfInertia(float mass);

    /**
     * Called every physics step to update position and rotation of shape.
     * @param position body world position
     * @param rotation body world rotation in radians
     */
    public void updateVertices(Vector2f position, float rotation) { }


    /**
     * A polygon is a convex shape with a number of vertices > 2.
     * Edges should be connected in the counter-clockwise order where the edge normal
     * is a vector pointing outwards
     */
    public static abstract class Polygon extends Shape {
        /** @return Vertices of Polygon Shape in world coordinates */
        public abstract Vector2f[] vertices();

        /**
         * calculate an axis aligned rectangle from the max and min x anf y
         * values of the polygon vertices. The bounding box contains the polygon.
         * @param dst the resulting bounding box
         * @return dst
         */
        // More complex polygons should have a bounding box rectangle field
        // And update the bounding box when updating it's vertices
        // so: return dst.set(bounding_box);
        public abstract Rectanglef boundingBox(Rectanglef dst);
        /**
         * Get the edge vector of polygon from v[index % len] -> v[(index + 1) % len]
         * The edges are in counter-clockwise order
         * @param index the polygon vertex
         * @param dst the vector to put the values into
         * @return the dst vector
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

    /**
     * A Circle Shape with radius r
     */
    public static class Circle extends Shape {
        public float radius;
        public Circle() { this(0.5f); }
        public Circle(float radius) { this.radius = radius; }
        public float momentOfInertia(float mass) {
            // For solid circles, the moment of inertia is 1/2 * r^2 * mass
            return 0.5f * (radius * radius) * mass;
        }
    }

    /**
     * A Box is a Polygon Shape with 4 vertices.
     * It has a width and a height.
     * The edges are in counter-clockwise order
     * starting from the top left vertex (un-rotated)
     */
    public static class Box extends Polygon {
        public float width;
        public float height;
        public final Vector2f[] vertices = new Vector2f[4];
        public Box(float size) { this(size,size); }
        public Box(float width, float height) {
            this.width = width;
            this.height = height;
            for (int i = 0; i < vertices.length; i++)
                vertices[i] = new Vector2f();
            /*
             * v0------v3
             * |        |
             * |        |
             * v1------v2
             */
            vertices[0].set(-(width / 2f), (height / 2f));
            vertices[1].set(-(width / 2f),-(height / 2f));
            vertices[2].set( (width / 2f),-(height / 2f));
            vertices[3].set( (width / 2f), (height / 2f));
        }

        public Vector2f[] vertices() { return vertices; }

        public Rectanglef boundingBox(Rectanglef dst) {
            // Don't do this for more complex polygons
            float min_x = Float.POSITIVE_INFINITY;
            float min_y = Float.POSITIVE_INFINITY;
            float max_x = Float.NEGATIVE_INFINITY;
            float max_y = Float.NEGATIVE_INFINITY;
            for (Vector2f vertex : vertices) {
                min_x = Math.min(min_x, vertex.x);
                min_y = Math.min(min_y, vertex.y);
                max_x = Math.max(max_x, vertex.x);
                max_y = Math.max(max_y, vertex.y);
            }
            dst.minX = min_x;
            dst.minY = min_y;
            dst.maxX = max_x;
            dst.maxY = max_y;
            return dst;
        }

        public float momentOfInertia(float mass) {
            // For a rectangle, the moment of inertia is 1/12 * (w^2 + h^2) * mass
            return (0.083333f) * (width * width + height * height) * mass;
        }

        /**
         * The Box is Axis Aligned when the
         * distance in x between vertex[0] and vertex[1] (left edge)
         * is either 0 (rotation = 0) or height (rotation = PI/2)
         * A sure way to keep Boxes axis aligned is to keep rotation at zero
         * Axis Aligned boxes are cheaper for collision detection
         * @return whether the Box is perfectly Axis aligned
         */
        public boolean isAxisAligned() {
            float dx = Math.abs(vertices[0].x - vertices[1].x);
            return dx == 0 || dx == height;
        }

        public void updateVertices(Vector2f position, float rotation) {
            final float wh = width * 0.5f;
            final float hh = height * 0.5f;
            float rot = rotation % Math.PI_TIMES_2_f;
            if (rot < 0) rot += Math.PI_TIMES_2_f;
            if (rot < 1e-3f) {
                /*
                    we only test for rotation ~= 0.
                    Don't want to check all other axis aligned
                    angles for special cases.
                    keep rotation set to 0 if you need
                    to avoid to calculate rotations each frame.
                 */
                vertices[0].set(-wh, hh).add(position);
                vertices[1].set(-wh,-hh).add(position);
                vertices[2].set( wh,-hh).add(position);
                vertices[3].set( wh, hh).add(position);
            } else {
                final float sin = Math.sin(rotation);
                final float cos = Math.cos(rotation);
                final float sin_wh = sin * wh;
                final float sin_hh = sin * hh;
                final float cos_wh = cos * wh;
                final float cos_hh = cos * hh;
                // First rotate, then translate
                vertices[0].x = -cos_wh - sin_hh + position.x;
                vertices[0].y = -sin_wh + cos_hh + position.y;
                vertices[1].x = -cos_wh + sin_hh + position.x;
                vertices[1].y = -sin_wh - cos_hh + position.y;
                vertices[2].x =  cos_wh + sin_hh + position.x;
                vertices[2].y =  sin_wh - cos_hh + position.y;
                vertices[3].x =  cos_wh - sin_hh + position.x;
                vertices[3].y =  sin_wh + cos_hh + position.y;
            }
        }

    }

}
