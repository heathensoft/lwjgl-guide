package no.hio.jagfw.testing.ecs.systems.input;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.Context;

/**
 * Frederik Dahl 2/23/2025
 */
public class CameraRefresh extends ECSystem {

    private boolean currently_zooming;
    private float zoom_timer;
    private float zoom_current;
    private float zoom_target;
    private final float zoom_min = -2.0f;
    private final float zoom_speed = 1.5f;


    protected void processSystem(ECS ecs, float dt) {
        Context context = ecs.getSharedContext(Context.class);
        context.camera.follow(context.camera_desired_position,dt);
        context.camera.refresh();

    }
}
