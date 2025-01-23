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
    public final Vector2f position_prev = new Vector2f();
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
    protected float rotation_prev;
    protected float angular_vel;
    protected float angular_acc;
    protected float sum_torque;
    // RESTITUTION AND FRICTION
    protected float restitution;
    protected float friction;
    public float angular_damping;
    public float linear_damping;
    public boolean rotatable;
    public boolean colliding;
    public boolean sleep; // used by world


    public RigidBody(Shape shape, float mass, float x, float y) {
        this.position.set(x,y);
        this.position_prev.set(x,y);
        this.velocity.zero();
        this.acceleration.zero();
        this.sum_forces.zero();
        this.rotation = 0.0f;
        this.rotation_prev = 0.0f;
        this.angular_acc = 0.0f;
        this.angular_vel = 0.0f;
        this.sum_torque = 0.0f;
        this.shape = shape;
        this.mass = mass;
        this.mass_inv = mass == 0 ? 0 : 1 / mass;
        this.I = shape.momentOfInertia(mass);
        this.I_inv = I == 0 ? 0 : 1 / I;
        this.shape.updateVertices(position,rotation);
        this.linear_damping = 1.0f;
        this.angular_damping = 1.0f;
        this.restitution = 0.5f;
        this.friction = 0.5f;
        this.rotatable = true;
        this.colliding = false;
        this.sleep = false;
    }

    public void update(float dt) {
        colliding = false;
        boolean moved_manually = true;
        boolean moved_this_frame = true;
        if (rotation == rotation_prev) {
            if (position.x == position_prev.x) {
                if (position.y == position_prev.y) {
                    moved_manually = false;
                }
            }
        }
        position_prev.set(position);
        rotation_prev = rotation % Math.PI_TIMES_2_f;
        if (rotation_prev < 0) {
            rotation_prev += Math.PI_TIMES_2_f;
        } if (mass == 0) {
            velocity.zero();
            acceleration.zero();
            angular_vel = 0f;
            angular_acc = 0f;
        } else  {
            // Linear integration
            // Find the acceleration based on the
            // forces that are being applied and the mass
            acceleration.set(sum_forces).mul(mass_inv);
            // Integrate the acceleration to find the new velocity
            velocity.x += acceleration.x * dt;
            velocity.y += acceleration.y * dt;
            // damping
            velocity.x *= 1.0f / (1.0f + linear_damping * dt);
            velocity.y *= 1.0f / (1.0f + linear_damping * dt);
            // Integrate the velocity to find the new position
            position.x = position_prev.x + velocity.x * dt;
            position.y = position_prev.y + velocity.y * dt;
            if (rotatable) {
                // Angular integration
                // Find the angular acceleration based on the torque
                // that is being applied and the moment of inertia
                angular_acc = sum_torque * I_inv;
                // Integrate the angular acceleration
                // to find the new angular velocity
                angular_vel += angular_acc * dt;
                // damping
                angular_vel *= 1.0f / (1.0f + angular_damping * dt);
                // Integrate the angular velocity
                // to find the new rotation angle
                rotation = rotation_prev + angular_vel * dt;
            } else {
                angular_vel = 0f;
                angular_acc = 0f;
            }
        }
        sum_forces.zero();
        sum_torque = 0f;
        if (rotation == rotation_prev) {
            if (position.x == position_prev.x) {
                if (position.y == position_prev.y) {
                    moved_this_frame = false;
                }
            }
        }
        if (moved_this_frame || moved_manually) {
            updateVertices();
        }
    }

    public void updateVertices() {
        shape.updateVertices(position,rotation);
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
        return rotation - rotation_prev;
    }
    public float rotation() { return rotation; }
    public void setRotation(float rotation) {
        rotation = rotation % Math.PI_TIMES_2_f;
        if (rotation < 0) rotation += Math.PI_TIMES_2_f;
        this.rotation = rotation;
    }

    // ********************************************************************************************
    // POSITION

    /**
     * Get position at a specific time of the frame.
     * for t = 0 returns the position before the last call to update.
     * for t = 1 returns the current position.
     * @param t value between 0 and 1
     * @param dst the resulting position
     * @return dst
     */
    public Vector2f interpolatedPosition(float t, Vector2f dst) {
        return U.lerp(position_prev,position,t,dst);
    }
    public Vector2f position() { return position; }
    public Vector2f velocity() { return velocity; }
    public Vector2f acceleration() { return acceleration; }
}
