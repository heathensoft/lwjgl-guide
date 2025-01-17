package io.github.heathensoft.jagfw.physics;

import org.joml.Intersectionf;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

import static io.github.heathensoft.jagfw.utils.U.*;

/**
 * Frederik Dahl 1/16/2025
 */
public class CollisionDetection {


    public static boolean bodyBody(RigidBody a, RigidBody b, Contact contact_info) {
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

        {
            // NOTE: very little impact if any on fps
            // should test with more complex shapes
            boolean possible_intersection;
            Rectanglef aar_a = a_polygon.boundingBox(popRect());
            Rectanglef aar_b = b_polygon.boundingBox(popRect());
            possible_intersection = rectRect(aar_a,aar_b);
            pushRect(2);
            if (!possible_intersection) {
                return false;
            }
        }

        {
            Vector2f a_point = popVec2();
            Vector2f b_point = popVec2();
            Vector2f a_edge_normal = popVec2();
            Vector2f b_edge_normal = popVec2();
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
        } return true;
    }

    private static boolean polyCircle(RigidBody a, RigidBody b, Contact contact_info) {
        Shape.Polygon polygon = (Shape.Polygon) a.shape;
        Shape.Circle circle = (Shape.Circle) b.shape;
        Vector2f circle_position = b.position;

        {
            // ###############################
            // Check if a collision is possible
            // with a cheaper check using the
            // polygons bounding box (AAR)
            // ###############################

            boolean possible_intersection;
            Rectanglef aar = polygon.boundingBox(popRect());
            possible_intersection = circleRect(circle_position,circle.radius,aar);
            pushRect();
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

                    if (v1_circle_len > circle.radius) {
                        // not colliding with corner
                        return false;
                    }

                    // Collision Region A
                    contact_info.bodyA = a;
                    contact_info.bodyB = b;
                    contact_info.depth = circle.radius - v1_circle_len;
                    contact_info.normal.set(v1_circle).normalize();
                    contact_info.start.set(contact_info.normal);
                    contact_info.start.mul(-circle.radius);
                    contact_info.start.add(circle_position);
                    contact_info.end.set(contact_info.normal).mul(contact_info.depth);
                    contact_info.end.add(contact_info.start);
                    return true;

                } else if (v0_circle.dot(v0_v1) < 0) {

                    // ###############################
                    // Inside region C (Corner)
                    // ###############################

                    float v0_circle_len = v1_circle.length();

                    if (v0_circle_len > circle.radius) {
                        // not colliding with corner
                        return false;
                    }

                    // Collision Region C
                    contact_info.bodyA = a;
                    contact_info.bodyB = b;
                    contact_info.depth = circle.radius - v0_circle_len;
                    contact_info.normal.set(v0_circle).normalize();
                    contact_info.start.set(contact_info.normal);
                    contact_info.start.mul(-circle.radius);
                    contact_info.start.add(circle_position);
                    contact_info.end.set(contact_info.normal).mul(contact_info.depth);
                    contact_info.end.add(contact_info.start);
                    return true;


                } else {

                    // ###############################
                    // Inside region B (Center)
                    // ###############################

                    if (dist > circle.radius) {
                        // not colliding with edge
                        return false;
                    }

                    // Collision Region B
                    contact_info.bodyA = a;
                    contact_info.bodyB = b;
                    contact_info.depth = circle.radius - dist;
                    contact_info.normal.set(v0_v1).perpendicular().normalize();
                    contact_info.start.set(contact_info.normal);
                    contact_info.start.mul(-circle.radius);
                    contact_info.start.add(circle_position);
                    contact_info.end.set(contact_info.normal).mul(contact_info.depth);
                    contact_info.end.add(contact_info.start);
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
                System.out.println(b.velocity.x);
                Vector2f v0_v1 = tmp0.set(edge_v1).sub(edge_v0);
                contact_info.bodyA = a;
                contact_info.bodyB = b;
                contact_info.depth = circle.radius + dist;
                contact_info.normal.set(v0_v1).perpendicular().normalize();
                contact_info.start.set(contact_info.normal);
                contact_info.start.mul(-circle.radius);
                contact_info.start.add(circle_position);
                contact_info.end.set(contact_info.normal).mul(contact_info.depth);
                contact_info.end.add(contact_info.start);
                return true;
            }
        } finally {
            pushVec2(3);
        }


    }


    /**
     * Look up SAT (Separating Axis Theorem)
     */
    private static float findMinSeparation(Shape.Polygon a, Shape.Polygon b, Vector2f edge_normal, Vector2f point) {
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
        return Intersectionf.testCircleCircle(a,ra*ra,b,rb*rb);
    }

    private static boolean circleRect(Vector2f a, float ra, Rectanglef b) {
        return Intersectionf.testAarCircle(b.minX,b.minY,b.maxX,b.maxY,a.x,a.y,ra*ra);
    }

    private static boolean rectRect(Rectanglef a, Rectanglef b) {
        return a.intersectsRectangle(b);
    }

}
