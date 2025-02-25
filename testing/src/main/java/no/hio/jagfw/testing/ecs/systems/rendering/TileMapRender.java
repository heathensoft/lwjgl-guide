package no.hio.jagfw.testing.ecs.systems.rendering;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.Background;
import no.hio.jagfw.testing.ecs.Context;

/**
 * Frederik Dahl 2/23/2025
 */
public class TileMapRender extends ECSystem {

    private final Background background;

    public TileMapRender() throws Exception {
        background = new Background();
    }

    protected void renderSystem(ECS ecs, float alpha) {
        Context context = ecs.getSharedContext(Context.class);
        if (context != null) {
            background.draw(context.camera);
            context.sprite_batch.begin(context.camera);
            context.tilemap.render(context.sprite_batch);
            context.sprite_batch.end();
        }
    }

    public void dispose() {
        Disposable.dispose(background);
    }
}
