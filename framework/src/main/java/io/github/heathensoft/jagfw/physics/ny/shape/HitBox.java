package io.github.heathensoft.jagfw.physics.ny.shape;

import io.github.heathensoft.jagfw.utils.U;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A HitBox is not the same as a Body Shape.
 * Hit boxes are separate from the physics.
 * Hit boxes do not collide with anything.
 * Hit boxes are used to check if an object
 * is hit by anything (true / false).
 * Frederik Dahl 1/23/2025
 */
public class HitBox implements Iterable<Shape>{

    private final List<Shape> shapes = new ArrayList<>(3);
    private final List<Vector2f> offsets = new ArrayList<>(3);

    public HitBox() {}

    public static HitBox pillBox(float width, float height) {
        HitBox hitBox = new HitBox();
        if (width == height) {
            hitBox.addShape(new Circle(width),new Vector2f());
        } else if (Math.abs(width) > Math.abs(height)){
            hitBox.addShape(new Circle(height),new Vector2f(0,width / 2f));
            hitBox.addShape(new Circle(height),new Vector2f(0,-width / 2f));
            hitBox.addShape(new Box(width - height,height),new Vector2f());
        } else { hitBox.addShape(new Circle(width),new Vector2f(height / 2f,0));
            hitBox.addShape(new Circle(width),new Vector2f(-height / 2f,0));
            hitBox.addShape(new Box(width,height - width),new Vector2f());
        } return hitBox;
    }

    public void addShape(Shape shape, Vector2f offset) {
        shapes.add(shape);
        offsets.add(offset);
    }

    public void update(Vector2f position, float rotation) {
        Vector2f offset = U.popVec2();
        float sin = Math.sin(rotation);
        float cos = Math.cos(rotation);
        for (int i = 0; i < shapes.size(); i++) {
            offset.set(offsets.get(i));
            offset.x = offset.x * cos - offset.y * sin;
            offset.y = offset.x * sin + offset.y * cos;
            offset.add(position);
            shapes.get(i).updateVertices(position, rotation);
        } U.pushVec2();
    }


    @NotNull
    public Iterator<Shape> iterator() {
        return shapes.iterator();
    }
}
