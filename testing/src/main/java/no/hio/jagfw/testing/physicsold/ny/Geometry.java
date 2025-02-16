package no.hio.jagfw.testing.physicsold.ny;

import io.github.heathensoft.jagfw.core.utils.LineSegment;
import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 1/30/2025
 */
public class Geometry {
    public final Vector2f[] vertices;
    public final Vector2f offset;
    public boolean sleeping;
    protected boolean polygon;
    public boolean one_way_collision;


    public static Geometry[] blockGeometry(int block_type) {
        return block_geometry_map[block_type];
    }

    public Geometry(Vector2f[] vertices) {
        this.offset = new Vector2f();
        this.vertices = vertices;
    } public Geometry(List<Vector2f> vertices) {
        this.vertices = new Vector2f[vertices.size()];
        for (int i = 0; i < this.vertices.length; i++) {
            this.vertices[i] = vertices.get(i);
        } this.offset = new Vector2f();
    } public Geometry(int capacity) {
        this.vertices = new Vector2f[capacity];
        for (int i = 0; i < capacity; i++) {
            this.vertices[i] = new Vector2f();
        } this.offset = new Vector2f();
    }

    public LineSegment segment(int i0, int i1) {
        return segment(i0,i1,new LineSegment());
    }

    public LineSegment segment(int i0, int i1, LineSegment dst) {
        Vector2f v0 = vertices[U.modRepeat(i0,vertices.length)];
        Vector2f v1 = vertices[U.modRepeat(i1,vertices.length)];
        dst.set(v0.x + offset.x,v0.y + offset.y,v1.x + offset.x ,v1.y + offset.y);
        return dst;
    }

    public int closestVertex(float x, float y) {
        int index = 0;
        float min = Float.POSITIVE_INFINITY;
        if (offset.x == 0 && offset.y == 0) {
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
                float dx = (vertex.x + offset.x) - x;
                float dy = (vertex.y + offset.y) - y;
                float l2 = dx * dx + dy * dy;
                if (min > l2) {
                    min = l2;
                    index = i;
                }
            }
        }
        return index;
    }

    public int closestVertex(Vector2f p) {
        return closestVertex(p.x,p.y);
    }

    public boolean isPolygon() {
        return vertices.length > 2 && polygon;
    }

    public void setPolygon(boolean enable) {
        polygon = enable;
    }

    public int numVertices() {
        return vertices.length;
    }

    public int numSegments() {
        if (vertices.length > 1) {
            if (vertices.length > 2) {
                if (polygon) {
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
        block_geometry_map[0][0].polygon = true;
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
