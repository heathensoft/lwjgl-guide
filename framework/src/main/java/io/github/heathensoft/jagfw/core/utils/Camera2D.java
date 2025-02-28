package io.github.heathensoft.jagfw.core.utils;

import io.github.heathensoft.jagfw.core.Resolution;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.primitives.Rectanglef;

/**
 * Orthographic Camera for 2D environments
 * Frederik Dahl 1/11/2025
 */
public class Camera2D {

    public static final float NEAR = 1.0f;          // near plane = pos + (dir * near)
    public static final float FAR = 2.0f;           // far plane = pos + (dir * far)
    public static final float POS_Z = 1.0f;         // pos locked in at z == 1.0
    public static final float DIR_Z = -1.0f;        // facing directly towards -z
    public static final float UP_X = 0.0f;          // up direction is the y-axis
    public static final float UP_Y = 1.0f;          // up direction is the y-axis
    public static final float UP_Z = 0.0f;          // up direction is the y-axis

    public final Matrix4f view = new Matrix4f();            // view matrix (affected by position)
    public final Matrix4f projection = new Matrix4f();      // projection matrix (affected by viewport)
    public final Matrix4f combined = new Matrix4f();        // view projection matrix (combined)
    public final Matrix4f combined_inv = new Matrix4f();    // inverse combined matrix
    public final Vector2f position  = new Vector2f(0,0); // camera position (eye)
    public final Vector2f viewport  = new Vector2f(1,1); // size of visible area in world units (for zoom == 1)
    public final Rectanglef frustum = new Rectanglef(); // visible world area

    private float follow_velocity_x = 0.0f;
    private float follow_velocity_y = 0.0f;
    public float follow_bias_term = 0.5f; // 0 - 1
    public float follow_damping = 0.1f;   // 0 - 1

    // zoom (used to expand or contract the frustum, making the scene appear smaller / larger)
    private float zoom = 0.0f;
    private float zoom_velocity = 0.0f;
    public float zoom_bias_term = 0.5f;
    public float zoom_damping = 0.1f;
    public float zoom_min = -2.0f;
    public float zoom_max = 3.0f;

    public Camera2D(Resolution resolution) {
        viewport.set(resolution.width(),resolution.height());
        position.set(viewport).div(2);
        refresh();
    }

    public Camera2D(float aspect_ratio, float width_in_tiles) {
        viewport.set(width_in_tiles, width_in_tiles / aspect_ratio);
        position.set(viewport).div(2);
        refresh();
    }

    public Camera2D(Resolution resolution, float tile_size) {
        viewport.set(resolution.width()/tile_size,resolution.height()/tile_size);
        position.set(viewport).div(2);
        refresh();
    }

    public void updateViewport(float aspect_ratio, float width_in_tiles) {
        viewport.set(width_in_tiles, width_in_tiles / aspect_ratio);
    }

    public void updateViewport(Resolution resolution, float tile_size) {
        viewport.set(resolution.width()/tile_size,resolution.height()/tile_size);
    }

    public void setZoom(float zoom) {
        this.zoom = U.clamp(zoom,zoom_min,zoom_max);
    }

    public float getZoom() {
        return zoom;
    }

    public void zoom(float target, float dt) {
        target = U.clamp(target,zoom_min,zoom_max);
        final float d = zoom - target;
        final float bias = -(zoom_bias_term / dt);
        zoom_velocity += bias * d;
        zoom_velocity *= zoom_damping;
        zoom += zoom_velocity * dt;
        zoom = U.clamp(zoom,zoom_min,zoom_max);
    }

    public void follow(Vector2f target, float dt) {
        final float dx = position.x - target.x;
        final float dy = position.y - target.y;
        final float bias = -(follow_bias_term / dt);
        follow_velocity_x += bias * dx;
        follow_velocity_y += bias * dy;
        follow_velocity_x *= follow_damping;
        follow_velocity_y *= follow_damping;
        position.x += follow_velocity_x * dt;
        position.y += follow_velocity_y * dt;
    }

    /**
     * recalculates the projection and view matrices
     */
    public void refresh() {
        final float scale = U.pow(2, zoom);
        final float x = position.x;
        final float y = position.y;
        final float r = viewport.x / 2f * scale;
        final float t = viewport.y / 2f * scale;
        view.identity().lookAt(x,y,POS_Z,x,y,DIR_Z,UP_X,UP_Y,UP_Z);
        projection.identity().ortho(-r,r,-t,t,NEAR,FAR);
        combined.set(projection).mul(view);
        combined_inv.set(combined).invert();
        frustum.setMax(x + r, y + t);
        frustum.setMin(x - r, y - t);
    }

    /**
     * convert vector from normalized viewport space to a world vector
     * @param vector vector to convert
     */
    public void unProjectVector(Vector2f vector) {
        vector.mul(viewport).mul(U.pow(2, zoom));
    }

    /**
     * convert position from normalized viewport space to a world position
     * @param position position to convert
     */
    public void unProjectPosition(Vector2f position) {
        Vector3f v3 = U.popSetVec3(
                position.x * 2 - 1,
                position.y * 2 - 1, 0);
        v3.mulProject(combined_inv);
        position.set(v3.x,v3.y);
        U.pushVec3();
    }

}
