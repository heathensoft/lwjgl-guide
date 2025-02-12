package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

import java.util.List;

/**
 * Collidable environment.
 * Geometry is "static" (Not affected by forces)
 * Frederik Dahl 1/30/2025
 */
public class Geometry {
    /**
     * Polygon vertices in "local space" (untranslated)
     */
    public final Vector2f[] vertices;
    /**
     * Geometry translation (Offset in position, added to all vertices)
     */
    public final Vector2f translation;
    /**
     * The first vertex is also the last (loops around).
     * Only works for vertices length > 2.
     */
    public boolean should_treat_as_polygon;
    /**
     * IF true, bodies only collide with the geometry in one direction.
     */
    public boolean one_way_collision;


    /**
     * Utility method for "tiled" games (Blocks).
     * There are 16 configurations of geometry needed for a classical block based game.
     * Instead of creating geometry for entire "tile maps" (taking up a huge amount of memory for larger worlds)
     * figure out the block type for blocks close to the body and do collision check with surrounding blocks.
     * (Translate the block geometry to the tile coordinates).
     * @param block_type index between 0 and 15
     * @return block geometry
     */
    public static Geometry[] blockGeometry(int block_type) {
        return block_geometry_map[block_type];
    }

    public Geometry(Vector2f[] vertices) {
        this.translation = new Vector2f();
        this.vertices = vertices;
    }

    public Geometry(List<Vector2f> vertices) {
        this.vertices = new Vector2f[vertices.size()];
        for (int i = 0; i < this.vertices.length; i++) {
            this.vertices[i] = vertices.get(i);
        } this.translation = new Vector2f();
    }

    public Geometry(int capacity) {
        this.vertices = new Vector2f[capacity];
        for (int i = 0; i < capacity; i++) {
            this.vertices[i] = new Vector2f();
        } this.translation = new Vector2f();
    }

    /**
     * Get line segment between vertices
     * (translation applied)
     * @param i0 index of vertex 0
     * @param i1 index of vertex 1
     * @return new line segment
     */
    public LineSegment segment(int i0, int i1) {
        return segment(i0,i1,new LineSegment());
    }

    /**
     * Get line segment between vertices
     * (translation applied)
     * @param i0 index of vertex 0
     * @param i1 index of vertex 1
     * @param dst line segment
     * @return dst
     */
    public LineSegment segment(int i0, int i1, LineSegment dst) {
        Vector2f v0 = vertices[U.modRepeat(i0,vertices.length)];
        Vector2f v1 = vertices[U.modRepeat(i1,vertices.length)];
        dst.set(v0.x + translation.x,v0.y + translation.y,v1.x + translation.x ,v1.y + translation.y);
        return dst;
    }

    /**
     * Get the index of the closest vertex for x and y
     * (translation applied)
     * @param x x
     * @param y y
     * @return the vertex index
     */
    public int closestVertex(float x, float y) {
        int index = 0;
        float min = Float.POSITIVE_INFINITY;
        if (translation.x == 0 && translation.y == 0) {
            for (int i = 0; i < vertices.length; i++) {
                Vector2f vertex = vertices[i];
                float dx = vertex.x - x;
                float dy = vertex.y - y;
                float l2 = dx * dx + dy * dy;
                if (min > l2) {
                    min = l2;
                    index = i;
                }
            }
        } else {
            for (int i = 0; i < vertices.length; i++) {
                Vector2f vertex = vertices[i];
                float dx = (vertex.x + translation.x) - x;
                float dy = (vertex.y + translation.y) - y;
                float l2 = dx * dx + dy * dy;
                if (min > l2) {
                    min = l2;
                    index = i;
                }
            }
        }
        return index;
    }

    /**
     * Get the index of the closest vertex for p
     * (translation applied)
     * @param p p
     * @return the vertex index
     */
    public int closestVertex(Vector2f p) {
        return closestVertex(p.x,p.y);
    }

    /**
     * @return true if geometry should be treated as a polygon (closed geometry)
     * and vertices.length() > 2
     */
    public boolean isPolygon() {
        return vertices.length > 2 && should_treat_as_polygon;
    }

    /**
     * @return length of vertices array (vertices.length())
     */
    public int numVertices() {
        return vertices.length;
    }

    /**
     * @return the number of individual line segments making up the geometry.
     * 0 if vertices.length() <= 1
     * 1 if vertices.length() == 2
     * else:
     * vertices.length() if treated as polygon
     * else vertices.length() - 1
     */
    public int numSegments() {
        if (vertices.length > 1) {
            if (vertices.length > 2) {
                if (should_treat_as_polygon) {
                    return vertices.length;
                } else return vertices.length - 1;
            } return 1;
        } return 0;
    }

    private static final Geometry[][] block_geometry_map;

    static {
        block_geometry_map = new Geometry[16][];
        // ###################################################
        // # # #
        // #   #
        // # # #
        block_geometry_map[0] = new Geometry[1];
        block_geometry_map[0][0] = new Geometry(4);
        block_geometry_map[0][0].vertices[0].set(0,0);
        block_geometry_map[0][0].vertices[1].set(1,0);
        block_geometry_map[0][0].vertices[2].set(1,1);
        block_geometry_map[0][0].vertices[3].set(0,1);
        block_geometry_map[0][0].should_treat_as_polygon = true;
        block_geometry_map[0][0].one_way_collision = true;
        // ###################################################
        // #   #
        // #   #
        // # # #
        block_geometry_map[1] = new Geometry[1];
        block_geometry_map[1][0] = new Geometry(4);
        block_geometry_map[1][0].vertices[0].set(0,1);
        block_geometry_map[1][0].vertices[1].set(0,0);
        block_geometry_map[1][0].vertices[2].set(1,0);
        block_geometry_map[1][0].vertices[3].set(1,1);
        block_geometry_map[1][0].one_way_collision = true;
        // ###################################################
        // # # #
        //     #
        // # # #
        block_geometry_map[2] = new Geometry[1];
        block_geometry_map[2][0] = new Geometry(4);
        block_geometry_map[2][0].vertices[0].set(0,0);
        block_geometry_map[2][0].vertices[1].set(1,0);
        block_geometry_map[2][0].vertices[2].set(1,1);
        block_geometry_map[2][0].vertices[3].set(0,1);
        block_geometry_map[2][0].one_way_collision = true;
        // ###################################################
        //     #
        //     #
        // # # #
        block_geometry_map[3] = new Geometry[1];
        block_geometry_map[3][0] = new Geometry(3);
        block_geometry_map[3][0].vertices[0].set(0,0);
        block_geometry_map[3][0].vertices[1].set(1,0);
        block_geometry_map[3][0].vertices[2].set(1,1);
        block_geometry_map[3][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #
        // # # #
        block_geometry_map[4] = new Geometry[1];
        block_geometry_map[4][0] = new Geometry(4);
        block_geometry_map[4][0].vertices[0].set(1,1);
        block_geometry_map[4][0].vertices[1].set(0,1);
        block_geometry_map[4][0].vertices[2].set(0,0);
        block_geometry_map[4][0].vertices[3].set(1,0);
        block_geometry_map[4][0].one_way_collision = true;
        // ###################################################
        // #
        // #
        // # # #
        block_geometry_map[5] = new Geometry[1];
        block_geometry_map[5][0] = new Geometry(3);
        block_geometry_map[5][0].vertices[0].set(0,1);
        block_geometry_map[5][0].vertices[1].set(0,0);
        block_geometry_map[5][0].vertices[2].set(1,0);
        block_geometry_map[5][0].one_way_collision = true;
        // ###################################################
        // # # #
        //
        // # # #
        block_geometry_map[6] = new Geometry[2]; // 2
        block_geometry_map[6][0] = new Geometry(2);
        block_geometry_map[6][0].vertices[0].set(0,0);
        block_geometry_map[6][0].vertices[1].set(1,0);
        block_geometry_map[6][0].one_way_collision = true;
        block_geometry_map[6][1] = new Geometry(2);
        block_geometry_map[6][1].vertices[0].set(1,1);
        block_geometry_map[6][1].vertices[1].set(0,1);
        block_geometry_map[6][1].one_way_collision = true;
        // ###################################################
        //
        //
        // # # #
        block_geometry_map[7] = new Geometry[1];
        block_geometry_map[7][0] = new Geometry(2);
        block_geometry_map[7][0].vertices[0].set(0,0);
        block_geometry_map[7][0].vertices[1].set(1,0);
        block_geometry_map[7][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #   #
        // #   #
        block_geometry_map[8] = new Geometry[1];
        block_geometry_map[8][0] = new Geometry(4);
        block_geometry_map[8][0].vertices[0].set(1,0);
        block_geometry_map[8][0].vertices[1].set(1,1);
        block_geometry_map[8][0].vertices[2].set(0,1);
        block_geometry_map[8][0].vertices[3].set(0,0);
        block_geometry_map[8][0].one_way_collision = true;
        // ###################################################
        // #   #
        // #   #
        // #   #
        block_geometry_map[9] = new Geometry[2]; // 2
        block_geometry_map[9][0] = new Geometry(2);
        block_geometry_map[9][0].vertices[0].set(0,1);
        block_geometry_map[9][0].vertices[1].set(0,0);
        block_geometry_map[9][0].one_way_collision = true;
        block_geometry_map[9][1] = new Geometry(2);
        block_geometry_map[9][1].vertices[0].set(1,0);
        block_geometry_map[9][1].vertices[1].set(1,1);
        block_geometry_map[9][1].one_way_collision = true;
        // ###################################################
        // # # #
        //     #
        //     #
        block_geometry_map[10] = new Geometry[1];
        block_geometry_map[10][0] = new Geometry(3);
        block_geometry_map[10][0].vertices[0].set(1,0);
        block_geometry_map[10][0].vertices[1].set(1,1);
        block_geometry_map[10][0].vertices[2].set(0,1);
        block_geometry_map[10][0].one_way_collision = true;
        // ###################################################
        //     #
        //     #
        //     #
        block_geometry_map[11] = new Geometry[1];
        block_geometry_map[11][0] = new Geometry(2);
        block_geometry_map[11][0].vertices[0].set(1,0);
        block_geometry_map[11][0].vertices[1].set(1,1);
        block_geometry_map[11][0].one_way_collision = true;
        // ###################################################
        // # # #
        // #
        // #
        block_geometry_map[12] = new Geometry[1];
        block_geometry_map[12][0] = new Geometry(3);
        block_geometry_map[12][0].vertices[0].set(1,1);
        block_geometry_map[12][0].vertices[1].set(0,1);
        block_geometry_map[12][0].vertices[2].set(0,0);
        block_geometry_map[12][0].one_way_collision = true;
        // ###################################################
        // #
        // #
        // #
        block_geometry_map[13] = new Geometry[1];
        block_geometry_map[13][0] = new Geometry(2);
        block_geometry_map[13][0].vertices[0].set(0,1);
        block_geometry_map[13][0].vertices[1].set(0,0);
        block_geometry_map[13][0].one_way_collision = true;
        // ###################################################
        // # # #
        //
        //
        block_geometry_map[14] = new Geometry[1];
        block_geometry_map[14][0] = new Geometry(2);
        block_geometry_map[14][0].vertices[0].set(1,1);
        block_geometry_map[14][0].vertices[1].set(0,1);
        block_geometry_map[14][0].one_way_collision = true;
        // ###################################################
        //
        //
        //
        block_geometry_map[15] = new Geometry[1];
        block_geometry_map[15][0] = new Geometry(0);
        block_geometry_map[15][0].one_way_collision = true;
        // ###################################################
    }

}
