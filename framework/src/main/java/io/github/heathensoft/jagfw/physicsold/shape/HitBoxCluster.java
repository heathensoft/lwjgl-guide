package io.github.heathensoft.jagfw.physicsold.shape;

import io.github.heathensoft.jagfw.utils.U;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A group of Hitboxes.
 * Frederik Dahl 1/24/2025
 */
public class HitBoxCluster implements Iterable<HitBox> {

    private final List<HitBox> hitBoxes = new ArrayList<>(3);
    private final Vector2f position = new Vector2f();
    private final Vector2f offset = new Vector2f();
    private float rotational_offset;

    public static HitBoxCluster pillbox(float width, float height) {
        return pillbox(width,height,new Vector2f());
    }

    public static HitBoxCluster pillbox(float width, float height, Vector2f offset) {
        return pillbox(width,height,offset,0);
    }

    public static HitBoxCluster pillbox(float width, float height, Vector2f offset, float rotational_offset) {
        HitBoxCluster cluster = new HitBoxCluster();
        cluster.offset.set(offset);
        cluster.setRotationalOffset(rotational_offset);
        if (width == height) {
            Circle circle = new Circle(width / 2f);
            cluster.addHitbox(new HitBox(circle,new Vector2f()));
        } else if (height > width) {
            Circle circle_t = new Circle(width / 2f);
            Circle circle_b = new Circle(width / 2f);
            Box center = new Box(width,height - width);
            Vector2f offset_t = new Vector2f(0,center.height / 2f);
            Vector2f offset_b = new Vector2f(0,-center.height / 2f);
            cluster.addHitbox(new HitBox(circle_t,offset_t));
            cluster.addHitbox(new HitBox(circle_b,offset_b));
            cluster.addHitbox(new HitBox(center,new Vector2f()));
        } else { Circle circle_l = new Circle(height / 2f);
            Circle circle_r = new Circle(height / 2f);
            Box center = new Box(width,width - height);
            Vector2f offset_l = new Vector2f(-center.width / 2f, 0);
            Vector2f offset_r = new Vector2f(center.width / 2f, 0);
            cluster.addHitbox(new HitBox(circle_l,offset_l));
            cluster.addHitbox(new HitBox(circle_r,offset_r));
            cluster.addHitbox(new HitBox(center,new Vector2f()));
        } return cluster;
    }

    public void update(Vector2f position, float rotation) {
        Vector2f pos = this.position.set(offset);
        rotation += rotational_offset;
        if (rotation != 0) U.rotate2D(pos,rotation);
        pos.add(position);
        for (HitBox hitbox : hitBoxes) {
            hitbox.update(pos,rotation);
        }
    }

    public void addHitbox(HitBox hitbox) {
        hitBoxes.add(hitbox);
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

    public int size() {
        return hitBoxes.size();
    }

    @NotNull
    @Override
    public Iterator<HitBox> iterator() {
        return hitBoxes.iterator();
    }
}
