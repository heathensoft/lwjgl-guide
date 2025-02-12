package io.github.heathensoft.jagfw.physicsold.shape;

import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

/**
 * Frederik Dahl 1/24/2025
 */
public class HitBox {

    private final Shape shape;        // shape
    private final Vector2f offset;    // local offset
    private final Vector2f position;  // world position
    private float rotational_offset;


    public HitBox(Shape shape) {
        this(shape,new Vector2f());
    }

    public HitBox(Shape shape, Vector2f offset) {
        this(shape,offset,0);
    }

    public HitBox(Shape shape, Vector2f offset, float rotational_offset) {
        this.rotational_offset = rotational_offset;
        this.position = new Vector2f();
        this.offset = offset;
        this.shape = shape;
    }

    public void update(Vector2f position, float rotation) {
        Vector2f pos = this.position.set(offset);
        rotation += rotational_offset;
        if (rotation != 0) U.rotate2D(pos,rotation);
        shape.updateVertices(pos.add(position),rotation);
    }

    public Shape shape() {
        return shape;
    }

    public Vector2f position() {
        return position;
    }

    public Vector2f offset() {
        return offset;
    }

    public float rotationalOffset() {
        return rotational_offset;
    }

    public void setRotationalOffset(float offset) {
        rotational_offset = offset;
    }

}
