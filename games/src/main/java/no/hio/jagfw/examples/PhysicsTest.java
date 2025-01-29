package no.hio.jagfw.examples;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
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
    private TileMap tile_map;
    private LineBatch line_batch;
    private SpriteBatch sprite_batch;
    private Background background;
    private Camera2D camera;
    private List<PhysicsBody> bodies;
    private List<PhysicsGeometry> geometry;

    PhysicsBody player;
    HitBoxCluster hitbox_list = HitBoxCluster.pillbox(1,2,new Vector2f(0,0.0f));
    Vector2f mouse_world = new Vector2f();

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
        geometry = new ArrayList<>();
        camera = new Camera2D(resolution,tile_size);
        background = new Background();
        line_batch = new LineBatch(512);
        line_batch.setLineWidth(1f);
        line_batch.enableSmoothLines(true);
        tile_map = new TileMap(MapSize.SMALL);
        sprite_batch = new SpriteBatch(512);

        float window_width_pixels = resolution.width();
        float window_height_pixels = resolution.height();
        float window_width_tiles = (window_width_pixels / tile_size);
        float window_height_tiles = (window_height_pixels / tile_size);

        PhysicsGeometry borders = new PhysicsGeometry(4);
        borders.vertices[0].set(2,2);
        borders.vertices[1].set(window_width_tiles - 2, 2);
        borders.vertices[2].set(window_width_tiles - 2,window_height_tiles - 2);
        borders.vertices[3].set(2,window_height_tiles - 2);
        borders.setPolygon(true);
        geometry.add(borders);

        PhysicsGeometry line = new PhysicsGeometry(2);
        line.vertices[0].set(8,8);
        line.vertices[1].set(16,12);
        geometry.add(line);

        player = new PhysicsBody(new Circle(0.5f),100,6,10);
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

        Mouse mouse = Engine.get().window().mouse();
        mouse_world.set(mouse.position());
        camera.unProjectPosition(mouse_world);
        {   // MOUSE INPUT

            //if (mouse.justClicked(Mouse.LEFT)) {
            //    PhysicsBody body = new PhysicsBody(new Circle(0.5f),50,mouse_world.x,mouse_world.y);
            //    body.setFriction(0.2f);
            //    body.setRestitution(0.8f);
            //    //body.rotatable = false;
            //    bodies.add(body);
            //} else if (mouse.justClicked(Mouse.RIGHT)) {
            //    PhysicsBody body = new PhysicsBody(new Box(1),100,mouse_world.x,mouse_world.y);
            //    body.setFriction(0.1f);
            //    body.setRestitution(0.2f);
            //    body.setAngularDamping(4.0f);
            //    bodies.add(body);
            //}
        }

        // Block Placement
        if (mouse.buttonPressed(Mouse.LEFT)) {
            if (tile_map.contains(mouse_world)) {
                tile_map.addBlock(U.floor(mouse_world.x),U.floor(mouse_world.y));
            }
        } else if (mouse.buttonPressed(Mouse.RIGHT)) {
            if (tile_map.contains(mouse_world)) {
                tile_map.removeBlock(U.floor(mouse_world.x),U.floor(mouse_world.y));
            }
        } else if (mouse.justClicked(Mouse.WHEEL)) {
            PhysicsBody body = new PhysicsBody(new Box(1),100,mouse_world.x,mouse_world.y);
            body.setFriction(0.1f);
            body.setRestitution(0.2f);
            body.setAngularDamping(4.0f);
            bodies.add(body);
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
                    //CommonForces.applyDrag(body,20);
                    //CommonForces.applyFriction(body,300);
                    CommonForces.applyDownwardsGravity(body,9.81f);
                } body.update(delta_time);
            } U.pushVec2();
        }

        {   // COLLISION
            int num_bodies = bodies.size();
            BodyContact body_contact = new BodyContact();
            GeometryContact geometry_contact = new GeometryContact();
            for (int i = 0; i < num_bodies; i++) {
                PhysicsBody bodyA = bodies.get(i);
                for (int j = i + 1; j < num_bodies; j++) {
                    PhysicsBody bodyB = bodies.get(j);
                    if (CollisionDetection.bodyBody(bodyA,bodyB,body_contact)) {
                        body_contact.resolveCollision();
                    }
                }
                for (PhysicsGeometry geom : geometry) {
                    if (CollisionDetection.bodyGeometry(bodyA,geom,geometry_contact)) {
                        geometry_contact.resolveCollision();
                    }
                }
                if(tile_map.bodyCollision(bodyA,geometry_contact)) {
                    geometry_contact.resolveCollision();
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
        sprite_batch.enableLayers(false);
        sprite_batch.begin(camera);
        tile_map.renderBlocks(sprite_batch, camera.frustum);
        sprite_batch.end();
        line_batch.begin(camera);
        DebugUtils.drawHitBox(hitbox_list,line_batch);
        for (PhysicsBody body : bodies) {
            DebugUtils.drawBody(body,line_batch);
        } for (PhysicsGeometry geom : geometry) {
            DebugUtils.drawGeometry(geom,line_batch);
        }//line_batch.drawLine(player.position(new Vector2f()),mouse_world,0xFFFFFF00);
        line_batch.end();
    }

    public void exit() {
        Disposable.dispose(line_batch, sprite_batch, tile_map, background);
    }
}
