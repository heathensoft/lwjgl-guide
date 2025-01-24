package no.hio.jagfw.examples;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.ShaderProgram;
import io.github.heathensoft.jagfw.physics.BodyContact;
import io.github.heathensoft.jagfw.physics.CollisionDetection;
import io.github.heathensoft.jagfw.physics.CommonForces;
import io.github.heathensoft.jagfw.physics.PhysicsBody;
import io.github.heathensoft.jagfw.physics.shape.*;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

/**
 * Frederik Dahl 1/16/2025
 */
public class PhysicsTest extends Game {

    public static void main(String[] args) {
        Engine.get().run(new PhysicsTest(),args);
    }

    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;
    public static final float tile_size = 32;
    private LineBatch line_batch;
    private Background background;
    private Camera2D camera;
    private List<PhysicsBody> bodies;

    PhysicsBody big_ball;
    PhysicsBody big_box;
    PhysicsBody player;
    HitBoxCluster hitbox_list = HitBoxCluster.pillbox(1,2,new Vector2f(0,0.0f));



    public void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "physics";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = true;
        boot_config.target_ups = 120;
    }

    public void start(Resolution resolution) throws Exception {
        bodies = new ArrayList<>();
        camera = new Camera2D(resolution,tile_size);
        background = new Background();
        line_batch = new LineBatch(512);
        line_batch.setLineWidth(1f);
        line_batch.enableSmoothLines(true);

        float window_width_pixels = resolution.width();
        float window_height_pixels = resolution.height();
        float window_width_tiles = (window_width_pixels / tile_size);
        float window_height_tiles = (window_height_pixels / tile_size);
        float center_x = window_width_tiles / 2f;
        float center_y = window_height_tiles / 2f;
        float wall_thickness = 2.0f;
        float wall_restitution = 0.2f;

        PhysicsBody ground = new PhysicsBody(
                new Box(window_width_tiles,wall_thickness),
                0, center_x,wall_thickness / 2f
        ); ground.setRestitution(wall_restitution);
        ground.setFriction(0.1f);

        PhysicsBody wall_left = new PhysicsBody(
                new Box(wall_thickness, window_height_tiles - wall_thickness),
                0, wall_thickness / 2f, center_y + wall_thickness / 2f
        ); wall_left.setRestitution(wall_restitution);

        PhysicsBody wall_right = new PhysicsBody(
                new Box(wall_thickness, window_height_tiles - wall_thickness),
                0, window_width_tiles - wall_thickness / 2f, center_y + wall_thickness / 2f
        ); wall_right.setRestitution(wall_restitution);


        for (int i = 0; i < window_width_tiles; i++) {
           PhysicsBody body = new PhysicsBody(new Box(1),0,i,5.5f);
           body.setFriction(0.1f);
           bodies.add(body);
        }

        Vector2f[] polygon_vertices = new Vector2f[6];

        for (int i = 0; i < polygon_vertices.length; i++) {
            Vector2f vertex = new Vector2f(1,0);
            float div = (i + 1f) / polygon_vertices.length;
            U.rotate2D(vertex, Math.PI_TIMES_2_f * div).mul(2);
            polygon_vertices[i] = vertex;
        }




        big_ball = new PhysicsBody(
                new Circle(3),0,
                center_x, center_y);
        big_box = new PhysicsBody(
                new Polygon(polygon_vertices),500,
                center_x,center_y);
        // big_box.setRotation(0.73f);

        player = new PhysicsBody(new Circle(0.5f),100,6,10);
        player.setRotatable(false);


        bodies.add(ground);
        bodies.add(wall_left);
        bodies.add(wall_right);
        bodies.add(big_box);
        bodies.add(player);
    }

    public void resize(Resolution resolution) {

    }

    public void update(float delta_time) {

        //System.out.println(Engine.get().time().framesPerSecond());

        // Player controls
        Keyboard keys = Engine.get().window().keys();

        Vector2f v = U.popSetVec2(0,0);

        if (keys.pressed(GLFW_KEY_A)) {
            v.add(-3000,0);
        } if (keys.pressed(GLFW_KEY_S)) {
            v.add(0,-3000);
        } if (keys.pressed(GLFW_KEY_D)) {
            v.add(3000,0);
        } if (keys.pressed(GLFW_KEY_W)) {
            v.add(0,3000);
        }

        player.addForce(v);

        if (v.lengthSquared() > 0) {

            player.setRotation(U.angle2D(v));

            if (keys.justPressed(GLFW_KEY_SPACE)) {
                v.normalize().mul(6000);
                player.applyImpulse(v);
            }
        }




        U.pushVec2();



        {   // MOUSE INPUT
            Mouse mouse = Engine.get().window().mouse();
            Vector2f mouse_pos = U.popSetVec2(mouse.position());
            camera.unProjectPosition(mouse_pos);
            if (mouse.justClicked(Mouse.LEFT)) {
                PhysicsBody body = new PhysicsBody(new Circle(0.5f),50,mouse_pos.x,mouse_pos.y);
                body.setFriction(0.2f);
                body.setRestitution(0.8f);
                //body.rotatable = false;
                bodies.add(body);
            } else if (mouse.justClicked(Mouse.RIGHT)) {
                PhysicsBody body = new PhysicsBody(new Box(1),100,mouse_pos.x,mouse_pos.y);
                body.setFriction(0.6f);
                body.setRestitution(0.2f);
                body.setAngularDamping(4.0f);
                bodies.add(body);
            } U.pushVec2(); // mouse
        }

        {   // APPLY FORCES
            Vector2f vec = U.popVec2();
            for (PhysicsBody body : bodies) {
                if (!body.isStatic()) {
                    if (keys.pressed(GLFW_KEY_E)) {
                        if (body != player) {
                            CommonForces.applySpringForce(body,player.position(vec),2,200);
                        }
                    }
                    CommonForces.applyDrag(body,50);
                    CommonForces.applyFriction(body,200);
                } body.update(delta_time);
            } U.pushVec2();
        }


        {   // COLLISION
            int num_bodies = bodies.size();
            BodyContact contact = new BodyContact();
            for (int i = 0; i < num_bodies; i++) {
                for (int j = i + 1; j < num_bodies; j++) {
                    PhysicsBody bodyA = bodies.get(i);
                    PhysicsBody bodyB = bodies.get(j);
                    if (CollisionDetection.bodyBody(bodyA,bodyB,contact)) {
                        contact.resolveCollision();

                    }
                }
            }
        }

        hitbox_list.update(player.position(new Vector2f()),player.rotation());

    }

    public void render() {
        Engine.get().window().useWindowViewport();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        background.draw(camera);
        line_batch.begin(camera);
        int color;
        for (PhysicsBody body : bodies) {
            if (body.colliding()) color = 0xFF0000FF;
            else color = 0xFF00FF00;
            if (body.shape() instanceof Circle circle) {
                Vector2f position = body.position(U.popVec2());
                line_batch.drawCircle(position,circle.radius(),32,color);
                line_batch.drawLine(position,body.rotation(),circle.radius(),color);
                U.pushVec2();
            } else if (body.shape() instanceof PolygonShape box) {
                Vector2f[] vertices = box.vertices();
                for (int i = 0; i < vertices.length; i++) {
                    Vector2f v0 = vertices[i];
                    Vector2f v1 = vertices[((i + 1) % vertices.length)];
                    line_batch.drawLine(v0,v1,color);
                }
            }
        }

        for (HitBox hitbox : hitbox_list) {
            if (hitbox.shape() instanceof Circle circle) {
                line_batch.drawCircle(hitbox.position(),circle.radius(),32,0xFFFF0000);
            } else if (hitbox.shape() instanceof PolygonShape polygon) {
                Vector2f[] vertices = polygon.vertices();
                for (int i = 0; i < vertices.length; i++) {
                    Vector2f v0 = vertices[i];
                    Vector2f v1 = vertices[((i + 1) % vertices.length)];
                    line_batch.drawLine(v0,v1,0xFFFF0000);
                }
            }
        }

        line_batch.end();

    }

    public void exit() {
        Disposable.dispose(
                line_batch,
                background);
        ShaderProgram.deleteAllPrograms(); // Todo: should be in engine
    }
}
