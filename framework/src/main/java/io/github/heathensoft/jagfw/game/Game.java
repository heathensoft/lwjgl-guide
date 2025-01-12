package io.github.heathensoft.jagfw.game;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.ShaderProgram;
import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.core.gfx.Texture;
import io.github.heathensoft.jagfw.utils.Camera2D;
import io.github.heathensoft.jagfw.utils.Color;
import io.github.heathensoft.jagfw.utils.U;
import org.joml.Math;
import org.joml.Random;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Frederik Dahl 12/5/2024
 */
public class Game implements IGame {

    public static void main(String[] args) {
        Engine.get().run(new Game(),args);
    }

    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;
    private SpriteBatch sprite_batch;
    private LineBatch line_batch;
    private Background background;
    private Camera2D camera_world;
    private Camera2D camera_hud;
    private TileMap tile_map;



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
        camera_world = new Camera2D(resolution.aspectRatio(),16f);
        camera_hud = new Camera2D(resolution.aspectRatio(),16f);
        tile_map = new TileMap(MapSize.SMALL);
        sprite_batch = new SpriteBatch(512);
        background = new Background();
        line_batch = new LineBatch(256);
        line_batch.setLineWidth(2f);
        line_batch.enableSmoothLines(true);
    }

    public void resize(Resolution resolution) { /* */ }


    public void update(float delta_time) {
        controls(camera_world, tile_map,delta_time);
    }





    public void render() {
        Engine.get().window().useWindowViewport();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        background.draw(camera_world);
        sprite_batch.enableLayers(true);
        sprite_batch.begin(camera_world);
        tile_map.renderBlocks(sprite_batch, camera_world.frustum);
        zFightingTest(sprite_batch);
        sprite_batch.end();
        renderControllerState();
    }

    public void exit() {
        Disposable.dispose(
                tile_map,
                sprite_batch,
                line_batch,
                background);
        ShaderProgram.deleteAllPrograms();
    }

    private boolean camera_currently_zooming = false;
    private float camera_zoom_timer = 0.0f;
    private float camera_zoom_accumulator;
    private float camera_current_zoom;
    private float camera_target_zoom;
    private final Vector2f camera_drag_origin = new Vector2f();

    private void controls(Camera2D camera, TileMap tile_map, float delta_time) {
        GLFWWindow window = Engine.get().window();
        Keyboard keys = window.keys();
        Mouse mouse = window.mouse();
        if (keys.justPressed(GLFW_KEY_ESCAPE)) {
            Engine.get().exitMainLoop();
            return;
        } else if (keys.justPressed(GLFW_KEY_F1)) {
            if (window.isWindowedMode()) window.fullScreen();
            else window.windowedMode(game_res_w,game_res_h);
        } else if (keys.justPressed(GLFW_KEY_DELETE,GLFW_KEY_LEFT_CONTROL)) {
            tile_map.clear();
        }

        // Block Placement
        if (mouse.buttonPressed(Mouse.LEFT)) {
            Vector2f cursor = U.popSetVec2(mouse.position());
            camera.unProjectPosition(cursor);
            if (tile_map.contains(cursor)) {
                tile_map.addBlock(U.floor(cursor.x),U.floor(cursor.y));
            } U.pushVec2();
        } else if (mouse.buttonPressed(Mouse.RIGHT)) {
            Vector2f cursor = U.popSetVec2(mouse.position());
            camera.unProjectPosition(cursor);
            if (tile_map.contains(cursor)) {
                tile_map.removeBlock(U.floor(cursor.x),U.floor(cursor.y));
            } U.pushVec2();
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
            camera.position.add(velocity);
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
            camera.position.set(camera_drag_origin);
            camera.position.add(drag.negate());
            U.pushVec2();
        }

        camera.refresh();
    }

    private void renderControllerState() {
        Controller controller = Engine.get().window().controller();
        if (controller.isConnected()) {
            Vector2f v1 = U.popVec2();
            Vector2f v2 = U.popVec2();
            line_batch.begin(camera_hud);

            v1.set(1,1);
            if (controller.leftStickPushed()) {
                float magnitude = controller.leftStickMagnitude();
                if (magnitude == 1.0f) line_batch.drawCircle(v1,1,32, 0xFFFF00FF);
                else line_batch.drawCircle(v1,1,32, 0xFF00FF00);
                v1.add(v2.set(controller.leftStickDirection()).mul(magnitude * 0.5f));
            } else line_batch.drawCircle(v1,1,32, 0xFF00FF00);
            line_batch.drawCircle(v1,0.5f,16, 0xFFFF0000);

            v1.set(camera_hud.viewport.x - 1,1);
            if (controller.rightStickPushed()) {
                float magnitude = controller.rightStickMagnitude();
                if (magnitude == 1.0f) line_batch.drawCircle(v1,1,32, 0xFFFF00FF);
                else line_batch.drawCircle(v1,1,32, 0xFF00FF00);
                v1.add(v2.set(controller.rightStickDirection()).mul(magnitude * 0.5f));
            } else line_batch.drawCircle(v1,1,32, 0xFF00FF00);
            line_batch.drawCircle(v1,0.5f,16, 0xFFFF0000);

            if (controller.leftTriggerPressed()) {
                int bars = (int) (controller.leftTriggerMagnitude() * 64);
                Vector4f color1 = Color.rgbToHsv(U.popSetVec4(0,1,0,1));
                Vector4f color2 = Color.rgbToHsv(U.popSetVec4(1,0,0,1));
                Vector4f color = U.popVec4();
                for (int i = 1; i <= bars; i++) {
                    float t = (float) i / 64;
                    float y = 2 + t * 2f;
                    Color.hsvLerp(color1,color2,t,color);
                    v1.set(0,y);
                    v2.set(1,y);
                    line_batch.drawLine(v1,v2,Color.rgbToIntColor(Color.hsvToRgb(color)));
                } U.pushVec4(3);
            }

            if (controller.rightTriggerPressed()) {
                int bars = (int) (controller.rightTriggerMagnitude() * 64);
                Vector4f color1 = Color.rgbToHsv(U.popSetVec4(0,1,0,1));
                Vector4f color2 = Color.rgbToHsv(U.popSetVec4(1,0,0,1));
                Vector4f color = U.popVec4();
                for (int i = 1; i <= bars; i++) {
                    float t = (float) i / 64;
                    float y = 2 + t * 2f;
                    Color.hsvLerp(color1,color2,t,color);
                    v1.set(camera_hud.viewport.x,y);
                    v2.set(camera_hud.viewport.x - 1,y);
                    line_batch.drawLine(v1,v2,Color.rgbToIntColor(Color.hsvToRgb(color)));
                } U.pushVec4(3);
            }


            line_batch.end();
            U.pushVec2(2);
        }
    }

    float accum = 0;
    int[] colors = new int[] {
            0xFFFFFFFF, 0xFF0000FF, 0xFF0000FF,
            0xFFFFFFFF, 0xFFFF00FF, 0xFFFF00FF,
            0xFFFFFFFF, 0xFF0000FF, 0xFF0000FF,
            0xFFFFFFFF, 0xFFFF00FF, 0xFFFF00FF,
    };
    private void zFightingTest(SpriteBatch batch) {
        accum += 0.01666667f;
        if (accum >= 10) {
            accum -= 10;
            shuffle(colors);
        }
        Texture tex = tile_map.block_texture;
        Rectanglef rect = U.popSetRect(0,0,8,6);
        Vector4f uvs = U.popSetVec4(0,0,1,1);
        for (int i = 0; i < 1; i++) {
            batch.draw(tex,rect,uvs,colors[i],15);
        } U.pushVec4();
        U.pushRect();

    }

    private void shuffle(int[] arr) {
        Random r = new Random();
        for (int i = arr.length - 1; i > 0; i--) {
            // Random index from 0 to i
            int j = r.nextInt(i + 1);
            // Swap elements at i and j
            int t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
    }
}
