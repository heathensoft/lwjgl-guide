package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.lengthSquared;

/**
 * Frederik Dahl 2/10/2025
 */
public class Collision {

    private static final RayContact ray_c_0 = new RayContact();
    private static final GeomContact geo_c_0 = new GeomContact();

    public static boolean bodyBody(final Body A, final Body B, BodyContact contact) {
        if (A.isStatic() && B.isStatic()) return false;
        final float dx = B.position.x - A.position.x;
        final float dy = B.position.y - A.position.y;
        final float r = A.radius + B.radius;
        final float d_squared = (dx * dx + dy * dy);
        if (d_squared <= r * r) {
            contact.A = A;
            contact.B = B;
            contact.normal.set(dx,dy).normalize();
            contact.depth = r - Math.sqrt(d_squared);
            return true;
        } return false;
    }

    public static boolean bodyGeometry(final Body body, final Geometry geom, GeomContact contact) {
        if (body.isStatic()) return false;
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
                    float rad2 = U.square(body.radius);
                    float len2 = lengthSquared(point_segment,body.position);
                    if (rad2 > len2) {
                        Vector2f center_to_point = new Vector2f();
                        center_to_point.set(point_segment).sub(body.position);
                        float depth = body.radius - center_to_point.length();
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
                contact.normal.set(point_of_penetration);
                contact.normal.sub(body.position).normalize();
                return true;
            }
        } return false;
    }

    public static boolean bodyBlock(final Body body, int x, int y, int block_type, GeomContact contact) {
        if (body.isStatic() || block_type < 0x0 || block_type >= 0xF) return false;
        Geometry[] block_geom = Geometry.blockGeometry(block_type);
        if (block_geom.length == 0) return false;
        if (block_geom.length == 1) {
            Geometry geom = block_geom[0];
            geom.translation.set(x,y);
            return bodyGeometry(body,geom,contact);
        } else { float MAX_DEPTH = Float.NEGATIVE_INFINITY;
            for (Geometry geom : block_geom) {
                geom.translation.set(x,y);
                if (bodyGeometry(body,geom, geo_c_0)) {
                    float depth = geo_c_0.depth;
                    if (depth > MAX_DEPTH) {
                        MAX_DEPTH = depth;
                        contact.normal.set(geo_c_0.normal);
                        contact.geometry = geom;
                        contact.body = body;
                        contact.depth = depth;
                    }
                }
            } return MAX_DEPTH > Float.NEGATIVE_INFINITY;
        }
    }

    public static boolean rayBody(final LineSegment ray, final Body body, RayContact contact) {
        if (!ray.isValid()) return false;
        return rayCircle(ray,body.position.x,body.position.y,body.radius,contact);
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

    public static boolean rayBlock(final LineSegment ray, int x, int y, int block_type, RayContact contact) {
        if (block_type < 0x0 || block_type >= 0xF) return false;
        Geometry[] block_geom = Geometry.blockGeometry(block_type);
        if (block_geom.length == 0) return false;
        if (block_geom.length == 1) {
            Geometry geom = block_geom[0];
            geom.translation.set(x,y);
            return rayGeom(ray,geom,contact);
        } else { float MIN_LEN = Float.POSITIVE_INFINITY;
            for (Geometry geom : block_geom) {
                geom.translation.set(x,y);
                if (rayGeom(ray,geom, ray_c_0)) {
                    float len2 = ray_c_0.lengthSquared();
                    if (len2 < MIN_LEN) {
                        contact.normal.set(ray_c_0.normal);
                        contact.ray.set(ray_c_0.ray);
                        contact.point.set(ray_c_0.point);
                        MIN_LEN = len2;
                    }
                }
            } return MIN_LEN < Float.POSITIVE_INFINITY;
        }
    }

    public static boolean rayCircle(final LineSegment ray, float cx, float cy, float cr, RayContact contact) {
        if (cr != 0) {
            if (intersectionRayCircle(ray,cx,cy,cr,contact.point)) {
                contact.normal.set(contact.point);
                contact.normal.sub(cx,cy).normalize();
                contact.ray = ray;
                return true; }
        } return false;
    }

    public static boolean intersectionRayCircle(final LineSegment ray, final Vector2f c, float cr, Vector2f dst) {
        return intersectionRayCircle(ray,c.x,c.y,cr,dst);
    }

    public static boolean intersectionRayCircle(final LineSegment ray, float cx, float cy, float cr, Vector2f dst) {
        // If ray origin is inside the circle we ignore it.
        // All ray circle methods are meant for rays with origin outside the circle
        // Todo: This might not be ideal
        // The reason is that bodies might be "shooting a ray" from it's center (inside itself)
        // And we don't want to intersect with the source of the ray.
        if (!testPointCircle(ray.x0, ray.y0,cx,cy,cr)) {
            Vector2f p = ray.closestPoint(cx,cy,dst);
            final float r2 = U.square(cr);
            final float a = p.x - cx;
            final float b = p.y - cy;
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

    public static boolean testRayCircle(final LineSegment ray, final Vector2f c, float cr) {
        return testRayCircle(ray,c.x,c.y,cr);
    }

    public static boolean testRayCircle(final LineSegment ray, float cx, float cy, float cr) {
        // Will also return true if ray is inside the circle (contained)
        // All ray circle methods are meant for rays with origin outside the circle
        // Todo: This might not be ideal
        Vector2f p = ray.closestPoint(cx,cy,U.popVec2());
        U.pushVec2();
        final float a = p.x - cx;
        final float b = p.y - cy;
        return (a * a + b * b) <= U.square(cr);
    }

    public static boolean testPointCircle(final Vector2f p, final Vector2f c, float cr) {
        return testPointCircle(p.x,p.y,c.x,c.y,cr);
    }

    public static boolean testPointCircle(float px, float py, float cx, float cy, float cr) {
        final float a = cx - px;
        final float b = cy - py;
        return ((a * a + b * b) <= U.square(cr));
    }

    public static boolean testCircleCircle(final Vector2f a, float ar, final Vector2f b, float br) {
        return testCircleCircle(a.x,a.y,ar,b.x,b.y,br);
    }

    public static boolean testCircleCircle(float ax, float ay, float ar, float bx, float by, float br) {
        final float dx = bx - ax;
        final float dy = by - ay;
        final float r = ar + br;
        return  (dx * dx + dy * dy) <= (r * r);
    }
}
