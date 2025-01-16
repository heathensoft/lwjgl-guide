package io.github.heathensoft.jagfw.physics.old;


import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;


/**
 * Physics Body
 *
 * @author Frederik Dahl
 * 13/01/2025
 */


public class Body {
    
    public final Shape2 shape;
    
    // POSITIONAL FORCES
    
    public final Vector2f position_Last = new Vector2f();
    public final Vector2f position = new Vector2f();
    public final Vector2f velocity = new Vector2f();
    public final Vector2f acceleration = new Vector2f();
    public final Vector2f sum_forces = new Vector2f();
    
    // MASS AND MOMENT OF INERTIA
    /*
        Mass variables are protected.
        This is because the variables are
        dependent on each other.
        Arbitrarily changing one and not
        the others will lead to instability.
        
     */
    protected float mass;
    protected float mass_inv;
    protected float I; // moment of inertia
    protected float I_inv;
    
    
    // ROTATIONAL FORCES (TORQUE)
    /*
        rotation_last is the body rotation after
        the previous update. The last update is
        a value between [0, 2PI>
        
        rotation is the rotation after this update.
        the current rotation is NOT clamped.
        I.e. it can be < 0 and > 2PI.
        But it is set to position_last + delta rotation.
        The reason is that a body might rotate
        more than 2PI in a single update,
     */
    public float rotation_last;
    public float rotation;
    public float angular_vel;
    public float angular_acc;
    public float sum_torque;
    
    
    // RESTITUTION AND FRICTION
    /*
        Restitution is a value between 0 and 1
        0 is no elasticity (Bowling-ball like)
        1 is very elastic (Basketball like)
     */
    protected float restitution;
    protected float friction;
    
    /*
        if the body collided during the last update.
        Resets every update. It's for debugging
        purposes. Not essential for the engine.
        Todo: Set this during collision detection
     */
    public boolean colliding;
    
    
    public Body(Shape2 shape, float x, float y, float mass) {
        this.shape = shape;
        this.position.set(x,y);
        this.position_Last.set(position);
        this.restitution = 1f;
        this.friction = 0.5f;
        this.rotation = 0f;
        this.rotation_last = rotation;
        this.setMass(mass);
        if (shape instanceof Shape2.PolygonShape polygon) {
            polygon.updateVertices(position,rotation);
        }
    }
    
    
    /**
     * One step in the physics engine by dt.
     * Static bodies:
     *  are effectively cleared by this. As in, any leftover
     *  velocity, acceleration and forces are set to zero.
     * Dynamic bodies:
     *  are integrated, after which forces are cleared.
     * @param dt delta time
     */
    public void update(float dt) {
        position_Last.set(position);
        rotation_last = rotation % Math.PI_TIMES_2_f;
        if (rotation_last < 0) {
            rotation_last += Math.PI_TIMES_2_f;
        }
        if (isStatic()) {
            velocity.zero();
            acceleration.zero();
            angular_vel = 0f;
            angular_acc = 0f;
        } else  {
            integrateLinear(dt);
            integrateAngular(dt);
        }
        clearForces(); // Clear all the forces acting on the object before the next physics step
        clearTorque(); // Clear all the torque acting on the object before the next physics step
        if (shape instanceof Shape2.PolygonShape polygon) {
            polygon.updateVertices(position,rotation);
        }
        colliding = false; // collision reset
    }
    
    /**
     * Add a force to the body
     * @param force force
     */
    public void addForce(Vector2f force) {
        sum_forces.add(force);
    }
    
    /**
     * Add a force to the body
     * @param fx force in x
     * @param fy force in y
     */
    public void addForce(float fx, float fy) {
        sum_forces.add(fx,fy);
    }
    
    /**
     * Add rotational force to the body
     * @param torque torque
     */
    public void addTorque(float torque) {
        sum_torque += torque;
    }
    
    /**
     * All forces currently affecting the body is cleared
     */
    public void clearForces() {
        sum_forces.zero();
    }
    
    /**
     * Any leftover torque (rotational forces) is cleared
     */
    public void clearTorque() {
        sum_torque = 0;
    }
    
    /**
     * Impulse is an instantaneous change in velocity of the body,
     * inversely proportional to the mass.
     * Momentum P = m * v
     * Impulse J = dP = m * dv
     * Change in velocity is: dv = J / m
     * @param jx Impulse in x
     * @param jy Impulse in y
     */
    public void applyImpulse(float jx, float jy) {
        velocity.x += jx * mass_inv;
        velocity.y += jy * mass_inv;
    }
    
    /**
     * Impulse is an instantaneous change in velocity of the body,
     * inversely proportional to the mass.
     * Momentum P = m * v
     * Impulse J = dP = m * dv
     * Change in velocity is: dv = J / m
     * @param j Impulse
     */
    public void applyImpulse(Vector2f j) {
        velocity.x += j.x * mass_inv;
        velocity.y += j.y * mass_inv;
    }
    
    
    public void applyImpulse(Vector2f j, Vector2f r) {
        velocity.x += j.x * mass_inv;
        velocity.y += j.y * mass_inv;
        angular_vel += U.cross(r,j) * I_inv;
    }
    
    /**
     * Static bodies are bodies of "infinite mass"
     * They are unaffected by forces and torque applied
     * to them, and cannot collide with other static bodies
     * @return if the body is static
     */
    public boolean isStatic() {
        return mass == 0;
    }
    
    /**
     * @return Body Mass
     */
    public float mass() {
        return mass;
    }
    
    /**
     * @return (1 / Body Mass)
     */
    public float massInverse() {
        return mass_inv;
    }
    
    /**
     * Depends on mass and shape.
     * (How hard it is to get the body to move when not in motion)
     * @return The Body's Moment of inertia.
     */
    public float momentOfInertia() {
        return I;
    }
    
    /**
     * @return (1 / moment of inertia)
     */
    public float momentOfInertiaInverse() {
        return I_inv;
    }
    
    /**
     * Set the body mass.
     * Will affect mass and moment of inertia.
     * @param mass the Body mass
     */
    public void setMass(float mass) {
        this.mass = mass;
        this.mass_inv = mass == 0 ? 0 : 1 / mass;
        this.I = shape.momentOfInertia(mass);
        this.I_inv = I == 0 ? 0 : 1 / I;
    }
    
    /**
     * @return the elasticity of the body.
     * Value between 0 ("bowling ball") and 1 ("basket ball")
     */
    public float restitution() {
        return restitution;
    }
    
    /**
     * @param restitution elasticity clamped to range [0 - 1]
     */
    public void setRestitution(float restitution) {
        this.restitution = Math.max(Math.min(1,restitution),0);
    }
    
    public float friction() {
        return friction;
    }
    
    public void setFriction(float friction) {
        this.friction = Math.max(Math.min(1,friction),0);
    }
    
    /**
     * Can be < 0 or > 2PI
     * @return how much the body rotated this update
     */
    public float rotationDelta() {
        return rotation - rotation_last;
    }
    
    /**
     * Euler integration
     * @param dt delta time
     */
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
    
    /**
     * Integrate rotation
     * @param dt delta time
     */
    private void integrateAngular(float dt) {
        // Find the angular acceleration based on the torque that is being applied and the moment of inertia
        angular_acc = sum_torque * I_inv;
        // Integrate the angular acceleration to find the new angular velocity
        angular_vel += angular_acc * dt;
        // Integrate the angular velocity to find the new rotation angle
        rotation = rotation_last + angular_vel * dt;
    }
    
    
}
