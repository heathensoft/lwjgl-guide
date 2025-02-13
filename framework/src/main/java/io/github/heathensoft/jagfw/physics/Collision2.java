package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

import static io.github.heathensoft.jagfw.utils.U.lengthSquared;

/**
 * Frederik Dahl 2/13/2025
 */
public class Collision2 {

    private static final int[][] CARDINALS = {{0,1},{-1,0},{1,0},{0,-1}};
    private static final BodyContact BODY_CONTACT_0 = new BodyContact();
    private static final GeomContact GEOM_CONTACT_0 = new GeomContact();
    private static final GeomContact GEOM_CONTACT_1 = new GeomContact();
    private static final Geometry[][] BLOCK_GEOMETRY_MAP;


    public static void resolveBodyBody(final Body A, final Body B) {
        if (A.isStatic() && B.isStatic()) return;
        final float dx = B.position.x - A.position.x;
        final float dy = B.position.y - A.position.y;
        final float r = A.radius + B.radius;
        final float d_squared = (dx * dx + dy * dy);
        if (d_squared <= r * r) {
            BODY_CONTACT_0.A = A;
            BODY_CONTACT_0.B = B;
            BODY_CONTACT_0.normal.set(dx,dy).normalize();
            BODY_CONTACT_0.depth = r - Math.sqrt(d_squared);
            BODY_CONTACT_0.resolveCollision();
        }
    }


    public static void resolveBodyMap(final Body body, final BlockLayout layout) {
        if (!body.isStatic() && body.radius > 0) {
            final int minX = U.floor(body.position.x - body.radius);
            final int minY = U.floor(body.position.y - body.radius);
            final int maxX = U.ceil(body.position.x  + body.radius);
            final int maxY = U.ceil(body.position.y  + body.radius);
            float MAX_DEPTH = Float.NEGATIVE_INFINITY;
            float RESTITUTION = 0;
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
                                    RESTITUTION = layout.blockRestitution(col,row);
                                    Vector2f normal = GEOM_CONTACT_0.normal;
                                    GEOM_CONTACT_1.normal.set(normal);
                                    GEOM_CONTACT_1.body = body;
                                    GEOM_CONTACT_1.depth = MAX_DEPTH;
                                }
                            }
                        }
                    }
                }
            }
            if (MAX_DEPTH > Float.NEGATIVE_INFINITY) {
                GEOM_CONTACT_1.resolveCollision(RESTITUTION);
            }
        }
    }

    public static void resolveBodyGeometry(final Body body, final Geometry geometry, float restitution) {
        if (bodyGeometry(body,geometry,GEOM_CONTACT_1)) {
            GEOM_CONTACT_1.resolveCollision(restitution);
        }
    }


    private static boolean bodyGeometry(final Body body, final Geometry geometry, GeomContact contact) {
        if (body.isStatic()) return false;
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
                contact.normal.set(point_of_penetration);
                contact.normal.sub(body.position).normalize();
                return true;
            }
        } return false;
    }

    private static Geometry[] blockGeometry(int block_type) {
        return BLOCK_GEOMETRY_MAP[block_type];
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
        public float depth;
        public void resolveCollision(float geometry_restitution) {
            if (!normal.isFinite()) normal.zero();
            // resolve penetration
            body.position.x -= normal.x * depth;
            body.position.y -= normal.y * depth;
            float e = (body.restitution + geometry_restitution) * 0.5f;
            float dot_normal = body.velocity.dot(normal);
            float impulse_magnitude = -(1 + e) * dot_normal / body.mass_inverse;
            body.applyImpulse(body.velocity.x * impulse_magnitude,body.velocity.y * impulse_magnitude);
        }
    }

    private static final class BodyContact {
        public Body A;
        public Body B;
        public final Vector2f normal = new Vector2f();
        public float depth;
        public void resolveCollision() {
            if (!normal.isFinite()) normal.zero();
            { // static bodies (inverse mass = 0) will not move
                float d = depth / (A.mass_inverse + B.mass_inverse);
                float da = d * A.mass_inverse;
                float db = d * B.mass_inverse;
                A.position.x -= normal.x * da;
                A.position.y -= normal.y * da;
                B.position.x += normal.x * db;
                B.position.y += normal.y * db;
            } // Define elasticity (coefficient of restitution e)
            // we are using the average, but you could use min() or something else instead.
            float e = (A.restitution + B.restitution) * 0.5f;
            // relative velocity of the bodies
            Vector2f relative_velocity = U.popSetVec2(A.velocity).sub(B.velocity);
            float dot_normal = relative_velocity.dot(normal);
            float impulse_magnitude = -(1 + e) * dot_normal / (A.mass_inverse + B.mass_inverse);
            Vector2f impulse = relative_velocity.mul(impulse_magnitude);
            A.applyImpulse(impulse);
            B.applyImpulse(impulse.negate());
            U.pushVec2();
        }
    }
}
