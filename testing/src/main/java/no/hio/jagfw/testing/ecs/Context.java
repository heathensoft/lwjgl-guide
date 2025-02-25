package no.hio.jagfw.testing.ecs;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Resolution;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.core.utils.Camera2D;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/23/2025
 */
public class Context implements Disposable {

    public SpriteBatch sprite_batch;
    public LineBatch line_batch;
    public Camera2D camera;
    public TileMap tilemap;
    public Vector2f mouse_position;
    public Vector2f camera_desired_position;
    public float player_force_scale = 4000;
    public float player_impulse_scale = 6000;
    public float body_friction_force = 300;
    public float body_drag_force = 20;
    public int aim_line_color = 0xFF0000FF;
    public float aim_controller_min = 1f;
    public float aim_controller_max = 8f;



    public Context() throws Exception {
        line_batch = new LineBatch(1024);
        line_batch.enableSmoothLines(true);
        line_batch.setLineWidth(2);
        sprite_batch = new SpriteBatch(1024);
        Resolution game_resolution = Engine.get().window().gameResolution();
        camera = new Camera2D(game_resolution,ECSGame.tile_size);
        mouse_position = new Vector2f();
        camera_desired_position = new Vector2f();
        tilemap = new TileMap(128,128);
    }

    public void dispose() {
        Disposable.dispose(sprite_batch,line_batch);
    }
}
