package io.github.heathensoft.jagfw.physics.old;


import org.joml.Math;
import org.joml.Vector2f;


/**
 * Various Physics Shapes
 *
 * @author Frederik Dahl
 * 13/01/2025
 */


public abstract class Shape2 {
    
    /**
     * Calculates the moment of inertia of shape based on Body mass
     * @param mass the mass of the Body
     * @return moment of inertia
     */
    public abstract float momentOfInertia(float mass);


    public static void main(String[] args) {

        Box box = new Box(1);

        for (int i = 0; i < 4; i++) {
            Vector2f edge = box.edge(i,new Vector2f());
            Vector2f edge_normal = new Vector2f(edge);
            edge_normal.perpendicular().normalize();
            System.out.print("Edge " + i + ": ");
            System.out.print("[ "+edge.x+", "+edge.y+" ]");
            System.out.print(" -- Normal: ");
            System.out.println("[ "+edge_normal.x+", "+edge_normal.y+" ]");
        }

    }
    
    
    public static abstract class PolygonShape extends Shape2 {
        
        /** @return Vertices of Polygon Shape in world coordinates */
        public abstract Vector2f[] vertices();
        
        /**
         * Called every physics step to update position and rotation of shape.
         * @param position body world position
         * @param rotation body world rotation in radians
         */
        public abstract void updateVertices(Vector2f position, float rotation);
        
        /**
         * Get the edge vector of polygon from v[index % len] -> v[(index + 1) % len]
         * @param index the polygon vertex
         * @param dst the destination vector
         * @return dst (edge)
         */
        public Vector2f edge(int index, Vector2f dst) {
            Vector2f[] vertices = vertices();
            int len = vertices.length;
            Vector2f v0 = vertices[index % len];
            Vector2f v1 = vertices[(index + 1) % len];
            return dst.set(v1).sub(v0);
        }
    }
    
    public static class Circle extends Shape2 {
        public float radius;
        public Circle() { this(0.5f); }
        public Circle(float radius) { this.radius = radius; }
        public float momentOfInertia(float mass) {
            // For solid circles, the moment of inertia is 1/2 * r^2
            // But this still needs to be multiplied by the rigidbody's mass
            return 0.5f * (radius * radius) * mass;
        }
    }
    
    public static class Box extends PolygonShape {
        public float width;
        public float height;
        public final Vector2f[] vertices = new Vector2f[4];
        public Box(float size) { this(size,size); }
        public Box(float width, float height) {
            this.width = width;
            this.height = height;
            for (int i = 0; i < vertices.length; i++)
                vertices[i] = new Vector2f();
            vertices[0].set(-(width / 2f), (height / 2f));
            vertices[1].set(-(width / 2f),-(height / 2f));
            vertices[2].set( (width / 2f),-(height / 2f));
            vertices[3].set( (width / 2f), (height / 2f));
            /*
            localVertices.push_back(Vec2(-width / 2.0, -height / 2.0));
            localVertices.push_back(Vec2(+width / 2.0, -height / 2.0));
            localVertices.push_back(Vec2(+width / 2.0, +height / 2.0));
            localVertices.push_back(Vec2(-width / 2.0, +height / 2.0));
             */
        }
        public float momentOfInertia(float mass) {
            // For a rectangle, the moment of inertia is 1/12 * (w^2 + h^2)
            // But this still needs to be multiplied by the rigidbody's mass
            return (0.083333f) * (width * width + height * height) * mass;
        }
        
        public Vector2f[] vertices() { return vertices; }
        
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
            // First rotate, then we translate
            final float wh = width * 0.5f;
            final float hh = height * 0.5f;
            float rot = rotation % Math.PI_TIMES_2_f;
            if (rot < 0) rot += Math.PI_TIMES_2_f;
            if (rot < 1e-3f) {
                /*
                    we only test for rotation ~= 0
                    Don't want to check all other axis aligned
                    angles for special cases.
                    
                    keep rotation set to 0 if you need
                    to avoid to calculate rotation every frame.
                    
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
    
    //public static class Polygon extends Shape {
    //    public Vector2f[] local_vertices;
    //    public Vector2f[] world_vertices;
    //    public float momentOfInertia(float mass) {
    //        return 0;
    //    } public void updateVertices(Vector2f position, float rotation) {
    //        // First rotate, then we translate
    //        float sin = Math.sin(rotation);
    //        float cos = Math.cos(rotation);
    //        for (int i = 0; i < local_vertices.length; i++) {
    //            float local_x = local_vertices[i].x;
    //            float local_y = local_vertices[i].y;
    //            world_vertices[i].x = (local_x * cos - local_y * sin) + position.x;
    //            world_vertices[i].y = (local_x * sin + local_y * cos) + position.y;
    //        }
    //    }
    //}
    
    

}
