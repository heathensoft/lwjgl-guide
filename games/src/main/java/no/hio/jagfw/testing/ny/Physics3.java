package no.hio.jagfw.testing.ny;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.Bitmap;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.physics.Body;
import io.github.heathensoft.jagfw.physics.Collision;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import io.github.heathensoft.jagfw.physics.RayContact;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.LineSegment;
import io.github.heathensoft.jagfw.utils.U;
import no.hio.jagfw.testing.Background;
import org.joml.Vector2f;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

/**
 * Frederik Dahl 2/13/2025
 */
public class Physics3 extends Game {

    public static void main(String[] args) {
        Engine.get().run(new Physics3(),args);
    }
    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;
    public static final float tile_size = 32;

    Body player;
    Camera2D camera;
    Background background;
    LineBatch line_batch;
    LineSegment ray = new LineSegment();
    SpriteBatch sprite_batch;
    Vector2f mouse_pos = new Vector2f();
    ArrayList<Body> bodies = new ArrayList<>();
    TileMap tilemap = new TileMap(128,128);

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
        background = new Background();
        line_batch = new LineBatch(512);
        line_batch.enableSmoothLines(true);
        line_batch.setLineWidth(2);
        sprite_batch = new SpriteBatch(512);
        camera = new Camera2D(resolution,tile_size);

        player = new Body(0,0,0.5f,100);
        bodies.add(player);
        bodies.add(new Body(4,4,1,200));
        bodies.add(new Body(8,8,1,200));
        bodies.add(new Body(4,8,1,200));
        bodies.add(new Body(8,4,1,200));

    }

    protected void resize(Resolution resolution) { }

    protected void update(float delta_time) {
        Keyboard keys = Engine.get().window().keys();
        Mouse mouse = Engine.get().window().mouse();
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
            //player.setRotation(U.angle2D(v));
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

        camera.follow(player.position,delta_time);
        camera.refresh();
        mouse_pos.set(mouse.position());
        camera.unProjectPosition(mouse_pos);
        ray.setP0(player.position);
        ray.setP1(mouse_pos);


        if (mouse.buttonPressed(Mouse.LEFT)) {
            if (tilemap.contains(mouse_pos)) {
                tilemap.placeBlock(U.floor(mouse_pos.x),U.floor(mouse_pos.y));
            }
        } else if (mouse.buttonPressed(Mouse.RIGHT)) {
            if (tilemap.contains(mouse_pos)) {
                tilemap.removeBlock(U.floor(mouse_pos.x),U.floor(mouse_pos.y));
            }
        }

        RayContact contact = new RayContact();
        //if (Collision.rayBodies(ray,bodies,contact)) {
        //    ray.setP1(contact.point);
        //}
        if (Collision.rayMap(ray,tilemap,contact)) {
            ray.setP1(contact.point);
        }

        applyForcesAndUpdate(delta_time);
        Collision.resolve(bodies,tilemap);

    }

    protected void render() {
        Framebuffer.bindDefault();
        Framebuffer.viewport();
        background.draw(camera);
        line_batch.begin(camera);
        for (Body body : bodies) {
            PhysicsUtils.drawBody(body,line_batch);
        } PhysicsUtils.drawRay(ray,line_batch);
        line_batch.end();
        sprite_batch.begin(camera);
        tilemap.render(sprite_batch);
        sprite_batch.end();
    }

    protected void exit() {
        Disposable.dispose(
                sprite_batch,
                line_batch,
                background);
    }

    private void applyForcesAndUpdate(float dt) {
        for (Body body : bodies) {
            if (!body.isStatic()) {
                PhysicsUtils.applyDrag(body,20);
                PhysicsUtils.applyFriction(body,300);
            } body.update(dt);
        }
    }
}
