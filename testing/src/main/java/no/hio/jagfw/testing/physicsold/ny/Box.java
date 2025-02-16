package no.hio.jagfw.testing.physicsold.ny;

import org.joml.Math;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/30/2025
 */
public class Box extends Polygon {
    /*
     * v0------v3
     * |        |
     * |        |
     * v1------v2
     */
    public final float width;
    public final float height;
    public final float radius;
    public final Vector2f[] vertices = new Vector2f[4];

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

    public Vector2f[] vertices() { return vertices; }
    public float boundingRadius() { return radius; }
    public float calculateMomentOfInertia(float mass) {
        // For a rectangle, the moment of inertia is 1/12 * (w^2 + h^2) * mass
        return (0.083333f) * (width * width + height * height) * mass;
    }

    public void update(Vector2f position, float rotation) {
        final float wh = width * 0.5f;
        final float hh = height * 0.5f;
        rotation %= Math.PI_TIMES_2_f;
        if (rotation < 0) rotation += Math.PI_TIMES_2_f;
        if (rotation == 0) {
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
