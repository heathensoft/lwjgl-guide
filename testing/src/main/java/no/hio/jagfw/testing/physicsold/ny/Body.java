package no.hio.jagfw.testing.physicsold.ny;

import io.github.heathensoft.jagfw.core.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/30/2025
 */
public class Body {

    public static final float DEFAULT_ROTATION = 0f;
    public static final float DEFAULT_LINEAR_DAMPING = 1.0f;
    public static final float DEFAULT_ANGULAR_DAMPING = 1.0f;
    public static final float DEFAULT_RESTITUTION = 0.5f;
    public static final float DEFAULT_FRICTION = 0.25f;

    public Shape shape;
    public final Vector2f position_previous = new Vector2f();
    public final Vector2f position = new Vector2f();
    public final Vector2f velocity = new Vector2f();
    public final Vector2f acceleration = new Vector2f();
    public final Vector2f sum_forces = new Vector2f();
    public float rotation = DEFAULT_ROTATION;
    public float rotation_previous = DEFAULT_ROTATION;
    public float angular_velocity;
    public float angular_acceleration;
    public float sum_torque;
    public float mass;
    public float mass_inverse;
    public float moi; // moment of inertia
    public float moi_inverse;
    public float linear_damping;
    public float angular_damping;
    public float restitution;
    public float friction;
    public boolean sleeping;
    public boolean rotatable;

    public Body(Shape shape, float mass, float x, float y) {
        reset(shape,mass, x,y);
    }

    public void reset(Shape shape, float mass, float x, float y, float restitution,
        float friction, float linear_damping, float angular_damping) {
        if (shape == null) throw new IllegalStateException("null shape");
        this.position.x = x;
        this.position.y = y;
        this.position_previous.x = x;
        this.position_previous.y = y;
        this.velocity.x = 0f;
        this.velocity.y = 0f;
        this.acceleration.x = 0f;
        this.acceleration.y = 0f;
        this.sum_forces.x = 0;
        this.sum_forces.y = 0f;
        this.rotation = 0;
        this.rotation_previous = 0f;
        this.sum_torque = 0f;
        this.angular_velocity = 0f;
        this.angular_acceleration = 0f;
        this.friction = U.clamp(friction);
        this.restitution = U.clamp(restitution);
        this.linear_damping = linear_damping;
        this.angular_damping = angular_damping;
        this.mass = mass;
        this.mass_inverse = mass == 0 ? 0 : 1 / mass;
        this.moi = shape.calculateMomentOfInertia(mass);
        this.moi_inverse = moi == 0 ? 0 : 1 / moi;
        this.shape = shape;
        this.shape.update(position, rotation);
        this.rotatable = true;
        this.sleeping = false;
    }

    public void reset(Shape shape, float mass, float x, float y) {
        reset(shape,mass,x,y,DEFAULT_RESTITUTION,DEFAULT_FRICTION,
                DEFAULT_LINEAR_DAMPING,DEFAULT_ANGULAR_DAMPING);
    }

    public void reset(float mass, float x, float y) {
        reset(shape,mass,x,y);
    }

    public void update(float dt) {
        position_previous.set(position);
        rotation_previous = rotation % Math.PI_TIMES_2_f;
        if (rotation_previous < 0) {
            rotation_previous += Math.PI_TIMES_2_f;
        } if (isStatic()) {
            velocity.x = 0f;
            velocity.y = 0f;
            acceleration.x = 0f;
            acceleration.y = 0f;
            angular_velocity = 0f;
            angular_acceleration = 0f;
        } else { integrateLinear(dt);
            if (rotatable) {
                integrateAngular(dt);
            } else {
                angular_velocity = 0f;
                angular_acceleration = 0f;
            }
        }
        sum_torque = 0f;
        sum_forces.x = 0f;
        sum_forces.y = 0f;
        shape.update(position, rotation);
    }

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
        velocity.x += jx * mass_inverse;
        velocity.y += jy * mass_inverse;
    }

    public void applyImpulse(Vector2f j) {
        velocity.x += j.x * mass_inverse;
        velocity.y += j.y * mass_inverse;
    }

    public void applyImpulse(Vector2f j, Vector2f r) {
        velocity.x += j.x * mass_inverse;
        velocity.y += j.y * mass_inverse;
        angular_velocity += U.cross(r,j) * moi_inverse;
    }

    public void setMass(float mass) {
        this.mass = mass;
        this.mass_inverse = mass == 0 ? 0 : 1 / mass;
        this.moi = shape.calculateMomentOfInertia(mass);
        this.moi_inverse = moi == 0 ? 0 : 1 / moi;
    }

    public void setRestitution(float restitution) {
        this.restitution = U.clamp(restitution);
    }

    public void setFriction(float friction) {
        this.friction = U.clamp(friction);
    }

    public void setRotation(float rotation) {
        rotation = rotation % Math.PI_TIMES_2_f;
        if (rotation < 0) rotation += Math.PI_TIMES_2_f;
        this.rotation = rotation;
    }

    public float interpolateRotation(float t) {
        return U.lerp(rotation_previous, rotation,t);
    }

    public float rotationDelta() {
        return rotation - rotation_previous;
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

    public Vector2f positionDelta(Vector2f dst) {
        return dst.set(position).sub(position_previous);
    }

    public Vector2f positionDelta() {
        return positionDelta(new Vector2f());
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

    protected void integrateLinear(float dt) {
        // Find the acceleration based on the
        // forces that are being applied and the mass
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
    }

    protected void integrateAngular(float dt) {
        // Angular integration
        // Find the angular acceleration based on the torque
        // that is being applied and the moment of inertia
        angular_acceleration = sum_torque * moi_inverse;
        // Integrate the angular acceleration
        // to find the new angular velocity
        angular_velocity += angular_acceleration * dt;
        // angular damping ("feel" value)
        angular_velocity *= 1.0f / (1.0f + angular_damping * dt);
        // Integrate the angular velocity to find the new rotation angle
        rotation = rotation_previous + angular_velocity * dt;
    }

}
