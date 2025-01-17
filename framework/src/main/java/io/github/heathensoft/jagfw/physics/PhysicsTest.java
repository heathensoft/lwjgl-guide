package io.github.heathensoft.jagfw.physics;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.ShaderProgram;
import io.github.heathensoft.jagfw.game.Background;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

/**
 * Frederik Dahl 1/16/2025
 */
public class PhysicsTest implements IGame {

    public static void main(String[] args) {
        Engine.get().run(new PhysicsTest(),args);
    }

    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;
    public static final float tile_size = 32;
    private LineBatch line_batch;
    private Background background;
    private Camera2D camera;
    private List<RigidBody> bodies;

    RigidBody big_ball;
    RigidBody big_box;


    public void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "physics";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = false;
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

        RigidBody ground = new RigidBody(
                new Shape.Box(window_width_tiles,wall_thickness),
                0, center_x,wall_thickness / 2f
        ); ground.setRestitution(wall_restitution);
        RigidBody wall_left = new RigidBody(
                new Shape.Box(wall_thickness, window_height_tiles - wall_thickness),
                0, wall_thickness / 2f, center_y + wall_thickness / 2f
        ); wall_left.setRestitution(wall_restitution);
        RigidBody wall_right = new RigidBody(
                new Shape.Box(wall_thickness, window_height_tiles - wall_thickness),
                0, window_width_tiles - wall_thickness / 2f, center_y + wall_thickness / 2f
        ); wall_right.setRestitution(wall_restitution);


        big_ball = new RigidBody(
                new Shape.Circle(3),0,
                center_x, center_y);
        big_box = new RigidBody(
                new Shape.Box(4,4),0,
                center_x,center_y);
        //big_box.setRotation(0.73f);


        bodies.add(ground);
        bodies.add(wall_left);
        bodies.add(wall_right);
        bodies.add(big_box);
    }

    public void resize(Resolution resolution) {

    }

    public void update(float delta_time) {

        //System.out.println(Engine.get().time().framesPerSecond());
        {   // MOUSE INPUT
            Mouse mouse = Engine.get().window().mouse();
            Vector2f mouse_pos = U.popSetVec2(mouse.position());
            camera.unProjectPosition(mouse_pos);
            if (mouse.justClicked(Mouse.LEFT)) {
                RigidBody body = new RigidBody(new Shape.Circle(0.5f),50,mouse_pos.x,mouse_pos.y);
                body.friction = 0.2f;
                body.restitution = 0.8f;
                bodies.add(body);
            } else if (mouse.justClicked(Mouse.RIGHT)) {
                RigidBody body = new RigidBody(new Shape.Box(1),100,mouse_pos.x,mouse_pos.y);
                body.friction = 0.6f;
                body.restitution = 0.2f;
                // body.setRotation(0.09f);
                bodies.add(body);
            } U.pushVec2(); // mouse
        }

        {   // APPLY FORCES
            Vector2f vec = U.popVec2();
            for (RigidBody body : bodies) {
                Vector2f weight = vec.set(0f,-9.8f * body.mass());
                body.addForce(weight);
                //Vector2f wind = vec.set(10.0f,0.0f);
                //body.addForce(wind);
                //ForceUtil.applyDrag(body,1f);
                body.update(delta_time);
            } U.pushVec2();
        }


        {   // COLLISION
            int num_bodies = bodies.size();
            Contact contact = new Contact();
            for (int i = 0; i < num_bodies; i++) {
                for (int j = i + 1; j < num_bodies; j++) {
                    RigidBody bodyA = bodies.get(i);
                    RigidBody bodyB = bodies.get(j);
                    if (CollisionDetection.bodyBody(bodyA,bodyB,contact)) {
                        contact.resolveCollision();
                        bodyA.colliding = true;
                        bodyB.colliding = true;
                    }
                }
            }
        }


        Rectanglef view = camera.frustum;
        for (RigidBody body : bodies) {
            if (body.shape instanceof Shape.Circle circleShape) {
                if ((body.position.x - circleShape.radius) <= view.minX) {
                    body.position.x = view.minX + circleShape.radius;
                    body.velocity.x *= -0.5f;
                } else if ((body.position.x + circleShape.radius) >= view.maxX) {
                    body.position.x = view.maxX - circleShape.radius;
                    body.velocity.x *= -0.5f;
                }
                if ((body.position.y - circleShape.radius) <= view.minY) {
                    body.position.y = view.minY + circleShape.radius;
                    body.velocity.y *= -0.5f;
                } else if ((body.position.y + circleShape.radius) >= view.maxY) {
                    body.position.y = view.maxY - circleShape.radius;
                    body.velocity.y *= -0.5f;
                }
            }

        }
    }

    public void render() {
        Engine.get().window().useWindowViewport();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        background.draw(camera);
        line_batch.begin(camera);
        int color;
        for (RigidBody body : bodies) {
            if (body.colliding) color = 0xFF0000FF;
            else color = 0xFF00FF00;
            if (body.shape instanceof Shape.Circle circle) {
                line_batch.drawCircle(body.position,circle.radius,32,color);
                line_batch.drawLine(body.position,body.rotation,circle.radius,color);
            } else if (body.shape instanceof Shape.Box box) {
                Vector2f[] vertices = box.vertices();
                for (int i = 0; i < vertices.length; i++) {
                    Vector2f v0 = vertices[i];
                    Vector2f v1 = vertices[((i + 1) % vertices.length)];
                    line_batch.drawLine(v0,v1,color);
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
