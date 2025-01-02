package io.github.heathensoft.guide.utils;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.primitives.Rectanglef;

/**
 * Frederik Dahl 12/28/2024
 */
public class Camera2D {

    private static final Vector3f UP = new Vector3f(0,1,0); // y-axis is always up

    public final Matrix4f view = new Matrix4f();        // view matrix
    public final Matrix4f projection = new Matrix4f();  // projection matrix
    public final Matrix4f combined = new Matrix4f();    // view projection matrix (combined)
    public final Matrix4f combined_inv = new Matrix4f();// inverse combined matrix

    public final Rectanglef bounds = new Rectanglef();               // visible world area
    public final Vector3f position  = new Vector3f(0,0,1);  // camera position (eye)
    public final Vector3f direction = new Vector3f(0,0,-1); // camera facing
    public final Vector2f viewport  = new Vector2f(16,9);      // size of visible area in world units (zoom == 1)

    public float far  = 257.0f; // far plane (anything beyond gets clipped)
    public float near = 1.00f;  // near plane (anything closer gets clipped)
    public float zoom = 1.0f;   // zoom (used to expand, contract the frustum making the scene appear smaller / larger)


    public void refresh() {
        direction.set(position.x,position.y,-1);
        view.identity().lookAt(position,direction,UP);
        float lr = viewport.x / 2f * zoom;
        float tb = viewport.y / 2f * zoom;
        projection.identity().ortho(-lr,lr,-tb,tb,near,far);
        bounds.setMax(position.x + lr, position.y + tb);
        bounds.setMin(position.x - lr, position.y - tb);
        combined.set(projection).mul(view);
        combined_inv.set(combined).invert();
    }

    /**
     * set camera xy position
     */
    public void setPosition(Vector2f position) {
        this.position.set(position.x,position.y,this.position.z);
    }

    /**
     * translate camera in xy
     */
    public void translate(Vector2f translation) {
        position.add(translation.x,translation.y,0);
    }

    /**
     * convert vector from normalized viewport space to a world vector
     * @param vector vector to convert
     */
    public void unProjectVector(Vector2f vector) {
        vector.mul(viewport).mul(zoom);
    }

    /**
     * convert position from normalized viewport space to a world position
     * @param position position to convert
     */
    public void unProjectPosition(Vector2f position) {
        // convert to normalized device coordinates
        Vector3f v3 = U.popSetVec3(
                position.x * 2 - 1,
                position.y * 2 - 1, 0);
        v3.mulProject(combined_inv);
        position.set(v3.x,v3.y);
        U.pushVec3();
    }

}
