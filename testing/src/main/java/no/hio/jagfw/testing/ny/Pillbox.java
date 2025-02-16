package no.hio.jagfw.testing.ny;

import io.github.heathensoft.jagfw.core.utils.LineSegment;
import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * PillBox is a "pill" shaped HurtBox.
 * It does not rotate.
 * Frederik Dahl 2/15/2025
 */
public class Pillbox extends Hitbox {
    /**
     * inner rectangle
     */
    public final Rectanglef box = new Rectanglef();
    /**
     * Width unscaled
     */
    public float width;
    /**
     * Height unscaled
     */
    public float height;
    /**
     * Radius of the circular edges
     */
    public float radius;

    public Pillbox(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void update(Vector2f position, float rotation, float scale) {
        final float x = position.x + offset.x;
        final float y = position.y + offset.y;
        if (width == height) {
            box.setMin(x,y);
            box.setMax(x,y);
            radius = width;
            return;
        }
        float wh, hh;
        if (width > height) {
            wh = (width - height) * scale * 0.5f;
            hh = height * scale * 0.5f;
            radius = hh;
        } else {
            wh = width * scale * 0.5f;
            hh = (height - width) * scale * 0.5f;
            radius = wh;
        }
        box.setMin(x - wh,y - hh);
        box.setMax(x + wh,y + hh);
    }

    public LineSegment edge0(LineSegment dst) {
        if (width > height) {
            // v1 -> v2
            dst.x0 = box.minX;
            dst.y0 = box.minY;
            dst.x1 = box.maxX;
            dst.y1 = box.minY;
        } else { // v0 -> v1
            dst.x0 = box.minX;
            dst.y0 = box.maxY;
            dst.x1 = box.minX;
            dst.y1 = box.minY;
        } return dst;
    }

    public LineSegment edge1(LineSegment dst) {
        if (width > height) {
            // v3 -> v0
            dst.x0 = box.maxX;
            dst.y0 = box.maxY;
            dst.x1 = box.minX;
            dst.y1 = box.maxY;
        } else { // v2 -> v3
            dst.x0 = box.maxX;
            dst.y0 = box.minY;
            dst.x1 = box.maxX;
            dst.y1 = box.maxY;
        } return dst;
    }

    public Vector2f circleCenter0(Vector2f dst) {
        if (width < height) {
            dst.x = (box.minX + box.maxX) / 2f;
            dst.y = box.minY;
        } else {
            dst.x = box.minX;
            dst.y = (box.minY + box.maxY) / 2f;
        } return dst;
    }

    public Vector2f circleCenter1(Vector2f dst) {
        if (width < height) {
            dst.x = (box.minX + box.maxX) / 2f;
            dst.y = box.maxY;
        } else {
            dst.x = box.maxX;
            dst.y = (box.minY + box.maxY) / 2f;
        } return dst;
    }

    public boolean contains(float x, float y) {
        if (isValid()) {
            if (isCircle()) {
                return U.testPointCircle(x,y,box.minX,box.minY,radius);
            } if (box.containsPoint(x,y)) return true;
            Vector2f c = circleCenter1(U.popVec2());
            if (U.testPointCircle(x,y,c.x,c.y,radius)) {
                U.pushVec2();
                return true;
            } circleCenter0(c);
            U.pushVec2();
            return U.testPointCircle(x,y,c.x,c.y,radius);
        } return false;
    }

    public boolean isValid() {
        return radius > 0;
    }

    public boolean isCircle() {
        return width == height;
    }


}
