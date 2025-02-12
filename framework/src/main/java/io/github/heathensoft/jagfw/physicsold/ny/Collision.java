package io.github.heathensoft.jagfw.physicsold.ny;

import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.*;

/**
 * Frederik Dahl 1/31/2025
 */
public class Collision {


    public static boolean bodyBody(final Body A, final Body B, BodyContact contact) {
        if (!(A.isStatic() && B.isStatic())) {
            boolean ap = A.shape instanceof Polygon;
            boolean bp = B.shape instanceof Polygon;
            boolean ac = A.shape instanceof Circle;
            boolean bc = B.shape instanceof Circle;
            if (ac && bc) return bodyCircleCircle(A,B,contact);
            if (ap && bp) return bodyPolyPoly(A,B,contact);
            if (ac && bp) return bodyPolyCircle(B,A,contact);
            if (ap && bc) return bodyPolyCircle(A,B,contact);
        } return false;
    }

    public static boolean bodyGeom(final Body body, final Geometry geom, GeomContact contact) {
        if (body.isStatic()) return false;
        if (body.shape instanceof Circle) return bodyCircleGeom(body,geom,contact);
        if (body.shape instanceof Polygon) return bodyPolyGeom(body,geom,contact);
        return false;
    }

    public static boolean bodyBlock(final Body body, int x, int y, int block_type, GeomContact contact) {
        if (body.isStatic() || block_type < 0x0 || block_type >= 0xF) return false;
        Geometry[] block_geom = Geometry.blockGeometry(block_type);
        if (block_geom.length == 0) return false;
        if (block_geom.length == 1) {
            Geometry geom = block_geom[0];
            geom.offset.set(x,y);
            if (body.shape instanceof Circle) return bodyCircleGeom(body,geom,contact);
            if (body.shape instanceof Polygon) return bodyPolyGeom(body,geom,contact);
        } else {
            float MAX_DEPTH = Float.NEGATIVE_INFINITY;
            for (Geometry geom : block_geom) {
                geom.offset.set(x,y);
                if (body.shape instanceof Circle) {
                    if (bodyCircleGeom(body,geom,geom_contact_internal)) {
                        float depth = geom_contact_internal.depth;
                        if (depth > MAX_DEPTH) {
                            MAX_DEPTH = depth;
                            contact.normal.set(geom_contact_internal.normal);
                            contact.point.set(geom_contact_internal.point);
                            contact.geometry = geom;
                            contact.body = body;
                            contact.depth = depth;
                        }
                    }
                }
                else if (body.shape instanceof Polygon) {
                    if (bodyPolyGeom(body,geom,geom_contact_internal)) {
                        float depth = geom_contact_internal.depth;
                        if (depth > MAX_DEPTH) {
                            MAX_DEPTH = depth;
                            contact.normal.set(geom_contact_internal.normal);
                            contact.point.set(geom_contact_internal.point);
                            contact.geometry = geom;
                            contact.body = body;
                            contact.depth = depth;
                        }
                    }
                }
            } return MAX_DEPTH > Float.NEGATIVE_INFINITY;
        } return false;
    }

    public static boolean rayShape(final LineSegment ray, final Shape shape, RayContact contact) {
        if (shape instanceof Polygon polygon) return rayPolygon(ray,polygon,contact);
        if (shape instanceof Circle circle) return rayCircle(ray,circle,contact);
        return false;
    }

    public static boolean rayGeom(final LineSegment ray, final Geometry geom, RayContact contact) {
        if (ray.isValid()) {
            Vector2f ray_dir = ray.direction();
            Vector2f[] vertices = geom.vertices;
            LineSegment geom_edge = new LineSegment();
            int num_segments = geom.numSegments();
            float min_dist2 = Float.POSITIVE_INFINITY;
            for (int i = 0; i < num_segments; i++) {
                Vector2f p0 = vertices[i];
                Vector2f p1 = vertices[(i + 1) % vertices.length];
                contact.normal.set(p1).sub(p0);
                contact.normal.perpendicular();
                float dot = contact.normal.dot(ray_dir);
                if (dot < 0) {
                    geom_edge.set(p0,p1);
                    if (geom_edge.intersects(ray,contact.point)) {
                        Vector2f origin_to_point = new Vector2f();
                        origin_to_point.set(contact.point).sub(ray.x0,ray.y0);
                        float len2 = origin_to_point.lengthSquared();
                        if (len2 < min_dist2) {
                            contact.ray = ray;
                            contact.normal.normalize();
                            min_dist2 = len2;
                        }
                    }
                } else if (!geom.one_way_collision) {
                    geom_edge.set(p0,p1);
                    if (geom_edge.intersects(ray,contact.point)) {
                        Vector2f origin_to_point = new Vector2f();
                        origin_to_point.set(contact.point).sub(ray.x0,ray.y0);
                        float len2 = origin_to_point.lengthSquared();
                        if (len2 < min_dist2) {
                            contact.ray = ray;
                            contact.normal.negate();
                            contact.normal.normalize();
                            min_dist2 = len2;
                        }
                    }
                }
            } return min_dist2 < Float.POSITIVE_INFINITY;
        } return false;
    }







    private static boolean bodyCircleCircle(final Body A, final Body B, BodyContact contact) {
        Circle circle_a = (Circle) A.shape;
        Circle circle_b = (Circle) B.shape;
        if (testCircleCircle(circle_a,circle_b)) {
            Vector2f aPos = circle_a.position;
            Vector2f bPos = circle_b.position;
            Vector2f ab = new Vector2f(bPos).sub(aPos);
            Vector2f start_end = new Vector2f();
            contact.A = A;
            contact.B = B;
            contact.normal.set(ab).normalize();
            contact.start.set(contact.normal).negate();
            contact.start.mul(circle_b.radius).add(bPos);
            contact.end.set(contact.normal);
            contact.end.mul(circle_a.radius).add(aPos);
            start_end.set(contact.end).sub(contact.start);
            contact.depth = start_end.length();
            return true;
        } return false;
    }

    private static boolean bodyPolyCircle(final Body A, final Body B, BodyContact contact) {
        Polygon polygon = (Polygon) A.shape;
        Circle circle = (Circle) B.shape;
        Vector2f circle_position = B.position;
        Vector2f polygon_position = A.position;
        if (testCircleCircle(
                A.position.x,A.position.y,circle.boundingRadius(),
                B.position.x,B.position.y,polygon.boundingRadius())) {
            /*
             * Todo: Need to test with more complex polygons
             * From here, i think the code might only apply for rectangles and triangles
             * Check the todo below
             */
            Vector2f edge_v0 = null;
            Vector2f edge_v1 = null;
            Vector2f tmp0 = new Vector2f();
            Vector2f tmp1 = new Vector2f();
            Vector2f tmp2 = new Vector2f();
            Vector2f[] vertices = polygon.vertices();
            float dist = Float.POSITIVE_INFINITY; // Distance from circle to edge

            // ###############################
            // Find and store the closest edge
            // And the distance to the circle
            // ###############################

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
            // They shouldn't be atp.
            // (polygons have at least 3 edges)
            if (edge_v0 == null) return false;
            if (edge_v1 == null) return false;

            if (dist > 0) { // outside  (negative distance if circle center is inside polygon)
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
                    contact.A = A;
                    contact.B = B;
                    contact.depth = circle.radius - v1_circle_len;
                    contact.normal.set(v1_circle).normalize();
                    contact.start.set(contact.normal);
                    contact.start.mul(-circle.radius);
                    contact.start.add(circle_position);
                    contact.end.set(contact.normal).mul(contact.depth);
                    contact.end.add(contact.start);
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
                    contact.A = A;
                    contact.B = B;
                    contact.depth = circle.radius - v0_circle_len;
                    contact.normal.set(v0_circle).normalize();
                    contact.start.set(contact.normal);
                    contact.start.mul(-circle.radius);
                    contact.start.add(circle_position);
                    contact.end.set(contact.normal).mul(contact.depth);
                    contact.end.add(contact.start);

                } else {
                    // ###############################
                    // Inside region B (Center)
                    // ###############################

                    if (dist > circle.radius) {
                        // not colliding with edge
                        return false;
                    }
                    // Collision Region B
                    contact.A = A;
                    contact.B = B;
                    contact.depth = circle.radius - dist;
                    contact.normal.set(v0_v1).perpendicular().normalize();
                    contact.start.set(contact.normal);
                    contact.start.mul(-circle.radius);
                    contact.start.add(circle_position);
                    contact.end.set(contact.normal).mul(contact.depth);
                    contact.end.add(contact.start);
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
                contact.depth = circle.radius + dist;
                contact.normal.set(v0_v1).perpendicular().normalize();
                contact.start.set(contact.normal);
                contact.start.mul(-circle.radius);
                contact.start.add(circle_position);
                contact.end.set(contact.normal).mul(contact.depth);
                contact.end.add(contact.start);
            } return true;
        } return false;

    }

    private static boolean bodyPolyPoly(final Body A, final Body B, BodyContact contact) {
        Polygon a_poly = (Polygon) A.shape;
        Polygon b_poly = (Polygon) B.shape;
        if (testCircleCircle(
                A.position.x,A.position.y,a_poly.boundingRadius(),
                B.position.x,B.position.y,b_poly.boundingRadius())) {
            Vector2f a_point = new Vector2f();
            Vector2f a_edge_normal = new Vector2f();
            float ab_separation = satFindMinSeparation(a_poly,b_poly,a_edge_normal,a_point);
            if (ab_separation >= 0) return false;
            Vector2f b_point = new Vector2f();
            Vector2f b_edge_normal = new Vector2f();
            float ba_separation = satFindMinSeparation(b_poly,a_poly,b_edge_normal,b_point);
            if (ba_separation >= 0) return false;
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
            }
            contact.A = A;
            contact.B = B;
            return true;
        } return false;
    }

    private static boolean bodyCircleGeom(final Body body, final Geometry geom, GeomContact contact) {
        Circle circle = (Circle) body.shape;
        int num_segments = geom.numSegments();
        if (num_segments >= 1) {
            int s0;
            int s1;
            if (num_segments == 1) {
                s0 = 0;
                s1 = 0;
            } else {
                int closest_vertex = geom.closestVertex(body.position);
                if (geom.isPolygon()) {
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
            } // need to save the deepest penetration and closest point
            float max_depth = Float.NEGATIVE_INFINITY;
            Vector2f point_of_penetration = new Vector2f();
            for (int s = s0; s <= s1; s++) {
                LineSegment segment = geom.segment(s,s+1);
                if (segment.isValid()) {
                    // check one way collision
                    if (geom.one_way_collision) {
                        if (segment.normal().dot(body.velocity) > 0) {
                            continue;
                        }
                    }
                    // If the body has gone through the surface
                    // in one frame (high velocities)
                    // Adjust the body position to previous position
                    LineSegment position_delta = new LineSegment();
                    position_delta.set(body.position_previous,body.position); // velocity dir
                    if (position_delta.lengthSquared() > 1e-5f) {
                        Vector2f intersection = new Vector2f();
                        if (position_delta.intersects(segment,intersection)) {
                            body.position.set(body.position_previous);
                        }
                    }
                    Vector2f point_segment = segment.closestPoint(body.position,new Vector2f());
                    float rad2 = U.square(circle.radius);
                    float len2 = lengthSquared(point_segment,body.position);
                    if (rad2 > len2) {
                        Vector2f center_to_point = new Vector2f();
                        center_to_point.set(point_segment).sub(body.position);
                        float depth = circle.radius - center_to_point.length();
                        if (depth > max_depth) {
                            max_depth = depth;
                            point_of_penetration.set(point_segment);
                        }
                    }
                }
            }
            if (max_depth > Float.NEGATIVE_INFINITY) {
                contact.body = body;
                contact.geometry = geom;
                contact.depth = max_depth;
                contact.point.set(point_of_penetration);
                contact.normal.set(contact.point).sub(body.position).normalize();
                contact.normal.normalize();
                return true;
            }
        } return false;
    }

    private static boolean bodyPolyGeom(final Body body, final Geometry geom, GeomContact contact) {
        // Todo: Buggy
        Polygon polygon = (Polygon) body.shape;
        int num_segments = geom.numSegments();
        if (num_segments >= 1) {
            int s0;
            int s1;
            if (num_segments == 1) {
                s0 = 0;
                s1 = 0;
            } else {
                int closest_vertex = geom.closestVertex(body.position);
                if (geom.isPolygon()) {
                    s0 = closest_vertex - 1;
                    s1 = closest_vertex;
                } else {
                    if (closest_vertex == 0) {
                        s0 = 0;
                        s1 = 0;
                    } else if (closest_vertex == num_segments) {
                        s0 = closest_vertex - 1;
                        s1 = closest_vertex - 1;
                    } else {
                        s0 = closest_vertex - 1;
                        s1 = closest_vertex;
                    }
                }
            } // need to save the deepest penetration and closest point
            // need to save the deepest penetration and closest point
            float DEPTH = Float.NEGATIVE_INFINITY;
            Vector2f POINT = new Vector2f();
            Vector2f NORMAL = new Vector2f();
            for (int s = s0; s <= s1; s++) {
                LineSegment segment = geom.segment(s, s + 1);
                Vector2f segment_normal = segment.normal();
                if (segment.isValid()) {
                    // check one way collision
                    if (geom.one_way_collision) {
                        if (segment_normal.dot(body.velocity) > 0) {
                            continue;
                        }
                    }
                }
                // If the body has gone through the surface
                // in one frame (high velocities)
                // Adjust the body position to previous position
                LineSegment position_delta = new LineSegment();
                position_delta.set(body.position_previous,body.position); // velocity dir
                if (position_delta.lengthSquared() > 1e-5f) {
                    Vector2f intersection = new Vector2f();
                    if (position_delta.intersects(segment,intersection)) {
                        body.position.set(body.position_previous);
                    }
                }
                float min_projection = Float.POSITIVE_INFINITY;
                float max_projection = Float.NEGATIVE_INFINITY;
                int index_min = -1;
                int index_max = -1;
                Vector2f[] vertices = polygon.vertices();
                for (int i = 0; i < vertices.length; i++) {
                    Vector2f v = vertices[i];
                    Vector2f pv = new Vector2f(v).sub(segment.x0,segment.y0);
                    float projection = segment_normal.dot(pv);
                    if (projection < min_projection) {
                        min_projection = projection;
                        index_min = i;
                    }
                    if (projection > max_projection) {
                        max_projection = projection;
                        index_max = i;
                    }
                }
                if (max_projection + min_projection < max_projection) {
                    // potential overlap
                    Vector2f intersection = new Vector2f();
                    Vector2f seg_vertex = new Vector2f();
                    int num_intersections = 0;
                    for (int i = 0; i < vertices.length; i++) {
                        LineSegment edge = polygon.edgeSegment(i);
                        if (num_intersections == 0) {
                            if (edge.intersects(segment,intersection)) {
                                float dist_p0 = segment.p0(seg_vertex).sub(body.position).lengthSquared();
                                float dist_p1 = segment.p1(seg_vertex).sub(body.position).lengthSquared();
                                if (dist_p0 < dist_p1) {
                                    segment.p0(seg_vertex);
                                } else segment.p1(seg_vertex);
                                num_intersections++;
                            }
                        } else {
                            if (edge.intersects(segment,intersection)) {
                                num_intersections++;
                                break;
                            }
                        }
                    }

                    if (num_intersections > 0) {
                        int index;
                        float depth;
                        float abs_depth;
                        if (geom.one_way_collision) {
                            index = index_min;
                            depth = min_projection; // negative depth
                            abs_depth = abs(min_projection);
                        } else {
                            abs_depth = abs(min_projection);
                            if (max_projection > abs_depth) {
                                index = index_min;
                                depth = min_projection; // negative depth
                            } else {
                                index = index_max;
                                depth = max_projection;
                                abs_depth = max_projection;
                            }
                        }
                        if (abs_depth > DEPTH) {
                            if (num_intersections == 1) {
                                POINT.set(seg_vertex);
                                NORMAL.set(intersection).sub(seg_vertex);
                                DEPTH = NORMAL.length();
                                NORMAL.normalize();
                            } else  { // 2
                                POINT.set(vertices[index]);
                                DEPTH = abs_depth;
                                NORMAL.set(segment_normal);
                                if (depth <= 0) {
                                    NORMAL.negate();
                                }
                            }
                        }
                    }


                }

            }
            if (DEPTH > Float.NEGATIVE_INFINITY) {
                contact.depth = DEPTH;
                contact.point.set(POINT);
                contact.normal.set(NORMAL);
                contact.body = body;
                contact.geometry = geom;
                return true;
            }
        }
        return false;
    }




    private static boolean rayCircle(final LineSegment ray, final Circle circle, RayContact contact) {
        if (ray.isValid() && circle.radius != 0)
            if (intersectionRayCircle(ray,circle,contact.point)) {
                contact.normal.set(contact.point);
                contact.normal.sub(circle.position).normalize();
                contact.ray = ray;
                return true;
            } return false;
    }

    private static boolean rayPolygon(final LineSegment ray, final Polygon polygon, RayContact contact) {
        if (ray.isValid()) {
            Vector2f ray_dir = ray.direction();
            Vector2f[] vertices = polygon.vertices();
            LineSegment poly_edge = new LineSegment();
            for (int i = 0; i < vertices.length; i++) {
                Vector2f p0 = vertices[i];
                Vector2f p1 = vertices[(i + 1) % vertices.length];
                contact.normal.set(p1).sub(p0);
                contact.normal.perpendicular();
                float dot = contact.normal.dot(ray_dir);
                if (dot < 0) { poly_edge.set(p0,p1);
                    if (poly_edge.intersects(ray,contact.point)) {
                        contact.normal.normalize();
                        contact.ray = ray;
                        return true;
                    }
                }
            }
        } return false;
    }

    public static boolean intersectionRayCircle(final LineSegment ray, final Circle circle, Vector2f dst) {
        // If origin is inside the circle we ignore it.
        if (!testPointCircle(circle, ray.x0, ray.y0)) {
            Vector2f p = ray.closestPoint(circle.position,dst);
            final float r2 = U.square(circle.radius);
            final float a = p.x - circle.position.x;
            final float b = p.y - circle.position.y;
            final float l2 = a * a + b * b;
            if (l2 < r2) {
                final float c = ray.x0 - p.x;
                final float d = ray.y0 - p.y;
                final float h = Math.sqrt(r2 - l2);
                final float invLen = Math.invsqrt(c * c + d * d);
                dst.add(c * invLen * h,d * invLen * h);
                return true;
            } return l2 == r2;
        } return false;
    }

    public static boolean testPointCircle(final Circle circle, float px, float py) {
        final float a = circle.position.x - px;
        final float b = circle.position.y - py;
        return ((a * a + b * b) <= (circle.radius * circle.radius));
    }

    public static boolean testRayCircle(final LineSegment ray, final Circle circle) {
        // Will also return true if line is inside the circle
        Vector2f p = ray.closestPoint(circle.position,U.popVec2());
        U.pushVec2();
        final float a = p.x - circle.position.x;
        final float b = p.y - circle.position.y;
        return (a * a + b * b) <= U.square(circle.radius);
    }

    public static boolean testCircleCircle(final Circle A, final Circle B) {
        final float dx = B.position.x - A.position.x;
        final float dy = B.position.y - A.position.y;
        final float r = A.radius + B.radius;
        return  (dx * dx + dy * dy) <= (r * r);
    }

    private static boolean testCircleCircle(float ax, float ay, float ar, float bx, float by, float br) {
        final float dx = bx - ax;
        final float dy = by - ay;
        final float r = ar + br;
        return  (dx * dx + dy * dy) <= (r * r);
    }

    /** Look up SAT (Separating Axis Theorem) */
    private static float satFindMinSeparation(final Polygon a, final Polygon b, Vector2f normal, Vector2f point) {
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

    private static final GeomContact geom_contact_internal = new GeomContact();

}
