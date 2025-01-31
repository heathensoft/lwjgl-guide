package no.hio.jagfw.examples;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.Bitmap;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.physics.ny.*;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

/**
 * Frederik Dahl 1/31/2025
 */
public class Physics2 extends Game {

    public static void main(String[] args) {
        Engine.get().run(new Physics2(),args);
    }

    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;
    public static final float tile_size = 32;


    Body player;
    Camera2D camera;
    Vector2f mouse_pos;
    Background background;
    LineBatch line_batch;
    List<Body> list_of_bodies;
    List<Geometry> list_of_geometry;


    protected void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "physics";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = true;
        boot_config.target_ups = 120;
    }

    protected void start(Resolution resolution) throws Exception {
        Framebuffer.bindDefault();
        Framebuffer.setClearColor(0xFF000000);
        Framebuffer.setClearMask(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        mouse_pos = new Vector2f();
        list_of_bodies = new ArrayList<>();
        list_of_geometry = new ArrayList<>();
        background = new Background();
        camera = new Camera2D(resolution,tile_size);
        line_batch = new LineBatch(512);
        line_batch.enableSmoothLines(true);
        player = new Body(new Circle(0.5f),100,10,10);
        player.rotatable = false;
        list_of_bodies.add(player);

    }

    protected void resize(Resolution resolution) { /* */ }

    protected void update(float delta_time) {
        controls(camera,delta_time);
        applyForcesAndUpdate(delta_time);
        bodyCollisions();
    }

    protected void render() {
        Framebuffer.bindDefault();
        Framebuffer.viewport();
        background.draw(camera);
        line_batch.begin(camera);
        for (Body body : list_of_bodies) {
            PhysicsUtils.drawBody(body,line_batch);
        } for (Geometry geom : list_of_geometry) {
            PhysicsUtils.drawGeometry(geom,line_batch);
        } line_batch.end();

    }

    protected void exit() {
        Disposable.dispose(line_batch, background);
    }

    private void applyForcesAndUpdate(float dt) {
        for (Body body : list_of_bodies) {
            if (!body.isStatic()) {
                CommonForces.applyDrag(body,20);
                CommonForces.applyFriction(body,300);
            } body.update(dt);
        }
    }

    private void bodyCollisions() {
        int num_bodies = list_of_bodies.size();
        BodyContact body_contact = new BodyContact();
        GeomContact geom_contact = new GeomContact();
        for (int i = 0; i < num_bodies; i++) {
            Body A = list_of_bodies.get(i);
            for (int j = i + 1; j < num_bodies; j++) {
                Body B = list_of_bodies.get(j);
                if (Collision.bodyBody(A,B,body_contact)) {
                    body_contact.resolveCollision();
                }
            }
            for (Geometry geom : list_of_geometry) {
                if (Collision.bodyGeom(A,geom,geom_contact)) {
                    geom_contact.resolveCollision(0.1f,0.4f);
                }
            }
        }
    }

    private void controls(Camera2D camera, float dt) {
        Keyboard keys = Engine.get().window().keys();
        Vector2f v = new Vector2f();
        if (keys.pressed(GLFW_KEY_A)) {
            v.add(-3000,0);
        } if (keys.pressed(GLFW_KEY_S)) {
            v.add(0,-3000);
        } if (keys.pressed(GLFW_KEY_D)) {
            v.add(3000,0);
        } if (keys.pressed(GLFW_KEY_W)) {
            v.add(0,3000);
        } player.addForce(v);
        if (v.lengthSquared() > 0) {
            player.setRotation(U.angle2D(v));
            if (keys.justPressed(GLFW_KEY_SPACE)) {
                v.normalize().mul(6000);
                player.applyImpulse(v);
            }
        }
        if (keys.justPressed(GLFW_KEY_F1)) {
            Bitmap bitmap = Framebuffer.screenshot();
            bitmap.compressToDisk("screenshot.png",true);
            bitmap.dispose();
        }

        camera.follow(player.position,dt);
        camera.refresh();
        Mouse mouse = Engine.get().window().mouse();
        mouse_pos.set(mouse.position());
        camera.unProjectPosition(mouse_pos);
    }

}
