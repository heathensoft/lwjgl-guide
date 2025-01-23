package io.github.heathensoft.jagfw.physics.ny.shape;

/**
 * Circle Shape
 * Frederik Dahl 1/23/2025
 */
public class Circle extends Shape {
    /** Construct circle with radius = 0.5 */
    public Circle() { this(0.5f); }
    /** @param radius radius */
    public Circle(float radius) { this.radius = radius; }
    public float calculateMomentOfInertia(float mass) {
        // For solid circles, the moment of inertia is 1/2 * r^2 * mass
        return 0.5f * (radius * radius) * mass;
    }
}
