package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;


/**
 * Frederik Dahl 1/16/2025
 */
public class RigidBody {

    public final Shape shape;
    // POSITIONAL FORCES
    public final Vector2f position = new Vector2f();
    public final Vector2f position_Last = new Vector2f();
    public final Vector2f velocity = new Vector2f();
    public final Vector2f acceleration = new Vector2f();
    public final Vector2f sum_forces = new Vector2f();
    // MASS AND MOMENT OF INERTIA
    protected float mass;
    protected float mass_inv;
    protected float I;
    protected float I_inv;
    // ROTATIONAL FORCES (TORQUE)
    protected float rotation;
    protected float rotation_last;
    protected float angular_vel;
    protected float angular_acc;
    protected float sum_torque;
    // RESTITUTION AND FRICTION
    protected float restitution;
    protected float friction;
    public boolean colliding;
    public boolean moved;
    public boolean sleep; // used by world


    public RigidBody(Shape shape, float mass, float x, float y) {
        this.position.set(x,y);
        this.position_Last.set(x,y);
        this.velocity.zero();
        this.acceleration.zero();
        this.sum_forces.zero();
        this.rotation = 0.0f;
        this.rotation_last = 0.0f;
        this.angular_acc = 0.0f;
        this.angular_vel = 0.0f;
        this.sum_torque = 0.0f;

        this.shape = shape;
        this.mass = mass;
        this.mass_inv = mass == 0 ? 0 : 1 / mass;
        this.I = shape.momentOfInertia(mass);
        this.I_inv = I == 0 ? 0 : 1 / I;
        this.shape.updateVertices(position,rotation);

        this.restitution = 0.5f;
        this.friction = 0.5f;
        this.colliding = false;
        this.moved = false;
        this.sleep = false;
    }

    public void update(float dt) {

        boolean body_moved_manually = bodyMoved();
        colliding = false;
        position_Last.set(position);
        rotation_last = rotation % Math.PI_TIMES_2_f;
        if (rotation_last < 0) rotation_last += Math.PI_TIMES_2_f;


        if (isStatic()) {
            velocity.zero();
            acceleration.zero();
            angular_vel = 0f;
            angular_acc = 0f;
        } else  {
            integrateLinear(dt);
            integrateAngular(dt);


        }
        sum_torque = 0;
        sum_forces.zero();

        moved = bodyMoved();

        if (moved || body_moved_manually) {
            shape.updateVertices(position,rotation);
        }

    }

    private void integrateLinear(float dt) {
        // Find the acceleration based on the forces that are being applied and the mass
        acceleration.set(sum_forces).mul(mass_inv);
        // Integrate the acceleration to find the new velocity
        velocity.x += acceleration.x * dt;
        velocity.y += acceleration.y * dt;
        // Integrate the velocity to find the new position
        position.x += velocity.x * dt;
        position.y += velocity.y * dt;
    }

    private void integrateAngular(float dt) {
        // Find the angular acceleration based on the torque that is being applied and the moment of inertia
        angular_acc = sum_torque * I_inv;
        // Integrate the angular acceleration to find the new angular velocity
        angular_vel += angular_acc * dt;
        // Integrate the angular velocity to find the new rotation angle
        rotation = rotation_last + angular_vel * dt;
    }

    private boolean bodyMoved() {
        if (rotation_last == rotation) {
            if (position.x == position_Last.x)
                return position.y != position_Last.y;
        } return true;
    }

    // ********************************************************************************************
    // FORCES AND IMPULSES
    public void addForce(Vector2f force) {
        sum_forces.add(force);
    }
    public void addForce(float fx, float fy) {
        sum_forces.add(fx,fy);
    }
    public void addTorque(float torque) {
        sum_torque += torque;
    }
    public void clearForces() {
        sum_forces.zero();
    }
    public void clearTorque() {
        sum_torque = 0;
    }
    public void applyImpulse(float jx, float jy) {
        velocity.x += jx * mass_inv;
        velocity.y += jy * mass_inv;
    }
    public void applyImpulse(Vector2f j) {
        velocity.x += j.x * mass_inv;
        velocity.y += j.y * mass_inv;
    }

    public void applyImpulse(Vector2f j, Vector2f r) {
        velocity.x += j.x * mass_inv;
        velocity.y += j.y * mass_inv;
        angular_vel += U.cross(r,j) * I_inv;
    }
    // ********************************************************************************************
    // MASS AND MOMENT OF INERTIA
    /**
     * Static bodies are bodies of "infinite mass"
     * They are unaffected by forces and torque applied
     * to them, and cannot collide with other static bodies
     * @return if the body is static
     */
    public boolean isStatic() {
        return mass == 0;
    }
    public float mass() {
        return mass;
    }
    public float massInverse() {
        return mass_inv;
    }
    public float momentOfInertia() {
        return I;
    }
    public float momentOfInertiaInverse() {
        return I_inv;
    }
    public void setMass(float mass) {
        this.mass = mass;
        this.mass_inv = mass == 0 ? 0 : 1 / mass;
        this.I = shape.momentOfInertia(mass);
        this.I_inv = I == 0 ? 0 : 1 / I;
    }
    // ********************************************************************************************
    // RESTITUTION AND FRICTION
    public float restitution() {
        return restitution;
    }
    public void setRestitution(float restitution) { this.restitution = U.clamp(restitution); }
    public float friction() {
        return friction;
    }
    public void setFriction(float friction) { this.friction = U.clamp(friction); }

    // ********************************************************************************************
    // ROTATION
    /**
     * Can be < 0 or > 2PI
     * @return how much the body rotated this update
     */
    public float rotationDelta() {
        return rotation - rotation_last;
    }
    public float rotation() { return rotation; }
    public void setRotation(float rotation) {
        rotation = rotation % Math.PI_TIMES_2_f;
        if (rotation < 0) rotation += Math.PI_TIMES_2_f;
        this.rotation = rotation;
    }
}
