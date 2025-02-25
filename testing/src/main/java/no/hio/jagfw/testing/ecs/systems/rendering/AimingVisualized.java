package no.hio.jagfw.testing.ecs.systems.rendering;

import io.github.heathensoft.jagfw.core.utils.LineSegment;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.RenderSystem;
import io.github.heathensoft.jagfw.physics.Body;
import no.hio.jagfw.testing.ecs.Context;
import no.hio.jagfw.testing.ecs.components.AimingDirection;
import org.joml.Vector2f;


import java.util.List;

/**
 * Frederik Dahl 2/23/2025
 */
public class AimingVisualized extends RenderSystem {

    private final float line_length = 8f;

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Body.class);
        required_components.add(AimingDirection.class);
    }

    protected void preRender(ECS ecs, float alpha) {
        Context context = ecs.getSharedContext(Context.class);
        context.line_batch.begin(context.camera);
    }

    protected void render(ECS ecs, int entity, float alpha) {
        Body body = ecs.getComponent(entity, Body.class);
        AimingDirection aim_dir = ecs.getComponent(entity, AimingDirection.class);
        if (body != null && aim_dir != null) {
            Context context = ecs.getSharedContext(Context.class);
            Vector2f v = U.popSetVec2(aim_dir.direction).mul(line_length);
            LineSegment line = U.popLine();
            line.x0 = body.position.x;
            line.y0 = body.position.y;
            line.x1 = line.x0 + v.x;
            line.y1 = line.y0 + v.y;
            context.line_batch.drawLine(line,context.aim_line_color);
            U.pushLine();
            U.pushVec2();
        }

    }

    @Override
    protected void postRender(ECS ecs, float alpha) {
        Context context = ecs.getSharedContext(Context.class);
        context.line_batch.end();
    }
}
