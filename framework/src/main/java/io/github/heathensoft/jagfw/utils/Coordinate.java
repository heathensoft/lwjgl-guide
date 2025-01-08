package io.github.heathensoft.jagfw.utils;

/**
 * Frederik Dahl 12/30/2024
 */
public class Coordinate {

    public int x, y;

    public Coordinate(int x, int y) { set(x,y);}

    public Coordinate() {
        set(0,0);
    }

    public Coordinate(Coordinate c) {
        set(c.x,c.y);
    }

    public void set(Coordinate c) {
        set(c.x, c.y);
    }

    public void set(int x, int y) { this.x = x; this.y = y; }

    public void sub(Coordinate c) {
        sub(c.x,c.y);
    }

    public void sub(int x, int y) {
        this.x -= x; this.y -= y;
    }

    public void add(Coordinate c) {
        add(c.x,c.y);
    }

    public void add(int x, int y) {
        this.x += x; this.y += y;
    }

    /** minimum discrete moves (diagonal allowed) */
    public int distance(Coordinate o) {
        return distance(o.x,o.y);
    }

    /** minimum discrete moves (diagonal allowed) */
    public int distance(int x, int y) {
        int xDist = Math.abs(this.x - x);
        int yDist = Math.abs(this.y - y);
        if (xDist < yDist)
            return xDist + (yDist - xDist);
        else return yDist + (xDist - yDist);
    }

    public int distanceX(Coordinate o) {
        return distanceX(o.x);
    }

    public int distanceX(int x) {
        return Math.abs(this.x - x);
    }

    public int distanceY(Coordinate o) {
        return distanceY(o.y);
    }

    public int distanceY(int y) {
        return Math.abs(this.y - y);
    }

    public boolean isAdjacentTo(Coordinate o) {
        return isAdjacentTo(o.x,o.y);
    }

    public boolean isAdjacentTo(int x, int y) {
        return Math.abs(this.x - x) < 2 && Math.abs(this.y - y) < 2;
    }

    public boolean equalsCoordinate(Coordinate o) { return o != null && this.x == o.x && this.y == o.y; }

    public boolean equalsCoordinate(int x, int y) {
        return this.x == x && this.y == y;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate)) return false;
        Coordinate c = (Coordinate)o;
        return this.x == c.x && this.y == c.y;
    }

    @Override
    public int hashCode () {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public String toString () {
        return "(" + x + ", " + y + ")";
    }

}
