package io.github.heathensoft.jagfw.physics.ny;

import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 1/30/2025
 */
public class Circle extends Shape {

    public final Vector2f position = new Vector2f();
    public final float radius;

    public Circle(float x, float y, float r) {
        this.position.set(x,y);
        this.radius = r;
    }

    public Circle(Vector2f center, float radius) {
        this(center.x, center.y, radius);
    }

    public Circle(float radius) {
        this.radius = radius;
    }

    public Circle() {
        this(0.5f);
    }

    public float calculateMomentOfInertia(float mass) {
        // For solid circles, the moment of inertia is 1/2 * r^2 * mass
        return 0.5f * (radius * radius) * mass;
    }

    public void update(Vector2f position, float rotation) {
        this.position.set(position);
    }

    public float boundingRadius() {
        return radius;
    }

    public Vector2f center() {
        return position;
    }

    public Rectanglef calculateBoundingBox() {
        final float rh = radius * 0.5f;
        return new Rectanglef(
                position.x - rh,
                position.y - rh,
                position.x + rh,
                position.y + rh
        );
    }

    public void translate(float x, float y) {
        position.add(x,y);
    }
}
