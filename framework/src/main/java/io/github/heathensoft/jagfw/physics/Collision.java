package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.physicsold.ny.Circle;
import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.lengthSquared;

/**
 * Frederik Dahl 2/10/2025
 */
public class Collision {

    private static final GeomContact geom_contact_internal = new GeomContact();

    public static boolean bodyBody(Body A, Body B, BodyContact contact) {
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

    public static boolean bodyGeometry(Body body, Geometry geom, GeomContact contact) {
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


    public static boolean bodyBlock(Body body, int x, int y, int block_type, GeomContact contact) {
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
                if (bodyGeometry(body,geom,geom_contact_internal)) {
                    float depth = geom_contact_internal.depth;
                    if (depth > MAX_DEPTH) {
                        MAX_DEPTH = depth;
                        contact.normal.set(geom_contact_internal.normal);
                        contact.geometry = geom;
                        contact.body = body;
                        contact.depth = depth;
                    }
                }
            } return MAX_DEPTH > Float.NEGATIVE_INFINITY;
        }
    }




    public static boolean intersectionRayCircle(final LineSegment ray, Vector2f c, float cr, Vector2f dst) {
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

    public static boolean testRayCircle(LineSegment ray, Vector2f c, float cr) {
        return testRayCircle(ray,c.x,c.y,cr);
    }

    public static boolean testRayCircle(LineSegment ray, float cx, float cy, float cr) {
        // Will also return true if ray is inside the circle (contained)
        // All ray circle methods are meant for rays with origin outside the circle
        // Todo: This might not be ideal
        Vector2f p = ray.closestPoint(cx,cy,U.popVec2());
        U.pushVec2();
        final float a = p.x - cx;
        final float b = p.y - cy;
        return (a * a + b * b) <= U.square(cr);
    }

    public static boolean testPointCircle(Vector2f p, Vector2f c, float cr) {
        return testPointCircle(p.x,p.y,c.x,c.y,cr);
    }

    public static boolean testPointCircle(float px, float py, float cx, float cy, float cr) {
        final float a = cx - px;
        final float b = cy - py;
        return ((a * a + b * b) <= U.square(cr));
    }

    public static boolean testCircleCircle(Vector2f a, float ar, Vector2f b, float br) {
        return testCircleCircle(a.x,a.y,ar,b.x,b.y,br);
    }

    public static boolean testCircleCircle(float ax, float ay, float ar, float bx, float by, float br) {
        final float dx = bx - ax;
        final float dy = by - ay;
        final float r = ar + br;
        return  (dx * dx + dy * dy) <= (r * r);
    }
}
