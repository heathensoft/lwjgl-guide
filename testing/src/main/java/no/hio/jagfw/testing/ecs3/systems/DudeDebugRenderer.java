package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.RenderSystem;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.components.Dude;

import java.util.List;

/**
 * Frederik Dahl 2/27/2025
 */
public class DudeDebugRenderer extends RenderSystem {


    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
    }

    protected void preRender(ECS ecs, float alpha) {
        Global global = ecs.getSharedContext(Global.class);
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        LineBatch batch = global.graphics.line_batch;
        batch.begin(world_cam.camera);
    }

    protected void render(ECS ecs, int entity, float alpha) {
        Dude dude = ecs.getComponent(entity,Dude.class);
        Global global = ecs.getSharedContext(Global.class);
        LineBatch batch = global.graphics.line_batch;
        if (dude != null) {
            int color;
            switch (dude.disposition) {
                case FRIENDLY -> color = 0xFF00FF00;
                case NEUTRAL -> color = 0xFF00FFFF;
                case HOSTILE -> color = 0xFF0000FF;
                default -> color = 0xFFFFFFFF;
            }
            PhysicsUtils.drawBody(dude,batch,color);
            //batch.drawCircle(dude.position,16,32,0xFFFFFF00);
        }
    }

    protected void postRender(ECS ecs, float alpha) {
        Global global = ecs.getSharedContext(Global.class);
        LineBatch batch = global.graphics.line_batch;
        batch.end();
    }
}
