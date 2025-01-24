package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.physics.shape.Circle;
import io.github.heathensoft.jagfw.physics.shape.PolygonShape;
import org.joml.Intersectionf;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

import static io.github.heathensoft.jagfw.utils.U.*;
import static io.github.heathensoft.jagfw.utils.U.pushVec2;

/**
 * Frederik Dahl 1/23/2025
 */
public class CollisionDetection {



    public static boolean bodyBody(PhysicsBody A, PhysicsBody B, BodyContact contact) {
        if (!(A.isStatic() && B.isStatic())) {
            boolean a_poly = A.shape instanceof PolygonShape;
            boolean b_poly = B.shape instanceof PolygonShape;
            boolean a_circ  = A.shape instanceof Circle;
            boolean b_circ  = B.shape instanceof Circle;
            if (a_circ && b_circ) return circleCircle(A,B,contact);
            if (a_poly && b_poly) return polyPoly(A,B,contact);
            if (a_circ && b_poly) return polyCircle(B,A,contact);
            if (a_poly && b_circ) return polyCircle(A,B,contact);
        } return false;
    }

    private static boolean circleCircle(PhysicsBody A, PhysicsBody B, BodyContact contact) {
        Circle circle_a = (Circle) A.shape;
        Circle circle_b = (Circle) B.shape;
        if (circleCircle(A.position,circle_a.radius(),B.position,circle_b.radius())) {
            Vector2f ab = popSetVec2(B.position).sub(A.position);
            Vector2f start_end = popVec2();
            contact.A = A;
            contact.B = B;
            contact.normal.set(ab).normalize();
            contact.start.set(contact.normal).negate();
            contact.start.mul(circle_b.radius()).add(B.position);
            contact.end.set(contact.normal);
            contact.end.mul(circle_a.radius()).add(A.position);
            start_end.set(contact.end).sub(contact.start);
            contact.depth = start_end.length();
            pushVec2(2);
            return true;
        } return false;
    }

    private static boolean polyPoly(PhysicsBody A, PhysicsBody B, BodyContact contact) {
        PolygonShape a_poly = (PolygonShape) A.shape;
        PolygonShape b_poly = (PolygonShape) B.shape;
        {
            boolean possible_intersection = circleCircle(
                    A.position,a_poly.radius(),
                    B.position,b_poly.radius());
            if (!possible_intersection) {
                return false;
            }
        }{
            Vector2f a_point = popVec2();
            Vector2f b_point = popVec2();
            Vector2f a_edge_normal = popVec2();
            Vector2f b_edge_normal = popVec2();
            float ab_separation = findMinSeparation(a_poly,b_poly,a_edge_normal,a_point);
            if (ab_separation >= 0) {
                pushVec2(4);
                return false;
            }
            float ba_separation = findMinSeparation(b_poly,a_poly,b_edge_normal,b_point);
            if (ba_separation >= 0) {
                pushVec2(4);
                return false;
            }
            contact.A = A;
            contact.B = B;
            if (ab_separation >= ba_separation) {
                // best separation was from polygon a to polygon b
                // the penetration was bigger, (the b vertex corner inside a)
                contact.depth = -ab_separation;
                contact.normal.set(a_edge_normal);
                contact.start.set(a_point);
                contact.end.set(a_edge_normal).mul(contact.depth);
                contact.end.add(contact.start);
            } else {
                contact.depth = -ba_separation;
                contact.normal.set(b_edge_normal).negate();
                contact.end.set(b_point);
                contact.start.set(b_edge_normal).mul(contact.depth);
                contact.start.add(contact.end);
            } pushVec2(4);
        } return true;
    }

    private static boolean polyCircle(PhysicsBody A, PhysicsBody B, BodyContact contact) {
        PolygonShape polygon = (PolygonShape) A.shape;
        Circle circle = (Circle) B.shape;
        Vector2f circle_position = B.position;
        Vector2f polygon_position = A.position;

        {
            // ###############################
            // Check if a collision is possible
            // with a cheaper check using the
            // polygons bounding radius
            // ###############################

            boolean possible_intersection = circleCircle(
                    circle_position,circle.radius(),
                    polygon_position,polygon.radius());
            if (!possible_intersection) return false;
        }

        /*
         * Todo: Need to test with more complex polygons
         * From here, i think the code might only apply for rectangles and triangles
         * Check the todo below
         */

        Vector2f edge_v0 = null;
        Vector2f edge_v1 = null;
        Vector2f[] vertices = polygon.vertices();
        // Distance from circle to edge
        float dist = Float.POSITIVE_INFINITY;

        {
            // ###############################
            // Find and store the closest edge
            // And the distance to the circle
            // ###############################

            Vector2f tmp0 = popVec2();
            Vector2f tmp1 = popVec2();
            for (int i = 0; i < vertices.length; i++) {
                Vector2f vertex_curr = vertices[i];
                Vector2f vertex_next = vertices[(i + 1) % vertices.length];
                Vector2f edge_normal = tmp0.set(vertex_next).sub(vertex_curr);
                edge_normal.perpendicular().normalize();
                Vector2f to_circle_center = tmp1.set(circle_position).sub(vertex_curr);
                // project the circle center onto the edge normal
                float projection = to_circle_center.dot(edge_normal);
                if (projection > 0) {
                    /*
                        Todo:
                        This is the reason why i think it only applies
                        For rectangles and triangle polygon shapes.
                        For rectangles and triangles only one edge will
                        have a positive projection (dot product)
                        But for polygons with more edges, i think
                        we need to store the max projection instead
                        of breaking out.
                     */
                    // circle center is outside the polygon atp
                    dist = projection;
                    edge_v0 = vertex_curr;
                    edge_v1 = vertex_next;
                    break;
                } else {
                    // circle center is inside rectangle atp.
                    // (including on the edge, projection == 0)
                    // we store the closest edge vertices
                    // (counter-clockwise)
                    if (abs(projection) < abs(dist)) {
                        dist = projection;
                        edge_v0 = vertex_curr;
                        edge_v1 = vertex_next;
                    }
                }
            }
            pushVec2(2);
        }

        // They shouldn't be atp.
        // (polygons have at least 3 edges)
        if (edge_v0 == null) return false;
        if (edge_v1 == null) return false;

        // negative distance if circle center is inside polygon
        boolean outside = dist > 0;

        try {

            Vector2f tmp0 = popVec2();
            Vector2f tmp1 = popVec2();
            Vector2f tmp2 = popVec2();

            if (outside) {

                // ###############################
                // Outside
                // ###############################
                /*
                 *  The regions are the "outside" rectangles formed
                 *  by the edges of the polygon (think rectangle)
                 *  A and C are the corners
                 */
                Vector2f v0_circle = tmp0.set(circle_position).sub(edge_v0);
                Vector2f v1_circle = tmp1.set(circle_position).sub(edge_v1);
                Vector2f v0_v1 = tmp2.set(edge_v1).sub(edge_v0);

                if (v1_circle.dot(v0_v1) > 0) {

                    // ###############################
                    // Inside region A (Corner)
                    // ###############################

                    float v1_circle_len = v1_circle.length();
                    if (v1_circle_len > circle.radius()) {
                        // not colliding with corner
                        return false;
                    }
                    // Collision Region A
                    contact.A = A;
                    contact.B = B;
                    contact.depth = circle.radius() - v1_circle_len;
                    contact.normal.set(v1_circle).normalize();
                    contact.start.set(contact.normal);
                    contact.start.mul(-circle.radius());
                    contact.start.add(circle_position);
                    contact.end.set(contact.normal).mul(contact.depth);
                    contact.end.add(contact.start);
                    return true;

                } else if (v0_circle.dot(v0_v1) < 0) {

                    // ###############################
                    // Inside region C (Corner)
                    // ###############################

                    float v0_circle_len = v1_circle.length();
                    if (v0_circle_len > circle.radius()) {
                        // not colliding with corner
                        return false;
                    }
                    // Collision Region C
                    contact.A = A;
                    contact.B = B;
                    contact.depth = circle.radius() - v0_circle_len;
                    contact.normal.set(v0_circle).normalize();
                    contact.start.set(contact.normal);
                    contact.start.mul(-circle.radius());
                    contact.start.add(circle_position);
                    contact.end.set(contact.normal).mul(contact.depth);
                    contact.end.add(contact.start);
                    return true;

                } else {
                    // ###############################
                    // Inside region B (Center)
                    // ###############################

                    if (dist > circle.radius()) {
                        // not colliding with edge
                        return false;
                    }
                    // Collision Region B
                    contact.A = A;
                    contact.B = B;
                    contact.depth = circle.radius() - dist;
                    contact.normal.set(v0_v1).perpendicular().normalize();
                    contact.start.set(contact.normal);
                    contact.start.mul(-circle.radius());
                    contact.start.add(circle_position);
                    contact.end.set(contact.normal).mul(contact.depth);
                    contact.end.add(contact.start);
                    return true;
                }
            } else {

                // ###############################
                // Inside
                // ###############################
                /*
                 *  NOTE:
                 *  Circles inside a polygon can get stuck.
                 *  It seems to collide trying to get out.
                 *  (Has to be completely inside)
                 */
                Vector2f v0_v1 = tmp0.set(edge_v1).sub(edge_v0);
                contact.A = A;
                contact.B = B;
                contact.depth = circle.radius() + dist;
                contact.normal.set(v0_v1).perpendicular().normalize();
                contact.start.set(contact.normal);
                contact.start.mul(-circle.radius());
                contact.start.add(circle_position);
                contact.end.set(contact.normal).mul(contact.depth);
                contact.end.add(contact.start);
                return true;
            }
        } finally {
            pushVec2(3);
        }
    }

    /**
     * Look up SAT (Separating Axis Theorem)
     */
    private static float findMinSeparation(PolygonShape a, PolygonShape b, Vector2f edge_normal, Vector2f point) {
        float separation = Float.NEGATIVE_INFINITY;
        final Vector2f[] a_vertices = a.vertices();
        final Vector2f[] b_vertices = b.vertices();
        Vector2f tmp0 = popVec2();
        Vector2f tmp1 = popVec2();
        Vector2f tmp2 = popVec2();
        Vector2f min_vertex = tmp2.set(0,0);
        for (int i = 0; i < a_vertices.length; i++) {
            Vector2f va = a_vertices[i];
            Vector2f normal = a.edgeNormal(i,tmp0);
            float min_sep = Float.POSITIVE_INFINITY;
            for (Vector2f vb : b_vertices) {
                Vector2f ab = tmp1.set(vb).sub(va);
                float projection = ab.dot(normal);
                if (projection < min_sep) {
                    min_sep = projection;
                    min_vertex = vb;
                } else if (projection == min_sep) {
                    // When we have to boxes colliding completely in parallel
                    // We set the pont to the center of the edge
                    min_vertex = new Vector2f(min_vertex);
                    min_vertex.add(vb).div(2);
                }
            }
            if (min_sep > separation) {
                separation = min_sep;
                edge_normal.set(normal);
                point.set(min_vertex);
            }
        }
        pushVec2(3);
        return separation;
    }

    private static boolean circleCircle(Vector2f a, float ra, Vector2f b, float rb) {
        final float dx = b.x - a.x;
        final float dy = b.y - a.y;
        final float r = ra + rb;
        return  (dx * dx + dy * dy) <= (r * r);
    }

    private static boolean circleRect(Vector2f a, float ra, Rectanglef b) {
        return Intersectionf.testAarCircle(b.minX,b.minY,b.maxX,b.maxY,a.x,a.y,ra*ra);
    }

    private static boolean rectRect(Rectanglef a, Rectanglef b) {
        return a.intersectsRectangle(b);
    }

}
