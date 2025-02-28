package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.Background;
import no.hio.jagfw.testing.ecs3.Global;

/**
 * Frederik Dahl 2/27/2025
 */
public class TileRenderer extends ECSystem {

    private final Background background;

    public TileRenderer() throws Exception {
        background = new Background(128,128);
    }

    protected void renderSystem(ECS ecs, float alpha) {
        Global global = ecs.getSharedContext(Global.class);
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        Global.Graphics graphics = global.graphics;
        background.draw(world_cam.camera);
        graphics.sprite_batch.begin(world_cam.camera);
        global.world.tilemap.render(graphics.sprite_batch);
        graphics.sprite_batch.end();
    }


    public void dispose() {
        Disposable.dispose(background);
    }
}
