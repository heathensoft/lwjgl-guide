package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.LineSegment;
import org.joml.Math;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/14/2025
 */
public class HurtBox {
    /* v0------v3
     * |        |
     * |        |
     * v1------v2 */
    public final Vector2f[] vertices = new Vector2f[] {
            new Vector2f(), /* v0 */
            new Vector2f(), /* v1 */
            new Vector2f(), /* v2 */
            new Vector2f()  /* v3 */
    }; public final Vector2f offset = new Vector2f();
    public boolean rounded;
    public float width;
    public float height;
    public float scale;
    public float offset_rotation;

    public HurtBox(int width, int height, boolean rounded) {
        this(width,height,1,rounded);
    }

    public HurtBox(int width, int height, float scale, boolean rounded) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.rounded = rounded;
    }

    public void update(Vector2f position, float rotation) {
        float wh; // width half
        float hh; // height half
        if (rounded) {
            if (width > height) {
                wh = (width - height) * scale * 0.5f;
                hh = height * scale * 0.5f;
            } else if (width < height) {
                wh = width * scale * 0.5f;
                hh = (height - width) * scale * 0.5f;
            } else {
                wh = 0;
                hh = 0;
            }
        } else {
            wh = width * scale * 0.5f;
            hh = height * scale * 0.5f;
        }
        float translation_x = position.x + offset.x;
        float translation_y = position.y + offset.y;
        float rot = (rotation + offset_rotation);
        while (rot < 0) rot += Math.PI_TIMES_2_f;
        rot %= Math.PI_TIMES_2_f;
        if (rot == 0 || (wh == 0 && hh == 0)) {
            vertices[0].set(-wh, hh).add(translation_x,translation_y);
            vertices[1].set(-wh,-hh).add(translation_x,translation_y);
            vertices[2].set( wh,-hh).add(translation_x,translation_y);
            vertices[3].set( wh, hh).add(translation_x,translation_y);
        } else {
            final float sin = Math.sin(rot);
            final float cos = Math.cos(rot);
            final float sin_wh = sin * wh;
            final float sin_hh = sin * hh;
            final float cos_wh = cos * wh;
            final float cos_hh = cos * hh;
            vertices[0].x = /*First rotate*/-cos_wh - sin_hh /*then translate*/+ translation_x;
            vertices[0].y = /*First rotate*/-sin_wh + cos_hh /*then translate*/+ translation_y;
            vertices[1].x = /*First rotate*/-cos_wh + sin_hh /*then translate*/+ translation_x;
            vertices[1].y = /*First rotate*/-sin_wh - cos_hh /*then translate*/+ translation_y;
            vertices[2].x = /*First rotate*/ cos_wh + sin_hh /*then translate*/+ translation_x;
            vertices[2].y = /*First rotate*/ sin_wh - cos_hh /*then translate*/+ translation_y;
            vertices[3].x = /*First rotate*/ cos_wh - sin_hh /*then translate*/+ translation_x;
            vertices[3].y = /*First rotate*/ sin_wh + cos_hh /*then translate*/+ translation_y;
        }
    }

    public float circleCollidersRadius() {
        // Remember to call isValid first
        if (width < height) {
            return width * scale * 0.5f;
        } else return height * scale * 0.5f;
    }

    public boolean isActiveColliderEdge(int edge) {
        // Remember to call isValid first
        /* v0------v3
         * |        |
         * |        |
         * v1------v2 */
        if (rounded) {
            if (width != height) {
                // not a single circle
                if (width > height) {
                    // v1 -> v2 is active (1)
                    // v3 -> v0 is active (3)
                    return edge == 1 || edge == 3;
                } else {
                    // v0 -> v1 is active (0)
                    // v2 -> v3 is active (2)
                    return edge == 0 || edge == 2;}
            } return false;
        } return true;
    }

    public LineSegment colliderEdge(int edge, LineSegment dst) {
        Vector2f v0 = vertices[edge % 4];
        Vector2f v1 = vertices[(edge + 1) % 4];
        dst.set(v0,v1);
        return dst;
    }

    public Vector2f circleColliderCenter(int edge, Vector2f dst) {
        Vector2f v0 = vertices[edge % 4];
        Vector2f v1 = vertices[(edge + 1) % 4];
        return dst.set(v0).add(v1).mul(0.5f);
    }

    public boolean isValid() {
        return width > 0 && height > 0 && scale > 0;
    }

    public boolean isCircle() {
        return rounded && width == height;
    }

    public boolean isRect() {
        return !rounded;
    }
}
