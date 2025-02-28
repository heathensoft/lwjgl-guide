package io.github.heathensoft.jagfw.core.utils;

import org.joml.Vector2f;
import org.joml.primitives.Intersectionf;
import org.joml.primitives.Rectanglef;

import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class QuadTree<E> {

    private static final int NW = 0;
    private static final int NE = 1;
    private static final int SW = 2;
    private static final int SE = 3;
    private static final int CAP = 5;

    private record Point<E>(float x, float y, E e) { /* */ }
    private record Circle(float x, float y, float r2) { /* */ }
    private final Point<E>[] points;
    private final float x0, y0, s;
    private QuadTree<E>[] regions;
    private int idx;

    @SuppressWarnings("unchecked")
    public QuadTree(float x0, float y0, float size) {
        this.points = new Point[CAP];
        this.s = size;
        this.x0 = x0;
        this.y0 = y0;
    }

    public void insert(E e, Vector2f v) {
        insert(new Point<>(v.x, v.y, e));
    }

    public void insert(E e, float x, float y) {
        insert(new Point<>(x, y, e));
    }

    public int query(float x, float y, float radius) {
        if (radius > 0) return queryC(new Circle(x,y,radius * radius));
        return 0;
    }

    public void query(List<E> list, float x, float y, float radius) {
        if (radius > 0) queryC(list,new Circle(x,y,radius * radius));
    }

    public void query(List<E> list, Rectanglef rect) {
        if (rect.isValid()) queryR(list,rect);
    }

    private int queryC(Circle circle) {
        int count = 0;
        if (overlapsC(circle)) {
            int l = Math.min(idx,CAP);
            for (int i = 0; i < l; i++) {
                Point<E> p = points[i];
                final float dx = p.x - circle.x;
                final float dy = p.y - circle.y;
                final float d2 = dx * dx + dy * dy;
                if (d2 < circle.r2) count++;
            } if (idx > CAP) { // if split
                count += regions[NW].queryC(circle);
                count += regions[NE].queryC(circle);
                count += regions[SW].queryC(circle);
                count += regions[SE].queryC(circle);
            }
        } return count;
    }

    private void queryC(List<E> list, Circle circle) {
        if (overlapsC(circle)) {
            int l = Math.min(idx,CAP);
            for (int i = 0; i < l; i++) {
                Point<E> p = points[i];
                final float dx = p.x - circle.x;
                final float dy = p.y - circle.y;
                final float d2 = dx * dx + dy * dy;
                if (d2 < circle.r2) list.add(p.e);
            } if (idx > CAP) { // if split
                regions[NW].queryC(list,circle);
                regions[NE].queryC(list,circle);
                regions[SW].queryC(list,circle);
                regions[SE].queryC(list,circle);
            }
        }
    }

    private void queryR(List<E> list, Rectanglef rect) {
        if (overlapsR(rect)) {
            int l = Math.min(idx,CAP);
            for (int i = 0; i < l; i++) {
                Point<E> p = points[i];
                if (rect.containsPoint(p.x,p.y))
                    list.add(p.e);
            } if (idx > CAP) { // if split
                regions[NW].queryR(list,rect);
                regions[NE].queryR(list,rect);
                regions[SW].queryR(list,rect);
                regions[SE].queryR(list,rect);
            }
        }
    }


    public void clear() {
        for (int i = 0; i < CAP; i++) {
            points[i] = null;
        } if (idx > CAP) {
            regions[NW].clear();
            regions[NE].clear();
            regions[SW].clear();
            regions[SE].clear();
            regions[NW] = null;
            regions[NE] = null;
            regions[SW] = null;
            regions[SE] = null;
        } idx = 0;
    }

    private void insert(Point<E> p) {
        if (contains(p.x,p.y)) {
            if (idx < CAP) points[idx++] = p;
            else {
                if (idx == CAP){
                    split();
                    idx++;
                }
                regions[NW].insert(p);
                regions[NE].insert(p);
                regions[SW].insert(p);
                regions[SE].insert(p);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void split() {
        final float s = this.s / 2;
        final float x1 = x0 + s;
        final float y1 = y0 + s;
        regions = new QuadTree[4];
        regions[SW] = new QuadTree<>(x0, y0, s);
        regions[SE] = new QuadTree<>(x1, y0, s);
        regions[NW] = new QuadTree<>(x0, y1, s);
        regions[NE] = new QuadTree<>(x1, y1, s);
    }

    private boolean overlapsR(Rectanglef r) {
        final float x = x0 + s;
        final float y = y0 + s;
        return Intersectionf.testAarAar(x0,y0,x,y,r.minX,r.minY,r.maxX,r.maxY);
    }

    private boolean overlapsC(Circle c) {
        final float x = x0 + s;
        final float y = y0 + s;
        return Intersectionf.testAarCircle(x0,y0,x,y,c.x,c.y,c.r2);
    }

    private boolean contains(float x, float y) {
        return x0 <= x && x0 + s > x && y0 <= y && y0 + s > y;
    }

}
