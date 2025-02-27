package no.hio.jagfw.testing.ecs2.systems;

import io.github.heathensoft.jagfw.core.Resolution;
import io.github.heathensoft.jagfw.core.utils.Camera2D;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs2.Context;

/**
 * Frederik Dahl 2/25/2025
 */
public class CameraUpdate extends ECSystem {

    private float zoom_target;

    protected void processSystem(ECS ecs, float dt) {
        Context context = ecs.getSharedContext(Context.class);
        Context.PlayerInput input = context.player_input;
        Camera2D camera = context.world_cam.camera;
        if (input.zoom_amount != 0 && !context.global_flags.menu_mode) {
            zoom_target += input.zoom_amount;
            zoom_target = U.clamp(zoom_target,camera.zoom_min,camera.zoom_max);
        } camera.zoom(zoom_target,dt);
        camera.follow(context.world_cam.desired_position,dt);
        camera.refresh();
    }

    protected void resizeEvent(ECS ecs, Resolution resolution) {
        Context context = ecs.getSharedContext(Context.class);
        context.world_cam.camera.updateViewport(resolution,context.TILE_SIZE);
        context.world_cam.camera.refresh();
    }
}
