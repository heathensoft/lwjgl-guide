package no.hio.jagfw.testing.ecs.systems;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Resolution;
import io.github.heathensoft.jagfw.core.utils.Camera2D;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.Global;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/27/2025
 */
public class WorldCamera extends ECSystem {

    public final Vector2f target_position;
    public final Camera2D camera;
    public final int tile_size = 32;
    private float target_zoom;

    public WorldCamera(Vector2f position) {
        Resolution resolution = Engine.get().window().gameResolution();
        target_position = new Vector2f(position);
        camera = new Camera2D(resolution,tile_size);
        camera.position.set(position);
        camera.refresh();
    }

    protected void processSystem(ECS ecs, float dt) {
        PlayerInput input = ecs.getSystem(PlayerInput.class);
        Global global = ecs.getSharedContext(Global.class);
        if (input.zoom_amount != 0 && !global.menu_mode) {
            target_zoom += input.zoom_amount;
            target_zoom = U.clamp(target_zoom,camera.zoom_min,camera.zoom_max);
        } camera.zoom(target_zoom,dt);
        camera.follow(target_position,dt);
        camera.refresh();
    }

    protected void resizeEvent(ECS ecs, Resolution resolution) {
        camera.updateViewport(resolution,tile_size);
        camera.refresh();
    }
}
