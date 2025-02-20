package no.hio.jagfw.testing.ny;


import org.joml.Vector2f;

import java.util.List;

/**
 * Where Bodies (Body.java) collide with other Bodies and the environment,
 * HitBoxes are meant for collision checks with projectiles, bullets, "explosions" and things like that.
 * Frederik Dahl 2/15/2025
 */
public abstract class Hitbox {


    private interface Component {}

    private static class Body implements Component {}

    private static class EnemyBody extends Body { }

    public static void main(String[] args) {

        Component body = new Body();
        Component enemyBody = new EnemyBody();

        // Body class is assignable from enemy body class, not the other way around
        Class<? extends Component> body_class = body.getClass();
        Class<? extends Component> enemy_body_class = enemyBody.getClass();

        System.out.println(enemy_body_class);

        if (body_class.isAssignableFrom(enemy_body_class)) {

        }
        if (enemy_body_class.isAssignableFrom(body_class)) {
            System.out.println("fff");
        }



    }



    /**
     * Positional Offset
     */
    public Vector2f offset = new Vector2f();

    /**
     * Update HitBox.
     * <p>Updates internal state</p>
     * @param position HitBox center position
     * @param rotation rotation
     * @param scale scaling
     */
    public abstract void update(Vector2f position, float rotation, float scale);

    public abstract boolean contains(float x, float y);

    public boolean contains(Vector2f point) {
        return contains(point.x,point.y);
    }

    /**
     * @return Hitboxes with 0 or negative area are invalid.
     */
    public abstract boolean isValid();

}
