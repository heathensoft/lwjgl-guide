package no.hio.jagfw.testing.ecs.systems.rendering;

import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.RenderSystem;
import io.github.heathensoft.jagfw.physics.Body;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs.Context;

import java.util.List;

/**
 * Frederik Dahl 2/23/2025
 */
public class BodyRenderer extends RenderSystem {


    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Body.class);
    }

    protected void preRender(ECS ecs, float alpha) {
        Context context = ecs.getSharedContext(Context.class);
        context.line_batch.begin(context.camera);
    }

    protected void render(ECS ecs, int entity, float alpha) {
        Body body = ecs.getComponent(entity, Body.class);
        Context context = ecs.getSharedContext(Context.class);
        if (body != null) PhysicsUtils.drawBody(body,context.line_batch);
    }


    protected void postRender(ECS ecs, float alpha) {
        Context context = ecs.getSharedContext(Context.class);
        context.line_batch.end();
    }
}
