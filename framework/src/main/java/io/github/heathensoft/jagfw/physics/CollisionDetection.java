package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.physics.shape.Circle;
import io.github.heathensoft.jagfw.physics.shape.PolygonShape;
import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.*;
import static io.github.heathensoft.jagfw.utils.U.pushVec2;

/**
 * NOTE: Circles can get stuck inside polygons
 * Frederik Dahl 1/23/2025
 */
public class CollisionDetection {

    private static final PhysicsGeometry[][] block_geometry_map;

    public static boolean bodyBody(PhysicsBody A, PhysicsBody B, BodyContact contact) {
        if (!(A.isStatic() && B.isStatic())) {
            boolean ap = A.shape instanceof PolygonShape;
            boolean bp = B.shape instanceof PolygonShape;
            boolean ac = A.shape instanceof Circle;
            boolean bc = B.shape instanceof Circle;
            if (ac && bc) return circleCircle(A,B,contact);
            if (ap && bp) return polyPoly(A,B,contact);
            if (ac && bp) return polyCircle(B,A,contact);
            if (ap && bc) return polyCircle(A,B,contact);
        } return false;
    }

    public static boolean bodyGeometry(PhysicsBody body, PhysicsGeometry geometry, GeometryContact contact) {
        if (body.isStatic()) return false;
        if (body.shape instanceof Circle) return circleGeometry(body,geometry,contact);
        if (body.shape instanceof PolygonShape) return polyGeometry(body,geometry,contact);
        return false;
    }

    // TODO: it's possible to collide with multiple blocks. save max depth, or sort by dist before. this should be private
    public static boolean bodyBlock(PhysicsBody body, int x, int y, int block_mask, GeometryContact contact) {
        if (body.isStatic() || block_mask < 0x0 || block_mask >= 0xF) return false;
        PhysicsGeometry[] block_geometry_list = block_geometry_map[block_mask];
        for (PhysicsGeometry geometry : block_geometry_list) {
            geometry.offset.set(x, y);
            if (body.shape instanceof Circle) {
                if (circleGeometry(body, geometry, contact)) {
                    return true;
                }
            } else if (body.shape instanceof PolygonShape) {
                if (polyGeometry(body, geometry, contact)) {
                    return true;
                }
            }
        } return false;
    }

    private static boolean polyGeometry(PhysicsBody body, PhysicsGeometry geometry, GeometryContact contact) {
        PolygonShape polygon = (PolygonShape) body.shape();
        int num_segments = geometry.numSegments();
        if (num_segments >= 1) {
            int s0;
            int s1;
            if (num_segments == 1) {
                s0 = 0;
                s1 = 0;
            } else {
                int closest = geometry.closestVertex(body.position);
                if (geometry.isPolygon()) {
                    s0 = closest - 1;
                    s1 = closest;
                } else {
                    if (closest == 0) {
                        s0 = 0;
                        s1 = 0;
                    } else if (closest == num_segments){
                        s0 = closest - 1;
                        s1 = closest - 1;
                    } else {
                        s0 = closest - 1;
                        s1 = closest;
                    }
                }
            }



            for (int s = s0; s <= s1; s++) {
                LineSegment segment = geometry.segment(s,s+1);
                if (segment.isValid()) {

                    if (geometry.one_way_collision) {
                        Vector2f segment_normal = segment.normal(U.popVec2());
                        U.pushVec2();
                        if (segment_normal.dot(body.velocity) > 0) {
                            continue;
                        }
                    }

                    {
                        // If the body has gone through the surface
                        // in one frame (high velocities)
                        // Adjust the body position to it's previous position
                        LineSegment position_delta = U.popLine();
                        position_delta.set(body.position_previous,body.position); // velocity dir
                        if (position_delta.lengthSquared() > 1e-5f) {
                            Vector2f intersection = U.popVec2();
                            if (position_delta.intersects(segment,intersection)) {
                                body.setPosition(body.position_previous.x,body.position_previous.y);
                            } U.pushVec2();
                        } U.pushLine();
                    }

                    float max_depth = Float.NEGATIVE_INFINITY;
                    Vector2f contact_point = U.popVec2();

                    Vector2f point = U.popVec2();
                    Vector2f pv = U.popVec2();
                    LineSegment body_to_vertex = U.popLine();
                    body_to_vertex.setP0(body.position);
                    Vector2f[] vertices = polygon.vertices();
                    for (Vector2f vertex : vertices) {
                        body_to_vertex.setP1(vertex);
                        if (body_to_vertex.intersects(segment, point)) {
                            pv.set(vertex).sub(point);
                            float depth = pv.length();
                            if (depth > max_depth) {
                                max_depth = depth;
                                contact_point.set(point);
                            } else if (depth == max_depth) {
                                // When we have to edges colliding completely in parallel
                                // We set the pont to the center of the edge
                                contact_point.add(point).div(2);
                                break;
                            }
                        }
                    }
                    U.pushLine();
                    U.pushVec2(3);
                    if (max_depth > Float.NEGATIVE_INFINITY) {
                        contact.body = body;
                        contact.depth = max_depth;
                        contact.geometry = geometry;
                        contact.point.set(contact_point);
                        {
                            // Need to check which side of surface we are
                            // or and adjust the contact normal accordingly
                            segment.normal(contact.normal);
                            Vector2f point_to_body = point.set(body.position).sub(contact.point);
                            float dot = contact.normal.dot(point_to_body);
                            if (dot >= 0) contact.normal.negate();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean polyGeometry2(PhysicsBody body, PhysicsGeometry geometry, GeometryContact contact) {
        PolygonShape polygon = (PolygonShape) body.shape();
        int num_segments = geometry.numSegments();
        // Find the two closest geometry line segments
        // (two lines connected to the closest vertex)
        // If the geometry is a polygon the first and last
        // vertex are connected. (enclosing the polygon)
        if (num_segments >= 1) {
            int s0;
            int s1;
            if (num_segments == 1) {
                s0 = 0;
                s1 = 0;
            } else {
                int closest = geometry.closestVertex(body.position);
                if (geometry.isPolygon()) {
                    s0 = closest - 1;
                    s1 = closest;
                } else {
                    if (closest == 0) {
                        s0 = 0;
                        s1 = 0;
                    } else if (closest == num_segments) {
                        s0 = closest - 1;
                        s1 = closest - 1;
                    } else {
                        s0 = closest - 1;
                        s1 = closest;
                    }
                }
            }

            // need to save the deepest penetration
            // and closest point of penetration
            // BEST SEGMENT OF THE GEOMETRY
            float CONTACT_DEPTH = Float.NEGATIVE_INFINITY;
            Vector2f CONTACT_POINT = U.popVec2();
            Vector2f CONTACT_NORMAL = U.popVec2();

            for (int s = s0; s <= s1; s++) {
                LineSegment segment = geometry.segment(s,s+1);
                if (segment.isValid()) {

                    Vector2f segment_normal = segment.normal(U.popVec2());
                    // if the geometry has one way collision
                    // we check the geometry normal direction
                    // and exit early if the body velocity
                    // vector face in the same direction.
                    if (geometry.one_way_collision) {
                        if (segment_normal.dot(body.velocity) > 0) {
                            U.pushVec2();
                            continue;
                        }
                    }

                    // If the body has gone through the surface
                    // in one frame (high velocities)
                    // Adjust the body position to its previous position
                    LineSegment position_delta = U.popLine();
                    position_delta.set(body.position_previous,body.position);
                    if (position_delta.isValid()) {
                        Vector2f intersection = U.popVec2();
                        if (position_delta.intersects(segment,intersection)) {
                            body.setPosition(body.position_previous.x,body.position_previous.y);
                        } U.pushVec2();
                    } U.pushLine();

                    // need to save the deepest penetration
                    // and closest point of penetration
                    // THE BEST VERTEX OF THE POLYGON
                    float contact_depth = Float.NEGATIVE_INFINITY;
                    Vector2f contact_point = U.popVec2();

                    {
                       Vector2f p_min = U.popVec2();
                       Vector2f p_max = U.popVec2();
                       Vector2f p = segment.p0(U.popVec2());
                       float dot_min = Float.POSITIVE_INFINITY;
                       float dot_max = Float.NEGATIVE_INFINITY;
                       int parallel_points_min = 1;
                       int parallel_points_max = 1;
                       Vector2f[] vertices = polygon.vertices();
                       for (Vector2f v : vertices) {
                           Vector2f pv = U.popSetVec2(v).sub(p);
                           float projection = segment_normal.dot(pv);
                           if (projection <= dot_min) {
                               if (projection == dot_min) {
                                   parallel_points_min++;
                                   p_min.add(v);
                               } else {
                                   parallel_points_min = 1;
                                   dot_min = projection;
                                   p_min.set(v);
                               }

                           }
                           if (projection >= dot_max) {
                               if (projection == dot_max) {
                                   parallel_points_max++;
                                   p_max.add(v);
                               } else {
                                   parallel_points_max = 1;
                                   dot_max = projection;
                                   p_max.set(v);
                               }
                           } U.pushVec2();
                       } p_min.div(parallel_points_min);
                       p_max.div(parallel_points_max);

                       if (dot_min <= 0 && dot_max >= 0) {
                           float abs_min = abs(dot_min);
                           if (abs_min <= dot_max) {
                               contact_point.set(p_min);
                               contact_depth = dot_min;
                           }
                       }

                       U.pushVec2(3);

                        /*
                            float min_sep = Float.POSITIVE_INFINITY;
                            int parallel_points = 1;
                            for (Vector2f vb : vertices_b) {
                                Vector2f ab = U.popSetVec2(vb).sub(vertex_a);
                                float projection = ab.dot(normal_a);
                                if (projection < min_sep) {
                                    min_sep = projection;
                                    dst.set(vb);
                                    parallel_points = 1;
                                } else if (projection == min_sep) {
                                    dst.add(vb);
                                    parallel_points++;
                                } U.pushVec2();
                            } dst.div(parallel_points);
                            return min_sep;
                         */

                    }


                    U.pushVec2(2);

                    // Keep track of the pest penetration
                    // for both geometry segments
                    if (contact_depth > CONTACT_DEPTH) {
                        CONTACT_DEPTH = contact_depth;
                        CONTACT_NORMAL.set(segment_normal);
                        CONTACT_POINT.set(contact_point);
                    }
                }
            }

            U.pushVec2(2);
            if (CONTACT_DEPTH > Float.NEGATIVE_INFINITY) {
                contact.body = body;
                contact.depth = CONTACT_DEPTH;
                contact.geometry = geometry;
                contact.point.set(CONTACT_POINT);
                contact.normal.set(CONTACT_NORMAL);
                // Need to check which side of surface we are
                // or and adjust the contact normal accordingly
                Vector2f point_to_body = U.popSetVec2(body.position).sub(contact.point);
                float dot = contact.normal.dot(point_to_body);
                if (dot >= 0) contact.normal.negate();
                U.pushVec2();
                return true;
            }
        }
        return false;
    }



    private static boolean circleGeometry(PhysicsBody body, PhysicsGeometry geometry, GeometryContact contact) {
        Circle circle = (Circle) body.shape();
        int num_segments = geometry.numSegments();
        if (num_segments >= 1) {
            int s0;
            int s1;
            if (num_segments == 1) {
                s0 = 0;
                s1 = 0;
            } else {
                int closest_vertex = geometry.closestVertex(body.position);
                if (geometry.isPolygon()) {
                    s0 = closest_vertex - 1;
                    s1 = closest_vertex;
                } else {
                    if (closest_vertex == 0) {
                        s0 = 0;
                        s1 = 0;
                    } else if (closest_vertex == num_segments){
                        s0 = closest_vertex - 1;
                        s1 = closest_vertex - 1;
                    } else {
                        s0 = closest_vertex - 1;
                        s1 = closest_vertex;
                    }
                }
            }
            // need to save the deepest penetration and closest point
            float max_depth = Float.NEGATIVE_INFINITY;
            Vector2f point_of_penetration = U.popVec2();

            for (int s = s0; s <= s1; s++) {
                LineSegment segment = geometry.segment(s,s+1);
                if (segment.isValid()) {

                    if (geometry.one_way_collision) {
                        Vector2f segment_normal = segment.normal(U.popVec2());
                        U.pushVec2();
                        if (segment_normal.dot(body.velocity) > 0) {
                            continue;
                        }
                    }
                    {
                        // If the body has gone through the surface
                        // in one frame (high velocities)
                        // Adjust the body position to previous position
                        LineSegment position_delta = U.popLine();
                        position_delta.set(body.position_previous,body.position); // velocity dir
                        if (position_delta.lengthSquared() > 1e-5f) {
                            Vector2f intersection = U.popVec2();
                            if (position_delta.intersects(segment,intersection)) {
                                body.setPosition(body.position_previous.x,body.position_previous.y);
                            } U.pushVec2();
                        } U.pushLine();
                    }

                    {
                        Vector2f point_segment = U.closestPointOnSegment(
                                body.position.x,body.position.y,
                                segment.x0, segment.y0,
                                segment.x1, segment.y1,
                                U.popVec2()
                        );
                        float rad2 = U.square(circle.radius());
                        float len2 = lengthSquared(point_segment,body.position);
                        if (rad2 > len2) {
                            float depth;
                            {
                                Vector2f center_to_point = U.popVec2();
                                center_to_point.set(point_segment).sub(body.position);
                                depth = circle.radius() - center_to_point.length();
                                U.pushVec2();
                            }
                            if (depth > max_depth) {
                                max_depth = depth;
                                point_of_penetration.set(point_segment);
                            }
                        }
                        U.pushVec2();
                    }
                }
            }
            U.pushVec2();
            if (max_depth > Float.NEGATIVE_INFINITY) {
                contact.body = body;
                contact.geometry = geometry;
                contact.depth = max_depth;
                contact.point.set(point_of_penetration);
                contact.normal.set(contact.point).sub(body.position).normalize();
                contact.normal.normalize();
                return true;
            }
        }
        return false;
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
            float ab_separation = satFindMinSeparation(a_poly,b_poly,a_edge_normal,a_point);
            if (ab_separation >= 0) {
                pushVec2(4);
                return false;
            }
            float ba_separation = satFindMinSeparation(b_poly,a_poly,b_edge_normal,b_point);
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



    private static float satDeepestPen(final Vector2f vertex_a, final Vector2f normal_a, final Vector2f[] vertices_b, Vector2f dst) {
        float min_sep = Float.POSITIVE_INFINITY;
        int parallel_points = 1;
        for (Vector2f vb : vertices_b) {
            Vector2f ab = U.popSetVec2(vb).sub(vertex_a);
            float projection = ab.dot(normal_a);
            if (projection < min_sep) {
                min_sep = projection;
                dst.set(vb);
                parallel_points = 1;
            } else if (projection == min_sep) {
                dst.add(vb);
                parallel_points++;
            } U.pushVec2();
        } dst.div(parallel_points);
        return min_sep;
    }

    /**
     * Look up SAT (Separating Axis Theorem)
     */
    private static float satFindMinSeparation(final PolygonShape a, final PolygonShape b, Vector2f normal, Vector2f point) {
        float separation = Float.NEGATIVE_INFINITY;
        final Vector2f[] a_vertices = a.vertices();
        final Vector2f[] b_vertices = b.vertices();
        for (int i = 0; i < a_vertices.length; i++) {
            Vector2f va = a_vertices[i];
            Vector2f min_vertex = U.popVec2();
            Vector2f a_edge_normal = a.edgeNormal(i,popVec2());
            float penetration = satDeepestPen(
            va,a_edge_normal,b_vertices,min_vertex);
            if (penetration > separation) {
                separation = penetration;
                normal.set(a_edge_normal);
                point.set(min_vertex);
            } U.pushVec2(2);
        } return separation;
    }


    private static boolean circleCircle(float ax, float ay, float ar, float bx, float by, float br) {
        final float dx = bx - ax;
        final float dy = by - ay;
        final float r = ar + br;
        return  (dx * dx + dy * dy) <= (r * r);
    }

    private static boolean circleCircle(Vector2f a, float ar, Vector2f b, float br) {
        final float dx = b.x - a.x;
        final float dy = b.y - a.y;
        final float r = ar + br;
        return  (dx * dx + dy * dy) <= (r * r);
    }

    static {
        block_geometry_map = new PhysicsGeometry[16][];
        // ###################################################
        // # # #
        // #   #
        // # # #
        block_geometry_map[0] = new PhysicsGeometry[1];
        block_geometry_map[0][0] = new PhysicsGeometry(4);
        block_geometry_map[0][0].vertices[0].set(0,0);
        block_geometry_map[0][0].vertices[1].set(1,0);
        block_geometry_map[0][0].vertices[2].set(1,1);
        block_geometry_map[0][0].vertices[3].set(0,1);
        block_geometry_map[0][0].polygon = true;
        block_geometry_map[0][0].one_way_collision = true;
        // ###################################################
        // #   #
        // #   #
        // # # #
        block_geometry_map[1] = new PhysicsGeometry[1];
        block_geometry_map[1][0] = new PhysicsGeometry(4);
        block_geometry_map[1][0].vertices[0].set(0,1);
        block_geometry_map[1][0].vertices[1].set(0,0);
        block_geometry_map[1][0].vertices[2].set(1,0);
        block_geometry_map[1][0].vertices[3].set(1,1);
        block_geometry_map[1][0].one_way_collision = true;
        // ###################################################
        // # # #
        //     #
        // # # #
        block_geometry_map[2] = new PhysicsGeometry[1];
        block_geometry_map[2][0] = new PhysicsGeometry(4);
        block_geometry_map[2][0].vertices[0].set(0,0);
        block_geometry_map[2][0].vertices[1].set(1,0);
        block_geometry_map[2][0].vertices[2].set(1,1);
        block_geometry_map[2][0].vertices[3].set(0,1);
        block_geometry_map[2][0].one_way_collision = true;
        // ###################################################
        //     #
        //     #
        // # # #
        block_geometry_map[3] = new PhysicsGeometry[1];
        block_geometry_map[3][0] = new PhysicsGeometry(3);
        block_geometry_map[3][0].vertices[0].set(0,0);
        block_geometry_map[3][0].vertices[1].set(1,0);
        block_geometry_map[3][0].vertices[2].set(1,1);
        block_geometry_map[3][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #
        // # # #
        block_geometry_map[4] = new PhysicsGeometry[1];
        block_geometry_map[4][0] = new PhysicsGeometry(4);
        block_geometry_map[4][0].vertices[0].set(1,1);
        block_geometry_map[4][0].vertices[1].set(0,1);
        block_geometry_map[4][0].vertices[2].set(0,0);
        block_geometry_map[4][0].vertices[3].set(1,0);
        block_geometry_map[4][0].one_way_collision = true;
        // ###################################################
        // #
        // #
        // # # #
        block_geometry_map[5] = new PhysicsGeometry[1];
        block_geometry_map[5][0] = new PhysicsGeometry(3);
        block_geometry_map[5][0].vertices[0].set(0,1);
        block_geometry_map[5][0].vertices[1].set(0,0);
        block_geometry_map[5][0].vertices[2].set(1,0);
        block_geometry_map[5][0].one_way_collision = true;
        // ###################################################
        // # # #
        //
        // # # #
        block_geometry_map[6] = new PhysicsGeometry[2]; // 2
        block_geometry_map[6][0] = new PhysicsGeometry(2);
        block_geometry_map[6][0].vertices[0].set(0,0);
        block_geometry_map[6][0].vertices[1].set(1,0);
        block_geometry_map[6][0].one_way_collision = true;
        block_geometry_map[6][1] = new PhysicsGeometry(2);
        block_geometry_map[6][1].vertices[0].set(1,1);
        block_geometry_map[6][1].vertices[1].set(0,1);
        block_geometry_map[6][1].one_way_collision = true;
        // ###################################################
        //
        //
        // # # #
        block_geometry_map[7] = new PhysicsGeometry[1];
        block_geometry_map[7][0] = new PhysicsGeometry(2);
        block_geometry_map[7][0].vertices[0].set(0,0);
        block_geometry_map[7][0].vertices[1].set(1,0);
        block_geometry_map[7][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #   #
        // #   #
        block_geometry_map[8] = new PhysicsGeometry[1];
        block_geometry_map[8][0] = new PhysicsGeometry(4);
        block_geometry_map[8][0].vertices[0].set(1,0);
        block_geometry_map[8][0].vertices[1].set(1,1);
        block_geometry_map[8][0].vertices[2].set(0,1);
        block_geometry_map[8][0].vertices[3].set(0,0);
        block_geometry_map[8][0].one_way_collision = true;
        // ###################################################
        // #   #
        // #   #
        // #   #
        block_geometry_map[9] = new PhysicsGeometry[2]; // 2
        block_geometry_map[9][0] = new PhysicsGeometry(2);
        block_geometry_map[9][0].vertices[0].set(0,1);
        block_geometry_map[9][0].vertices[1].set(0,0);
        block_geometry_map[9][0].one_way_collision = true;
        block_geometry_map[9][1] = new PhysicsGeometry(2);
        block_geometry_map[9][1].vertices[0].set(1,0);
        block_geometry_map[9][1].vertices[1].set(1,1);
        block_geometry_map[9][1].one_way_collision = true;
        // ###################################################
        // # # #
        //     #
        //     #
        block_geometry_map[10] = new PhysicsGeometry[1];
        block_geometry_map[10][0] = new PhysicsGeometry(3);
        block_geometry_map[10][0].vertices[0].set(1,0);
        block_geometry_map[10][0].vertices[1].set(1,1);
        block_geometry_map[10][0].vertices[2].set(0,1);
        block_geometry_map[10][0].one_way_collision = true;
        // ###################################################
        //     #
        //     #
        //     #
        block_geometry_map[11] = new PhysicsGeometry[1];
        block_geometry_map[11][0] = new PhysicsGeometry(2);
        block_geometry_map[11][0].vertices[0].set(1,0);
        block_geometry_map[11][0].vertices[1].set(1,1);
        block_geometry_map[11][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #
        // #
        block_geometry_map[12] = new PhysicsGeometry[1];
        block_geometry_map[12][0] = new PhysicsGeometry(3);
        block_geometry_map[12][0].vertices[0].set(1,1);
        block_geometry_map[12][0].vertices[1].set(0,1);
        block_geometry_map[12][0].vertices[2].set(0,0);
        block_geometry_map[12][0].one_way_collision = true;
        // ###################################################
        // #
        // #
        // #
        block_geometry_map[13] = new PhysicsGeometry[1];
        block_geometry_map[13][0] = new PhysicsGeometry(2);
        block_geometry_map[13][0].vertices[0].set(0,1);
        block_geometry_map[13][0].vertices[1].set(0,0);
        block_geometry_map[13][0].one_way_collision = true;
        // ###################################################
        // # # #
        //
        //
        block_geometry_map[14] = new PhysicsGeometry[1];
        block_geometry_map[14][0] = new PhysicsGeometry(2);
        block_geometry_map[14][0].vertices[0].set(1,1);
        block_geometry_map[14][0].vertices[1].set(0,1);
        block_geometry_map[14][0].one_way_collision = true;
        // ###################################################
        //
        //
        //
        block_geometry_map[15] = new PhysicsGeometry[1];
        block_geometry_map[15][0] = new PhysicsGeometry(0);
        block_geometry_map[15][0].one_way_collision = true;
        // ###################################################
    }


}
