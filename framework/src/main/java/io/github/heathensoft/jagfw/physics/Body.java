package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Vector2f;

/**
 * Circle Collider for Entities in Top-Down style games.
 * Frederik Dahl 2/10/2025
 */
public class Body {


    public static final float DEFAULT_LINEAR_DAMPING = 2.0f;
    public static final float DEFAULT_RESTITUTION = 0.25f;
    public static final float DEFAULT_FRICTION = 0.15f;
    public static final Vector2f DEFAULT_DIRECTION = new Vector2f(0,-1);

    /**
     * Previous position of the Body (previous update).
     */
    public final Vector2f position_previous = new Vector2f();
    /**
     * Current position of the Body
     */
    public final Vector2f position = new Vector2f();
    /**
     * Current velocity of the Body
     */
    public final Vector2f velocity = new Vector2f();
    /**
     * Current acceleration of the Body
     */
    public final Vector2f acceleration = new Vector2f();
    /**
     * Sum of all the forces acting on the Body.
     * Resets every update
     */
    public final Vector2f sum_forces = new Vector2f();
    /**
     * Sum of all "controlled forces" these are also added to sum_forces.
     * The difference is that controlled forces are the only forces to determine the body facing direction.
     * Example: Player movement is a "controlled" force, wind or friction etc. are not.
     */
    public final Vector2f controlled_forces = new Vector2f();
    /**
     * This is the direction of the acceleration.
     * If the acceleration is zero then the direction is set to previous direction
     * Default direction is "downwards" (-y)
     */
    public final Vector2f facing_direction = new Vector2f();
    /**
     * Inverse of Body mass in kilograms
     */
    public float mass_inverse;
    /**
     * Radius of Body collider.
     * for more complicated physics, this would be replaced with a shape.
     */
    public float radius;
    /**
     * Friction applies when Bodies (Or geometry) collide.
     * Value between 0 and 1
     */
    public float friction;
    /**
     * "Bouncy-ness"
     * Value between 0 and 1
     */
    public float restitution;
    /**
     * "dampens" linear velocity.
     */
    public float linear_damping;


    public Body(float x, float y, float radius, float mass) {
        reset(x, y, radius, mass, DEFAULT_FRICTION, DEFAULT_RESTITUTION, DEFAULT_LINEAR_DAMPING);
    }

    public Body(float x, float y, float radius, float mass, float friction, float restitution, float linear_damping) {
        reset(x, y, radius, mass, friction, restitution, linear_damping);
    }

    public void reset(float x, float y, float radius, float mass, float friction, float restitution, float linear_damping) {
        this.position.set(x,y);
        this.position_previous.set(x,y);
        this.facing_direction.set(DEFAULT_DIRECTION);
        this.velocity.zero();
        this.acceleration.zero();
        this.sum_forces.zero();
        this.controlled_forces.zero();
        this.mass_inverse = mass == 0 ? 0 : 1 / mass;
        this.radius = radius;
        this.friction = friction;
        this.restitution = restitution;
        this.linear_damping = linear_damping;
    }

    public void update(float dt) {
        position_previous.set(position);
        if (mass_inverse == 0) {
            velocity.zero();
            acceleration.zero();
        } else { // Find the acceleration based on the
            // forces that are being applied and the mass
            if (controlled_forces.lengthSquared() > 1) {
                facing_direction.set(controlled_forces).normalize();
            } sum_forces.add(controlled_forces);
            acceleration.set(sum_forces).mul(mass_inverse);
            // Integrate the acceleration to find the new velocity
            velocity.x += acceleration.x * dt;
            velocity.y += acceleration.y * dt;
            // linear damping
            velocity.x *= 1.0f / (1.0f + linear_damping * dt);
            velocity.y *= 1.0f / (1.0f + linear_damping * dt);
            // Integrate the velocity to find the new position
            position.x = position_previous.x + velocity.x * dt;
            position.y = position_previous.y + velocity.y * dt;
        } controlled_forces.zero();
        sum_forces.zero();
    }

    public void addForce(Vector2f force) {
        sum_forces.add(force);
    }

    public void addForce(float fx, float fy) {
        sum_forces.add(fx,fy);
    }

    /** This is the same as the regular add force, but only controlled forces affects the body direction*/
    public void addForceControlled(Vector2f force) { controlled_forces.add(force); }

    /** This is the same as the regular add force, but only controlled forces affects the body direction*/
    public void addForceControlled(float fx, float fy) { controlled_forces.add(fx,fy); }

    public void clearForces() {
        sum_forces.zero();
    }

    public void applyImpulse(float jx, float jy) {
        velocity.x += jx * mass_inverse;
        velocity.y += jy * mass_inverse;
    }

    public void applyImpulse(Vector2f j) {
        velocity.x += j.x * mass_inverse;
        velocity.y += j.y * mass_inverse;
    }

    public boolean isStatic() {
        return mass_inverse == 0;
    }

    public void setMass(float mass) {
        mass_inverse = mass == 0 ? 0 : 1 / mass;
    }

    public float mass() {
        return mass_inverse == 0 ? 0 : 1 / mass_inverse;
    }

    /**
     * Get position at a specific time of the frame.
     * for t = 0 returns the position before the last call to update.
     * for t = 1 returns the current position.
     * @param t value between 0 and 1
     * @param dst the resulting position
     * @return dst
     */
    public Vector2f interpolatedPosition(float t, Vector2f dst) {
        return U.lerp(position_previous, position,t,dst);
    }

    public Vector2f interpolatedPosition(float t) {
        return interpolatedPosition(t, new Vector2f());
    }



}
