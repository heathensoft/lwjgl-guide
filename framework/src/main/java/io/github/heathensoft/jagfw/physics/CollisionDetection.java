package io.github.heathensoft.jagfw.physics;

import org.joml.Intersectionf;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.*;

/**
 * Frederik Dahl 1/16/2025
 */
public class CollisionDetection {


    public static boolean check(RigidBody a, RigidBody b, Contact contact_info) {
        if (a.isStatic() && b.isStatic()) return false;
        boolean a_polygon = a.shape instanceof Shape.Polygon;
        boolean b_polygon = b.shape instanceof Shape.Polygon;
        boolean a_circle  = a.shape instanceof Shape.Circle;
        boolean b_circle  = b.shape instanceof Shape.Circle;
        if (a_circle && b_circle) {
            return circleCircle(a,b,contact_info);
        }if (a_polygon && b_polygon) {
            return polyPoly(a,b,contact_info);
        }if (a_circle && b_polygon) {
            return polyCircle(b,a,contact_info);
        }if (a_polygon && b_circle) {
            return polyCircle(a,b,contact_info);
        } return false;
    }


    private static boolean circleCircle(RigidBody a, RigidBody b, Contact contact_info) {
        Shape.Circle circle_a = (Shape.Circle) a.shape;
        Shape.Circle circle_b = (Shape.Circle) b.shape;
        Vector2f ab = popSetVec2(b.position).sub(a.position);
        float radius_sum = circle_a.radius + circle_b.radius;
        boolean collision = ab.lengthSquared() <= (radius_sum * radius_sum);
        if (collision) {
            Vector2f start_end = popVec2();
            contact_info.bodyA = a;
            contact_info.bodyB = b;
            contact_info.normal.set(ab).normalize();
            contact_info.start.set(contact_info.normal).negate();
            contact_info.start.mul(circle_b.radius).add(b.position);
            contact_info.end.set(contact_info.normal);
            contact_info.end.mul(circle_a.radius).add(a.position);
            start_end.set(contact_info.end).sub(contact_info.start);
            contact_info.depth = start_end.length();
            pushVec2();
        } pushVec2();
        return collision;
    }

    private static boolean polyPoly(RigidBody a, RigidBody b, Contact contact_info) {
        Shape.Polygon a_polygon = (Shape.Polygon) a.shape;
        Shape.Polygon b_polygon = (Shape.Polygon) b.shape;

        /*
            Should check if polygons are boxes and if they are axis aligned.
            If they are we can do cheaper collision checks
         */


        {
            Vector2f a_edge_normal = popVec2();
            Vector2f b_edge_normal = popVec2();
            Vector2f a_point = popVec2();
            Vector2f b_point = popVec2();

            float ab_separation = findMinSeparation(a_polygon,b_polygon,a_edge_normal,a_point);
            if (ab_separation >= 0) {
                pushVec2(4);
                return false;
            }

            float ba_separation = findMinSeparation(b_polygon,a_polygon,b_edge_normal,b_point);
            if (ba_separation >= 0) {
                pushVec2(4);
                return false;
            }

            contact_info.bodyA = a;
            contact_info.bodyB = b;
            if (ab_separation >= ba_separation) {
                // best separation was from polygon a to polygon b
                // the penetration was bigger, (the b vertex corner inside a)
                contact_info.depth = -ab_separation;
                contact_info.normal.set(a_edge_normal);
                contact_info.start.set(a_point);
                contact_info.end.set(a_edge_normal).mul(contact_info.depth);
                contact_info.end.add(contact_info.start);
            } else {
                contact_info.depth = -ba_separation;
                contact_info.normal.set(b_edge_normal).negate();
                contact_info.end.set(b_point);
                contact_info.start.set(b_edge_normal).mul(contact_info.depth);
                contact_info.start.add(contact_info.end);
            }


            pushVec2(4);
        }

        return true;
    }

    private static boolean polyCircle(RigidBody a, RigidBody b, Contact contact_info) {

        return false;
    }


    private static float findMinSeparation(Shape.Polygon a, Shape.Polygon b, Vector2f edge_normal, Vector2f point) {
        float separation = Float.NEGATIVE_INFINITY;
        final Vector2f[] a_vertices = a.vertices();
        final Vector2f[] b_vertices = b.vertices();
        Vector2f v0 = popVec2();
        Vector2f v1 = popVec2();
        Vector2f min_vertex = new Vector2f();
        for (int i = 0; i < a_vertices.length; i++) {
            Vector2f va = a_vertices[i];
            Vector2f normal = a.edgeNormal(i,v0);
            float min_sep = Float.POSITIVE_INFINITY;
            for (Vector2f vb : b_vertices) {
                Vector2f ab = v1.set(vb).sub(va);
                float projection = ab.dot(normal);
                if (projection < min_sep) {
                    min_sep = projection;
                    min_vertex = vb;
                }
            }
            if (min_sep > separation) {
                separation = min_sep;
                edge_normal.set(normal);
                point.set(min_vertex);
            }
        }
        pushVec2(2);
        return separation;
    }


}
