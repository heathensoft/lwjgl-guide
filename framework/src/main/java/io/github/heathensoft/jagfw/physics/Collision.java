package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.core.utils.LineSegment;
import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

import java.util.List;

import static io.github.heathensoft.jagfw.core.utils.U.*;

/**
 * Main class for collision detection.
 * Methods starting with "resolve" will check for collision and resolve it if collision occurred.
 * Frederik Dahl 2/13/2025
 */
public class Collision {

    private static final int[][] CARDINALS = {{0,1},{-1,0},{1,0},{0,-1}};
    private static final LineSegment RAY_INTERNAL = new LineSegment();
    private static final RayContact RAY_CONTACT = new RayContact();
    private static final BodyContact BODY_CONTACT = new BodyContact();
    private static final GeomContact GEOM_CONTACT_0 = new GeomContact();
    private static final GeomContact GEOM_CONTACT_1 = new GeomContact();
    private static final Geometry[][] BLOCK_GEOMETRY_MAP;

    /**
     * Handles collision for all Physics Bodies and Blocks.
     * This includes Body vs.Bodies, Body vs.Blocks and Body vs. geometry (in that order)
     * @param bodies relevant bodies (use arraylist not linked list)
     * @param geometry relevant geometry
     * @param layout layout of blocks
     */
    public static void resolve(List<Body> bodies, List<Geometry> geometry, BlockLayout layout) {
        int num_bodies = bodies.size();
        for (int i = 0; i < num_bodies; i++) {
            Body A = bodies.get(i);
            for (int j = i + 1; j < num_bodies; j++) {
                Body B = bodies.get(j);
                resolveBodyBody(A,B);
            } resolveBodyMap(A,layout);
            resolveBodyGeometry(A,geometry);
        }
    }

    /**
     * Handles collision for all Physics Bodies and Blocks.
     * This includes Body vs.Bodies and Body vs.Blocks.
     * @param bodies relevant bodies (use arraylist not linked list)
     * @param layout layout of blocks
     */
    public static void resolve(List<Body> bodies, BlockLayout layout) {
        int num_bodies = bodies.size();
        for (int i = 0; i < num_bodies; i++) {
            Body A = bodies.get(i);
            for (int j = i + 1; j < num_bodies; j++) {
                Body B = bodies.get(j);
                resolveBodyBody(A,B);
            } resolveBodyMap(A,layout);
        }
    }

    public static void resolve(List<Body> bodies) {
        int num_bodies = bodies.size();
        for (int i = 0; i < num_bodies; i++) {
            Body A = bodies.get(i);
            for (int j = i + 1; j < num_bodies; j++) {
                Body B = bodies.get(j);
                resolveBodyBody(A,B);
            }
        }
    }

    /**
     * Checks whether two bodies are colliding.
     * If a collision has occurred, the collision is resolved:
     * Applying appropriate impulses to the bodies based on gathered collision information.
     * @param A Body A
     * @param B Body B
     */
    public static void resolveBodyBody(final Body A, final Body B) {
        if (A.isStatic() && B.isStatic()) return;
        final float dx = B.position.x - A.position.x;
        final float dy = B.position.y - A.position.y;
        final float r = A.radius + B.radius;
        final float d_squared = (dx * dx + dy * dy);
        if (d_squared <= r * r) {
            BODY_CONTACT.A = A;
            BODY_CONTACT.B = B;
            BODY_CONTACT.normal.set(dx,dy).normalize();
            BODY_CONTACT.depth = r - Math.sqrt(d_squared);
            BODY_CONTACT.start.set(BODY_CONTACT.normal).negate();
            BODY_CONTACT.start.mul(B.radius).add(A.position);
            BODY_CONTACT.end.set(BODY_CONTACT.normal);
            BODY_CONTACT.end.mul(A.radius).add(A.position);
            BODY_CONTACT.resolveCollision();
        }
    }

    /**
     * Checks whether a Body is colliding with a tile map (blocks).
     * Only immediately surrounding tiles are checked.
     * If a collision has occurred, the collision is resolved:
     * Applying appropriate impulse to the body based on gathered collision information.
     * @param body Body
     * @param layout tile map (block layout)
     */
    public static void resolveBodyMap(final Body body, final BlockLayout layout) {
        if (!body.isStatic() && body.radius > 0) {
            final int minX = U.floor(body.position.x - body.radius);
            final int minY = U.floor(body.position.y - body.radius);
            final int maxX = U.ceil(body.position.x  + body.radius);
            final int maxY = U.ceil(body.position.y  + body.radius);
            float MAX_DEPTH = Float.NEGATIVE_INFINITY;
            for (int row = minY; row <= maxY; row++) {
                for (int col = minX; col <= maxX; col++) {
                    int block_type = blockType(col,row,layout);
                    if (block_type != 0xF) {
                        Geometry[] block_geom = blockGeometry(block_type);
                        for (Geometry geometry : block_geom) {
                            geometry.translation.set(col,row);
                            if (bodyGeometry(body,geometry,GEOM_CONTACT_0)) {
                                if (GEOM_CONTACT_0.depth > MAX_DEPTH) {
                                    MAX_DEPTH = GEOM_CONTACT_0.depth;
                                    GEOM_CONTACT_1.normal.set(GEOM_CONTACT_0.normal);
                                    GEOM_CONTACT_1.point.set(GEOM_CONTACT_0.point);
                                    GEOM_CONTACT_1.body = body;
                                    GEOM_CONTACT_1.depth = MAX_DEPTH;
                                    GEOM_CONTACT_1.friction = layout.blockFriction(col,row);
                                    GEOM_CONTACT_1.restitution = layout.blockRestitution(col,row);
                                }
                            }
                        }
                    }
                }
            }
            if (MAX_DEPTH > Float.NEGATIVE_INFINITY) {
                GEOM_CONTACT_1.resolveCollision();
            }
        }
    }

    public static void resolveBodyGeometry(final Body body, final List<Geometry> geometry) {
        for (Geometry geom : geometry) {
            resolveBodyGeometry(body,geom);
        }
    }

    public static void resolveBodyGeometry(final Body body, final Geometry geometry) {
        if (bodyGeometry(body,geometry,GEOM_CONTACT_1)) {
            GEOM_CONTACT_1.resolveCollision();
        }
    }

    public static boolean rayMap(final LineSegment ray, final BlockLayout layout, RayContact contact) {
        // https://lodev.org/cgtutor/raycasting.html
        // https://til.zimventures.com/GameMaker/dda
        if (ray.isValid()) {
            int tile_x = U.floor(ray.x0);
            int tile_y = U.floor(ray.y0);
            if (layout.contains(tile_x,tile_y)) {
                if (layout.isBlock(tile_x,tile_y)) {
                    // if inside a block, ignore ray collisions
                    return false;
                }
            }
            if (tile_x == U.floor(ray.x1) && tile_y == U.floor(ray.y1)) {
                // Don't check if the ray is entirely inside a single tile
                return false;
            } // ray direction components
            Vector2f ray_dir = ray.direction(U.popVec2());
            final float ray_dir_x = ray_dir.x;
            final float ray_dir_y = ray_dir.y;
            U.pushVec2(1);
            // length of hyp for traveling on unit in x
            final float scaling_factor_x = Math.sqrt(1 + U.square(ray_dir_y / ray_dir_x));
            // length of hyp for traveling on unit in y
            final float scaling_factor_y = Math.sqrt(1 + U.square(ray_dir_x / ray_dir_y));
            // ray length
            float ray_len = ray.length();
            // current length of the ray for steps in x
            float ray_len_x;
            // current length of the ray for steps in y
            float ray_len_y;
            int step_x; // step direction x
            int step_y; // step direction y
            if (ray_dir.x < 0) { step_x = -1;
                ray_len_x = (ray.x0 - tile_x) * scaling_factor_x;
            } else { step_x = 1;
                ray_len_x = (tile_x + 1 - ray.x0) * scaling_factor_x;
            } if (ray_dir.y < 0) { step_y = -1;
                ray_len_y = (ray.y0 - tile_y) * scaling_factor_y;
            }  else { step_y = 1;
                ray_len_y = (tile_y + 1 - ray.y0) * scaling_factor_y;
            } float distance = 0f;
            while (distance < ray_len) {
                if (ray_len_y > ray_len_x) {
                    tile_x += step_x;
                    distance = ray_len_x;
                    ray_len_x += scaling_factor_x;
                    if (distance < ray_len) {
                        if (layout.contains(tile_x,tile_y)) {
                            if (layout.isBlock(tile_x,tile_y)) {
                                LineSegment edge = popLine();
                                if (step_x > 0) {
                                    edge.x0 = tile_x;
                                    edge.y0 = tile_y + 1;
                                    edge.x1 = tile_x;
                                    edge.y1 = tile_y;
                                } else {
                                    edge.x0 = tile_x + 1;
                                    edge.y0 = tile_y;
                                    edge.x1 = tile_x + 1;
                                    edge.y1 = tile_y + 1;
                                }
                                edge.normal(contact.normal);
                                contact.point.set(
                                        ray_dir_x * distance + ray.x0,
                                        ray_dir_y * distance + ray.y0
                                ); contact.ray.set(ray);
                                U.pushLine();
                                return true;
                            }
                        }
                    }
                } else {
                    tile_y += step_y;
                    distance = ray_len_y;
                    ray_len_y += scaling_factor_y;
                    if (distance < ray_len) {
                        if (layout.contains(tile_x,tile_y)) {
                            if (layout.isBlock(tile_x,tile_y)) {
                                if (layout.isBlock(tile_x,tile_y)) {
                                    LineSegment edge = popLine();
                                    if (step_y > 0) {
                                        edge.x0 = tile_x;
                                        edge.y0 = tile_y;
                                        edge.x1 = tile_x + 1;
                                        edge.y1 = tile_y;
                                    } else {
                                        edge.x0 = tile_x + 1;
                                        edge.y0 = tile_y + 1;
                                        edge.x1 = tile_x;
                                        edge.y1 = tile_y + 1;
                                    } edge.normal(contact.normal);
                                    contact.point.set(
                                            ray_dir_x * distance + ray.x0,
                                            ray_dir_y * distance + ray.y0
                                    ); contact.ray.set(ray);
                                    U.pushLine();
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } return false;
    }

    public static boolean rayGeometry(final LineSegment ray, final List<Geometry> geometry, RayContact contact) {
        RAY_INTERNAL.set(ray);
        for (Geometry geom : geometry) {
            if (rayGeometry(RAY_INTERNAL,geom,RAY_CONTACT)) {
                contact.ray.set(RAY_CONTACT.ray);
                contact.normal.set(RAY_CONTACT.normal);
                contact.point.set(RAY_CONTACT.point);
                RAY_INTERNAL.setP1(contact.point); }
        } return RAY_INTERNAL.lengthSquared() < ray.lengthSquared();
    }

    public static boolean rayGeometry(final LineSegment ray, final Geometry geometry, RayContact contact) {
        if (ray.isValid()) {
            Vector2f ray_dir = ray.direction();
            Vector2f[] vertices = geometry.vertices;
            LineSegment edge = popLine();
            int num_segments = geometry.numSegments();
            float min_dist2 = Float.POSITIVE_INFINITY;
            for (int i = 0; i < num_segments; i++) {
                Vector2f p0 = vertices[i];
                Vector2f p1 = vertices[(i + 1) % vertices.length];
                contact.normal.set(p1).sub(p0);
                contact.normal.perpendicular();
                float dot = contact.normal.dot(ray_dir);
                if (dot < 0) {
                    edge.set(p0,p1);
                    if (edge.intersects(ray,contact.point)) {
                        Vector2f origin_to_point = U.popVec2();
                        origin_to_point.set(contact.point).sub(ray.x0,ray.y0);
                        float len2 = origin_to_point.lengthSquared();
                        if (len2 < min_dist2) {
                            contact.ray.set(ray);
                            contact.normal.normalize();
                            min_dist2 = len2;
                        } U.pushVec2();
                    }
                } else if (!geometry.one_way_collision) {
                    edge.set(p0,p1);
                    if (edge.intersects(ray,contact.point)) {
                        Vector2f origin_to_point = U.popVec2();
                        origin_to_point.set(contact.point).sub(ray.x0,ray.y0);
                        float len2 = origin_to_point.lengthSquared();
                        if (len2 < min_dist2) {
                            contact.ray.set(ray);
                            contact.normal.negate();
                            contact.normal.normalize();
                            min_dist2 = len2;
                        } U.pushVec2();
                    }
                }
            } U.pushLine();
            return min_dist2 < Float.POSITIVE_INFINITY;
        } return false;
    }

    public static boolean rayBodies(final LineSegment ray, final List<Body> bodies, RayContact contact) {
        RAY_INTERNAL.set(ray);
        for (Body body : bodies) {
            if (rayBody(RAY_INTERNAL,body,RAY_CONTACT)) {
                contact.ray.set(RAY_CONTACT.ray);
                contact.normal.set(RAY_CONTACT.normal);
                contact.point.set(RAY_CONTACT.point);
                RAY_INTERNAL.setP1(contact.point); }
        } return RAY_INTERNAL.lengthSquared() < ray.lengthSquared();
    }

    public static boolean rayBody(final LineSegment ray, final Body body, RayContact contact) {
        return rayCircle(ray,body.position.x,body.position.y,body.radius,contact);
    }

    public static boolean rayConvexPolygon(final LineSegment ray, final Vector2f[] vertices, RayContact contact) {
        if (ray.isValid() && vertices.length > 2) {
            Vector2f ray_dir = ray.direction();
            LineSegment edge = new LineSegment();
            boolean intersect_from_inside = false;
            for (int i = 0; i < vertices.length; i++) {
                Vector2f p0 = vertices[i];
                Vector2f p1 = vertices[(i + 1) % vertices.length];
                edge.set(p0,p1);
                if (edge.isValid()) {
                    contact.normal.set(p1).sub(p0);
                    contact.normal.perpendicular();
                    if (edge.intersects(ray,contact.point)) {
                        float dot = contact.normal.dot(ray_dir);
                        if (dot < 0) {
                            contact.ray.set(ray);
                            contact.normal.normalize();
                            U.pushLine();
                            return true;
                        } else intersect_from_inside = true;
                    }
                }
            }
            if (intersect_from_inside) {
                // at this point we have intersected the polygon
                // from the inside, but not from the outside.
                // This means the ray origin is on the inside,
                // and we set the collision point to ray origin.
                // The normal is the ray direction negated.
                // For this to work as intended it's important
                // to:
                // 1. order the polygon vertices in anti-clockwise order.
                // 2. for the polygon to be convex.
                contact.normal.set(ray_dir).negate();
                contact.point.set(ray.x0,ray.y0);
                contact.ray.set(ray);
                return true;
            }
        } return false;
    }

    public static boolean rayCircle(final LineSegment ray, final Vector2f c, float cr, RayContact contact) {
        return rayCircle(ray,c.x,c.y,cr,contact);
    }

    public static boolean rayCircle(final LineSegment ray, float cx, float cy, float cr, RayContact contact) {
        if (ray.isValid() && cr > 0) {
            if (intersectionRayCircle(ray,cx,cy,cr,contact.point)) {
                contact.normal.set(contact.point);
                contact.normal.sub(cx,cy).normalize();
                // if normal in NaN we set it to ray direction neg
                // This happens if ray origin is inside the circle
                if (!contact.normal.isFinite()) {
                    ray.direction(contact.normal).negate();
                } contact.ray.set(ray);
                return true; }
        } return false;
    }

    public static boolean rayBox(final LineSegment ray, final Rectanglef rect, RayContact contact) {
        if (ray.isValid() && rect.isValid()) {
            if (rect.containsPoint(ray.x0,ray.y0)) {
                ray.p0(contact.point);
                ray.direction(contact.normal).negate();
                contact.ray.set(ray);
                return true;
            } else try {
                LineSegment edge = popLine();
                Vector2f edge_normal = U.popVec2();
                Vector2f ray_dir = ray.direction(U.popVec2());
                Vector2f point = U.popVec2();
                // v0 -> v1
                edge.x0 = rect.minX;
                edge.y0 = rect.maxY;
                edge.x1 = rect.minX;
                edge.y1 = rect.minY;
                edge.normal(edge_normal);
                float dot = edge_normal.dot(ray_dir);
                if (dot < 0 && edge.intersects(ray,point)) {
                    contact.ray.set(ray);
                    contact.normal.set(edge_normal);
                    contact.point.set(point);
                    return true;
                }
                // v2 -> v3
                edge.x0 = rect.maxX;
                edge.y0 = rect.minY;
                edge.x1 = rect.maxX;
                edge.y1 = rect.maxY;
                if (-dot < 0 && edge.intersects(ray,point)) {
                    contact.ray.set(ray);
                    contact.normal.set(edge_normal);
                    contact.normal.negate();
                    contact.point.set(point);
                    return true;
                }
                // v1 -> v2
                edge.x0 = rect.minX;
                edge.y0 = rect.minY;
                edge.x1 = rect.maxX;
                edge.y1 = rect.minY;
                edge.normal(edge_normal);
                dot = edge_normal.dot(ray_dir);
                if (dot < 0 && edge.intersects(ray,point)) {
                    contact.ray.set(ray);
                    contact.normal.set(edge_normal);
                    contact.point.set(point);
                    return true;
                }
                // v3 -> v0
                edge.x0 = rect.maxX;
                edge.y0 = rect.maxY;
                edge.x1 = rect.minX;
                edge.y1 = rect.maxY;
                if (-dot < 0 && edge.intersects(ray,point)) {
                    contact.ray.set(ray);
                    contact.normal.set(edge_normal);
                    contact.normal.negate();
                    contact.point.set(point);
                    return true;
                }
            } finally {
                U.pushLine();
                U.pushVec2(3);
            }
        } return false;
    }


    private static boolean testRayCircle(final LineSegment ray, final Vector2f c, float cr) {
        return testRayCircle(ray,c.x,c.y,cr);
    }

    private static boolean testRayCircle(final LineSegment ray, float cx, float cy, float cr) {
        // Will also return true if ray is inside the circle (contained)
        // All ray circle methods are meant for rays with origin outside the circle
        Vector2f p = ray.closestPoint(cx,cy,U.popVec2());
        U.pushVec2();
        final float a = p.x - cx;
        final float b = p.y - cy;
        return (a * a + b * b) <= U.square(cr);
    }

    private static boolean bodyGeometry(final Body body, final Geometry geometry, GeomContact contact) {
        if (body.isStatic()) return false;
        int num_segments = geometry.numSegments();
        if (num_segments >= 1) {
            int s0, s1;
            if (num_segments == 1) {
                s0 = 0; s1 = 0;
            } else {
                int closest_vertex = geometry.closestVertex(body.position);
                if (geometry.isPolygon()) {
                    s0 = closest_vertex - 1;
                    s1 = closest_vertex;
                } else {
                    if (closest_vertex == 0) {
                        s0 = 0; s1 = 0;
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
                LineSegment segment = geometry.segment(s,s+1);
                if (segment.isValid()) {
                    // check one way collision
                    if (geometry.one_way_collision) {
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
                contact.depth = max_depth;
                contact.point.set(point_of_penetration);
                contact.normal.set(point_of_penetration);
                contact.normal.sub(body.position).normalize();
                contact.friction = geometry.friction;
                contact.restitution = geometry.restitution;
                return true;
            }
        } return false;
    }

    private static boolean intersectionRayCircle(final LineSegment ray, final Vector2f c, float cr, Vector2f dst) {
        return intersectionRayCircle(ray,c.x,c.y,cr,dst);
    }

    private static boolean intersectionRayCircle(final LineSegment ray, float cx, float cy, float cr, Vector2f dst) {
        if (ray.isValid()) {
            if (U.testPointCircle(ray.x0,ray.y0,cx,cy,cr)) {
                dst.set(ray.x0,ray.y0);
                return true;
            } Vector2f p = ray.closestPoint(cx,cy,dst);
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

    private static int blockType(int x, int y, final BlockLayout layout) {
        if (layout.contains(x,y)) {
            if (layout.isBlock(x,y)) {
                int block_type = 0;
                for (int i = 0; i < CARDINALS.length; i++) {
                    int nx = x + CARDINALS[i][0];
                    int ny = y + CARDINALS[i][1];
                    if (layout.contains(nx,ny)) {
                        if (layout.isBlock(nx,ny)) {
                            block_type |= (1 << i); }
                    } else block_type |= (1 << i);
                } return block_type; }
        } return 0xF;
    }

    private static Geometry[] blockGeometry(int block_type) {
        return BLOCK_GEOMETRY_MAP[block_type];
    }

    static {
        BLOCK_GEOMETRY_MAP = new Geometry[16][];
        // ###################################################
        // # # #
        // #   #
        // # # #
        BLOCK_GEOMETRY_MAP[0] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[0][0] = new Geometry(4);
        BLOCK_GEOMETRY_MAP[0][0].vertices[0].set(0,0);
        BLOCK_GEOMETRY_MAP[0][0].vertices[1].set(1,0);
        BLOCK_GEOMETRY_MAP[0][0].vertices[2].set(1,1);
        BLOCK_GEOMETRY_MAP[0][0].vertices[3].set(0,1);
        BLOCK_GEOMETRY_MAP[0][0].should_treat_as_polygon = true;
        BLOCK_GEOMETRY_MAP[0][0].one_way_collision = true;
        // ###################################################
        // #   #
        // #   #
        // # # #
        BLOCK_GEOMETRY_MAP[1] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[1][0] = new Geometry(4);
        BLOCK_GEOMETRY_MAP[1][0].vertices[0].set(0,1);
        BLOCK_GEOMETRY_MAP[1][0].vertices[1].set(0,0);
        BLOCK_GEOMETRY_MAP[1][0].vertices[2].set(1,0);
        BLOCK_GEOMETRY_MAP[1][0].vertices[3].set(1,1);
        BLOCK_GEOMETRY_MAP[1][0].one_way_collision = true;
        // ###################################################
        // # # #
        //     #
        // # # #
        BLOCK_GEOMETRY_MAP[2] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[2][0] = new Geometry(4);
        BLOCK_GEOMETRY_MAP[2][0].vertices[0].set(0,0);
        BLOCK_GEOMETRY_MAP[2][0].vertices[1].set(1,0);
        BLOCK_GEOMETRY_MAP[2][0].vertices[2].set(1,1);
        BLOCK_GEOMETRY_MAP[2][0].vertices[3].set(0,1);
        BLOCK_GEOMETRY_MAP[2][0].one_way_collision = true;
        // ###################################################
        //     #
        //     #
        // # # #
        BLOCK_GEOMETRY_MAP[3] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[3][0] = new Geometry(3);
        BLOCK_GEOMETRY_MAP[3][0].vertices[0].set(0,0);
        BLOCK_GEOMETRY_MAP[3][0].vertices[1].set(1,0);
        BLOCK_GEOMETRY_MAP[3][0].vertices[2].set(1,1);
        BLOCK_GEOMETRY_MAP[3][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #
        // # # #
        BLOCK_GEOMETRY_MAP[4] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[4][0] = new Geometry(4);
        BLOCK_GEOMETRY_MAP[4][0].vertices[0].set(1,1);
        BLOCK_GEOMETRY_MAP[4][0].vertices[1].set(0,1);
        BLOCK_GEOMETRY_MAP[4][0].vertices[2].set(0,0);
        BLOCK_GEOMETRY_MAP[4][0].vertices[3].set(1,0);
        BLOCK_GEOMETRY_MAP[4][0].one_way_collision = true;
        // ###################################################
        // #
        // #
        // # # #
        BLOCK_GEOMETRY_MAP[5] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[5][0] = new Geometry(3);
        BLOCK_GEOMETRY_MAP[5][0].vertices[0].set(0,1);
        BLOCK_GEOMETRY_MAP[5][0].vertices[1].set(0,0);
        BLOCK_GEOMETRY_MAP[5][0].vertices[2].set(1,0);
        BLOCK_GEOMETRY_MAP[5][0].one_way_collision = true;
        // ###################################################
        // # # #
        //
        // # # #
        BLOCK_GEOMETRY_MAP[6] = new Geometry[2]; // 2
        BLOCK_GEOMETRY_MAP[6][0] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[6][0].vertices[0].set(0,0);
        BLOCK_GEOMETRY_MAP[6][0].vertices[1].set(1,0);
        BLOCK_GEOMETRY_MAP[6][0].one_way_collision = true;
        BLOCK_GEOMETRY_MAP[6][1] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[6][1].vertices[0].set(1,1);
        BLOCK_GEOMETRY_MAP[6][1].vertices[1].set(0,1);
        BLOCK_GEOMETRY_MAP[6][1].one_way_collision = true;
        // ###################################################
        //
        //
        // # # #
        BLOCK_GEOMETRY_MAP[7] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[7][0] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[7][0].vertices[0].set(0,0);
        BLOCK_GEOMETRY_MAP[7][0].vertices[1].set(1,0);
        BLOCK_GEOMETRY_MAP[7][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #   #
        // #   #
        BLOCK_GEOMETRY_MAP[8] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[8][0] = new Geometry(4);
        BLOCK_GEOMETRY_MAP[8][0].vertices[0].set(1,0);
        BLOCK_GEOMETRY_MAP[8][0].vertices[1].set(1,1);
        BLOCK_GEOMETRY_MAP[8][0].vertices[2].set(0,1);
        BLOCK_GEOMETRY_MAP[8][0].vertices[3].set(0,0);
        BLOCK_GEOMETRY_MAP[8][0].one_way_collision = true;
        // ###################################################
        // #   #
        // #   #
        // #   #
        BLOCK_GEOMETRY_MAP[9] = new Geometry[2]; // 2
        BLOCK_GEOMETRY_MAP[9][0] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[9][0].vertices[0].set(0,1);
        BLOCK_GEOMETRY_MAP[9][0].vertices[1].set(0,0);
        BLOCK_GEOMETRY_MAP[9][0].one_way_collision = true;
        BLOCK_GEOMETRY_MAP[9][1] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[9][1].vertices[0].set(1,0);
        BLOCK_GEOMETRY_MAP[9][1].vertices[1].set(1,1);
        BLOCK_GEOMETRY_MAP[9][1].one_way_collision = true;
        // ###################################################
        // # # #
        //     #
        //     #
        BLOCK_GEOMETRY_MAP[10] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[10][0] = new Geometry(3);
        BLOCK_GEOMETRY_MAP[10][0].vertices[0].set(1,0);
        BLOCK_GEOMETRY_MAP[10][0].vertices[1].set(1,1);
        BLOCK_GEOMETRY_MAP[10][0].vertices[2].set(0,1);
        BLOCK_GEOMETRY_MAP[10][0].one_way_collision = true;
        // ###################################################
        //     #
        //     #
        //     #
        BLOCK_GEOMETRY_MAP[11] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[11][0] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[11][0].vertices[0].set(1,0);
        BLOCK_GEOMETRY_MAP[11][0].vertices[1].set(1,1);
        BLOCK_GEOMETRY_MAP[11][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #
        // #
        BLOCK_GEOMETRY_MAP[12] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[12][0] = new Geometry(3);
        BLOCK_GEOMETRY_MAP[12][0].vertices[0].set(1,1);
        BLOCK_GEOMETRY_MAP[12][0].vertices[1].set(0,1);
        BLOCK_GEOMETRY_MAP[12][0].vertices[2].set(0,0);
        BLOCK_GEOMETRY_MAP[12][0].one_way_collision = true;
        // ###################################################
        // #
        // #
        // #
        BLOCK_GEOMETRY_MAP[13] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[13][0] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[13][0].vertices[0].set(0,1);
        BLOCK_GEOMETRY_MAP[13][0].vertices[1].set(0,0);
        BLOCK_GEOMETRY_MAP[13][0].one_way_collision = true;
        // ###################################################
        // # # #
        //
        //
        BLOCK_GEOMETRY_MAP[14] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[14][0] = new Geometry(2);
        BLOCK_GEOMETRY_MAP[14][0].vertices[0].set(1,1);
        BLOCK_GEOMETRY_MAP[14][0].vertices[1].set(0,1);
        BLOCK_GEOMETRY_MAP[14][0].one_way_collision = true;
        // ###################################################
        //
        //
        //
        BLOCK_GEOMETRY_MAP[15] = new Geometry[1];
        BLOCK_GEOMETRY_MAP[15][0] = new Geometry(0);
        BLOCK_GEOMETRY_MAP[15][0].one_way_collision = true;
        // ###################################################
    }

    private static final class GeomContact {
        public Body body;
        public final Vector2f normal = new Vector2f();
        public final Vector2f point = new Vector2f();
        public float depth;
        public float friction;
        public float restitution;
        public void resolveCollision() {
            if (!normal.isFinite()) normal.zero();
            Vector2f jn, jt;
            Vector2f tmp0 = U.popVec2();
            Vector2f tmp1 = U.popVec2();
            Vector2f tmp2 = U.popVec2();
            Vector2f r = tmp0.set(point).sub(body.position);

            // resolve penetration
            body.position.x -= normal.x * depth;
            body.position.y -= normal.y * depth;

            // Define elasticity (coefficient of restitution e) and friction
            float f = (body.friction + friction) * 0.5f;
            float e = (body.restitution + restitution) * 0.5f;

            // calculate moment of inertia for circle shapes
            float moi_inverse;
            {
                float moi = 0.5f * U.square(body.radius) / body.mass_inverse;
                moi_inverse = moi == 0 ? 0 : 1 / moi;
            }

            { // impulse along the collision normal
                // the relative velocity along the collision normal
                float dot_normal = body.velocity.dot(normal);
                float numerator = -(1 + e) * dot_normal;
                float r_cross_nor = cross(r,normal);
                float denominator = body.mass_inverse;
                denominator += U.square(r_cross_nor) * moi_inverse;
                float impulse_magnitude = numerator / denominator;
                jn = tmp1.set(normal).mul(impulse_magnitude);
            }
            { // collision impulse along the tangent (including friction)
                Vector2f tangent = tmp2.set(normal).perpendicular();
                // the relative velocity along the collision normal
                float dot_tangent = body.velocity.dot(tangent);
                // calculating impulse magnitude
                float r_cross_tan = cross(r,tangent);
                float numerator = f * -(1 + e) * dot_tangent;
                float denominator = body.mass_inverse;
                denominator += U.square(r_cross_tan) * moi_inverse;
                float impulse_magnitude = numerator / denominator;
                jt = tangent.mul(impulse_magnitude);
            }
            body.applyImpulse(jn.add(jt));
            U.pushVec2(3);
        }
    }

    private static final class BodyContact {
        public Body A;
        public Body B;
        public final Vector2f normal = new Vector2f();
        public final Vector2f start = new Vector2f();
        public final Vector2f end = new Vector2f();
        public float depth;
        public void resolveCollision() {
            // impulse, impulse along normal, impulse along tangent
            Vector2f j, jn, jt;
            Vector2f tmp0 = U.popVec2();
            Vector2f tmp1 = U.popVec2();
            Vector2f tmp2 = U.popVec2();
            Vector2f tmp3 = U.popVec2();
            Vector2f tmp4 = U.popVec2();

            Vector2f ra = tmp0.set(end).sub(A.position);
            Vector2f rb = tmp1.set(start).sub(B.position);

            if (!normal.isFinite()) normal.zero();
            resolvePenetration(); // modifies position directly (out of collision)

            // Define elasticity (coefficient of restitution e) and friction.
            // I take the average, but you could also use min
            final float f = (A.friction + B.friction) * 0.5f;
            final float e = (A.restitution + B.restitution) * 0.5f;

            // calculate moment of inertia for circle shapes
            float a_moi_inv, b_moi_inv;
            {
                float a_moi = 0.5f * U.square(A.radius) / A.mass_inverse;
                float b_moi = 0.5f * U.square(B.radius) / B.mass_inverse;
                a_moi_inv = a_moi == 0 ? 0 : 1 / a_moi;
                b_moi_inv = b_moi == 0 ? 0 : 1 / b_moi;
            }

            // relative velocity is the linear + angular velocity of body a
            // minus the linear + angular velocity of body b
            // since we are not using angular velocity (rotation), the
            // relative velocity is simply the relative linear velocity
            Vector2f relative_velocity = tmp4.set(A.velocity).sub(B.velocity);


            { // impulse along the collision normal
                // the relative velocity along the collision normal
                float dot_normal = relative_velocity.dot(normal);
                // calculating impulse magnitude
                float ra_cross_nor = cross(ra,normal);
                float rb_cross_nor = cross(rb,normal);
                float numerator = -(1 + e) * dot_normal;
                float denominator = (A.mass_inverse + B.mass_inverse);
                denominator += (ra_cross_nor * ra_cross_nor) * a_moi_inv;
                denominator += (rb_cross_nor * rb_cross_nor) * b_moi_inv;
                float impulse_magnitude = numerator / denominator;
                jn = tmp2.set(normal).mul(impulse_magnitude);
            }

            { // collision impulse along the tangent (including friction)
                Vector2f tangent = tmp3.set(normal).perpendicular();
                // the relative velocity along the collision normal
                float dot_tangent = relative_velocity.dot(tangent);
                // calculating impulse magnitude
                float ra_cross_tan = cross(ra,tangent);
                float rb_cross_tan = cross(rb,tangent);
                float numerator = f * -(1 + e) * dot_tangent;
                float denominator = (A.mass_inverse + B.mass_inverse);
                denominator += (ra_cross_tan * ra_cross_tan) * a_moi_inv;
                denominator += (rb_cross_tan * rb_cross_tan) * b_moi_inv;
                float impulse_magnitude = numerator / denominator;
                jt = tangent.mul(impulse_magnitude);
            }
            j = jn.add(jt);
            A.applyImpulse(j);
            B.applyImpulse(j.negate());
            U.pushVec2(5);
        }

        private void resolvePenetration() {
            // static bodies (inverse mass = 0) will not move
            float d = depth / (A.mass_inverse + B.mass_inverse);
            float da = d * A.mass_inverse;
            float db = d * B.mass_inverse;
            A.position.x -= normal.x * da;
            A.position.y -= normal.y * da;
            B.position.x += normal.x * db;
            B.position.y += normal.y * db;
        }
    }
}
