package io.github.heathensoft.guide.game;

import io.github.heathensoft.guide.core.*;
import io.github.heathensoft.guide.core.Disposable;
import io.github.heathensoft.guide.core.gfx.ShaderProgram;
import io.github.heathensoft.guide.core.gfx.SpriteBatch;
import io.github.heathensoft.guide.utils.U;
import org.joml.Math;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

/**
 * Frederik Dahl 12/5/2024
 */
public class Game implements IGame {

    public static void main(String[] args) {
        Engine.get().run(new Game(),args);
    }

    public static final int game_res_w = 1200;
    public static final int game_res_h = 800;
    private SpriteBatch batch;
    private Background background;
    private Camera2D camera;
    private World world;


    public void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "lwjgl-guide";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = true;
    }

    public void start(Resolution resolution) throws Exception {
        camera = new Camera2D();
        world = new World();
        camera.viewport.set(resolution.width(),resolution.height()).div(100);
        camera.refresh();
        batch = new SpriteBatch(256);
        background = new Background();
    }

    public void resize(Resolution resolution) { /* */ }

    public void update(float delta_time) {
        controls(camera,world,delta_time);
        world.update(delta_time);
    }

    public void render() {
        Engine.get().window().useWindowViewport();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        background.draw(camera);
        batch.begin(camera);
        world.render(batch,camera.bounds);
        batch.end();
    }

    public void exit() {
        Disposable.dispose(batch,background);
        ShaderProgram.deleteAllPrograms();
    }

    private boolean camera_currently_zooming = false;
    private float camera_zoom_timer = 0.0f;
    private float camera_zoom_accumulator;
    private float camera_current_zoom;
    private float camera_target_zoom;
    private final Vector2f camera_drag_origin = new Vector2f();

    private void controls(Camera2D camera, World world, float delta_time) {

        GLFWWindow window = Engine.get().window();
        Keyboard keys = window.keys();
        Mouse mouse = window.mouse();

        if (keys.justPressed(GLFW_KEY_ESCAPE)) {
            Engine.get().exitMainLoop();
            return;
        } else if (keys.justPressed(GLFW_KEY_F1)) {
            if (window.isWindowedMode()) window.fullScreen();
            else window.windowedMode(game_res_w,game_res_h);
        }

        if (mouse.justClicked(Mouse.LEFT)) {
            Vector2f cursor = U.popSetVec2(mouse.position());
            camera.unProjectPosition(cursor);
            world.toggleBlock(U.floor(cursor.x),U.floor(cursor.y));
            U.pushVec2();
        }

        final float move_speed = 5.0f;
        final float zoom_speed = 1.5f;
        final float zoom_min = -2.0f;
        final float zoom_max = 4.0f;

        Vector2f velocity = U.popVec2().zero();
        if (keys.pressed(GLFW_KEY_W) || keys.pressed(GLFW_KEY_UP)) velocity.y += 1;
        if (keys.pressed(GLFW_KEY_A) || keys.pressed(GLFW_KEY_LEFT)) velocity.x -= 1;
        if (keys.pressed(GLFW_KEY_S) || keys.pressed(GLFW_KEY_DOWN)) velocity.y -= 1;
        if (keys.pressed(GLFW_KEY_D) || keys.pressed(GLFW_KEY_RIGHT)) velocity.x += 1;
        if (velocity.x != 0 || velocity.y != 0) {
            velocity.normalize().mul(move_speed * delta_time);
            camera.translate(velocity);
        } U.pushVec2();

        if (mouse.scrolled()) {
            float scroll_value = mouse.scrollValue();
            if (camera_currently_zooming) {
                if (scroll_value > 0.0) { // scroll in
                    if ((camera_current_zoom > camera_target_zoom) && camera_zoom_timer < 0.3) {
                        camera_target_zoom -= scroll_value;
                        camera_target_zoom = Math.max(camera_target_zoom,zoom_min);
                    } else camera_zoom_accumulator -= scroll_value;
                } else { // scroll out
                    if ((camera_current_zoom < camera_target_zoom) && camera_zoom_timer < 0.3) {
                        camera_target_zoom -= scroll_value;
                        camera_target_zoom = Math.min(camera_target_zoom,zoom_max);
                    } else camera_zoom_accumulator -= scroll_value;
                }
            } else {
                camera_target_zoom = camera_current_zoom - scroll_value;
                camera_target_zoom = U.clamp(camera_target_zoom,zoom_min,zoom_max);
                if (camera_current_zoom != camera_target_zoom) {
                    camera_currently_zooming = true;
                    camera_zoom_timer = 0.0f;
                }
            }
        }

        if (camera_currently_zooming) {
            camera_zoom_timer += delta_time * zoom_speed;
            if (camera_zoom_timer >= 1.0f) {
                camera_current_zoom = camera_target_zoom;
                camera.zoom = U.pow(2, camera_current_zoom);
                if (camera_zoom_accumulator != 0.0) {
                    camera_target_zoom = camera_current_zoom + camera_zoom_accumulator;
                    camera_target_zoom = U.clamp(camera_target_zoom,zoom_min,zoom_max);
                    camera_zoom_accumulator = 0.0f;
                } else camera_currently_zooming = false;
                camera_zoom_timer = 0.0f;
            } else {
                float t = U.smooth(camera_zoom_timer);
                float zoom = U.lerp(camera_current_zoom, camera_target_zoom,t);
                camera.zoom = U.pow(2,zoom);
            }
        }

        if (mouse.isDragging(Mouse.WHEEL)) {
            if (mouse.justStartedDrag(Mouse.WHEEL)) {
                camera_drag_origin.set(camera.position);
            } Vector2f drag = U.popSetVec2((mouse.dragVector(Mouse.WHEEL)));
            camera.unProjectVector(drag);
            camera.setPosition(camera_drag_origin);
            camera.translate(drag.negate());
            U.pushVec2();
        }

        camera.refresh();
    }
}
