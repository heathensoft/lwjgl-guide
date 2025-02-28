package no.hio.jagfw.testing.ecs;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.tiles.MapSize;

/**
 * Frederik Dahl 2/27/2025
 */
public class Global implements Disposable {

    public final PlayerState player_state;
    public final Graphics graphics;
    public final World world;
    public boolean controller_connected;
    public boolean editor_mode;
    public boolean menu_mode; // game pause

    public Global(MapSize map_size) throws Exception {
        graphics = new Graphics();
        player_state = new PlayerState();
        world = new World(new TileMap(map_size));
    }

    public void dispose() {
        Disposable.dispose(graphics);
    }

    public static final class Graphics implements Disposable {
        public final SpriteBatch sprite_batch;
        public final LineBatch line_batch;
        Graphics() throws Exception {
            sprite_batch = new SpriteBatch(1024);
            line_batch = new LineBatch(2048);
            line_batch.enableSmoothLines(true);
            line_batch.setLineWidth(2);
        } public void dispose() {
            Disposable.dispose(sprite_batch,line_batch);
        }
    }

}
