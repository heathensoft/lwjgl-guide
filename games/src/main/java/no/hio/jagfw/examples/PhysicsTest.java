package no.hio.jagfw.examples;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.ShaderProgram;
import io.github.heathensoft.jagfw.physics.*;
import io.github.heathensoft.jagfw.physics.shape.*;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.U;
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
    private List<Surface> surfaces;


    PhysicsBody player;
    HitBoxCluster hitbox_list = HitBoxCluster.pillbox(1,2,new Vector2f(0,0.0f));
    Vector2f mouse_world = new Vector2f();
    SurfaceContact surface_contact = new SurfaceContact();


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
        surfaces = new ArrayList<>();
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


        Surface b = new Surface();
        Surface l = new Surface();
        Surface r = new Surface();
        b.segment.set(0,4f,window_width_tiles,4);
        b.friction = 0.4f;
        l.segment.set(4,0,4,window_height_tiles);
        r.segment.set(window_width_tiles - 4,0,window_width_tiles - 4,window_height_tiles);
        surfaces.add(b);
        surfaces.add(l);
        surfaces.add(r);

        Surface surface = new Surface();
        surface.segment.set(4,8,20,20);
        surfaces.add(surface);



        player = new PhysicsBody(new Circle(0.25f),100,6,10);
        player.setRotatable(false);

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
            mouse_world.set(mouse.position());
            camera.unProjectPosition(mouse_world);
            if (mouse.justClicked(Mouse.LEFT)) {
                PhysicsBody body = new PhysicsBody(new Circle(0.5f),50,mouse_world.x,mouse_world.y);
                body.setFriction(0.2f);
                body.setRestitution(0.8f);
                //body.rotatable = false;
                bodies.add(body);
            } else if (mouse.justClicked(Mouse.RIGHT)) {
                PhysicsBody body = new PhysicsBody(new Box(1),100,mouse_world.x,mouse_world.y);
                body.setFriction(0.1f);
                body.setRestitution(0.2f);
                body.setAngularDamping(4.0f);
                bodies.add(body);
            }
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
                    CommonForces.applyDrag(body,10);

                    if (body != player) {
                        CommonForces.applyDownwardsGravity(body,9.81f);
                    } else CommonForces.applyFriction(body,200);

                } body.update(delta_time);
            } U.pushVec2();
        }


        {   // COLLISION
            int num_bodies = bodies.size();
            BodyContact contact = new BodyContact();
            for (int i = 0; i < num_bodies; i++) {
                PhysicsBody bodyA = bodies.get(i);
                for (int j = i + 1; j < num_bodies; j++) {
                    PhysicsBody bodyB = bodies.get(j);
                    if (CollisionDetection.bodyBody(bodyA,bodyB,contact)) {
                        contact.resolveCollision();
                    }
                }
                for (int j = 0; j < surfaces.size(); j++) {
                    Surface surface = surfaces.get(j);
                    if (CollisionDetection.bodySurface(bodyA,surface,surface_contact)) {
                        surface_contact.resolveCollision();
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

        //for (HitBox hitbox : hitbox_list) {
        //    if (hitbox.shape() instanceof Circle circle) {
        //        line_batch.drawCircle(hitbox.position(),circle.radius(),32,0xFFFF0000);
        //    } else if (hitbox.shape() instanceof PolygonShape polygon) {
        //        Vector2f[] vertices = polygon.vertices();
        //        for (int i = 0; i < vertices.length; i++) {
        //            Vector2f v0 = vertices[i];
        //            Vector2f v1 = vertices[((i + 1) % vertices.length)];
        //            line_batch.drawLine(v0,v1,0xFFFF0000);
        //        }
        //    }
        //}

        {

            for (Surface surface : surfaces) {
                line_batch.drawLine(
                        surface.segment.x0,
                        surface.segment.y0,
                        surface.segment.x1,
                        surface.segment.y1,
                        0xFF00FF00
                );
            }
        }


        //line_batch.drawLine(player.position(new Vector2f()),mouse_world,0xFFFFFF00);

        line_batch.end();

    }

    public void exit() {
        Disposable.dispose(
                line_batch,
                background);
        ShaderProgram.deleteAllPrograms(); // Todo: should be in engine
    }
}
