package io.github.heathensoft.jagfw.physicsold.shape;

import org.joml.Math;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * A Box is a PolygonShape with 4 vertices.
 * The edges are in counter-clockwise order
 * Frederik Dahl 1/23/2025
 */
public class Box extends PolygonShape {
    /*
     * v0------v3
     * |        |
     * |        |
     * v1------v2
     */
    protected final float width;
    protected final float height;
    protected final Vector2f[] vertices = new Vector2f[4];
    public Box(float size) { this(size,size); }
    public Box(float width, float height) {
        this.width = width;
        this.height = height;
        this.radius = Math.sqrt(width * width + height * height) / 2f;
        for (int i = 0; i < vertices.length; i++)
            vertices[i] = new Vector2f();
        vertices[0].set(-(width / 2f), (height / 2f));
        vertices[1].set(-(width / 2f),-(height / 2f));
        vertices[2].set( (width / 2f),-(height / 2f));
        vertices[3].set( (width / 2f), (height / 2f));
    }

    public float width() { return width; }
    public float height() { return height; }
    public Vector2f[] vertices() { return vertices; }
    public Rectanglef boundingBox(Rectanglef dst) {
        // Don't do this for more complex polygons
        float min_x = Float.POSITIVE_INFINITY;
        float min_y = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY;
        float max_y = Float.NEGATIVE_INFINITY;
        for (Vector2f vertex : vertices) {
            min_x = Math.min(min_x, vertex.x);
            min_y = Math.min(min_y, vertex.y);
            max_x = Math.max(max_x, vertex.x);
            max_y = Math.max(max_y, vertex.y);}
        dst.minX = min_x;
        dst.minY = min_y;
        dst.maxX = max_x;
        dst.maxY = max_y;
        return dst;
    }

    public float calculateMomentOfInertia(float mass) {
        // For a rectangle, the moment of inertia is 1/12 * (w^2 + h^2) * mass
        return (0.083333f) * (width * width + height * height) * mass;
    }

    /**
     * The Box is Axis Aligned when the
     * distance in x between vertex[0] and vertex[1] (left edge)
     * is either 0 (rotation = 0 or PI) or height (rotation = PI/2 or 3PI/2)
     * A sure way to keep Boxes axis aligned is to keep rotation at zero
     * Axis Aligned boxes are cheaper for collision detection
     * @return whether the Box is perfectly Axis aligned
     */
    public boolean isAxisAligned() {
        float dx = Math.abs(vertices[0].x - vertices[1].x);
        return dx == 0 || dx == height;
    }

    public void updateVertices(Vector2f position, float rotation) {
        final float wh = width * 0.5f;
        final float hh = height * 0.5f;
        float rot = rotation % Math.PI_TIMES_2_f;
        if (rot < 0) rot += Math.PI_TIMES_2_f;
        if (rot == 0) {
            // we only test for rotation ~= 0.
            // Don't want to check all other axis aligned
            // angles for special cases.
            // keep rotation set to 0 if you need
            // to avoid to calculate rotations each frame
            vertices[0].set(-wh, hh).add(position);
            vertices[1].set(-wh,-hh).add(position);
            vertices[2].set( wh,-hh).add(position);
            vertices[3].set( wh, hh).add(position);
        } else {
            final float sin = Math.sin(rotation);
            final float cos = Math.cos(rotation);
            final float sin_wh = sin * wh;
            final float sin_hh = sin * hh;
            final float cos_wh = cos * wh;
            final float cos_hh = cos * hh;
            vertices[0].x = /*First rotate*/-cos_wh - sin_hh /*then translate*/+ position.x;
            vertices[0].y = /*First rotate*/-sin_wh + cos_hh /*then translate*/+ position.y;
            vertices[1].x = /*First rotate*/-cos_wh + sin_hh /*then translate*/+ position.x;
            vertices[1].y = /*First rotate*/-sin_wh - cos_hh /*then translate*/+ position.y;
            vertices[2].x = /*First rotate*/ cos_wh + sin_hh /*then translate*/+ position.x;
            vertices[2].y = /*First rotate*/ sin_wh - cos_hh /*then translate*/+ position.y;
            vertices[3].x = /*First rotate*/ cos_wh - sin_hh /*then translate*/+ position.x;
            vertices[3].y = /*First rotate*/ sin_wh + cos_hh /*then translate*/+ position.y;
        }
    }
}
