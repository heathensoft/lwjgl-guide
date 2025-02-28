package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.components.Dude;
import org.joml.primitives.Rectanglef;

import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class DudeRenderer extends ECSystem {

    private final List<Dude> list = new ArrayList<>();

    protected void renderSystem(ECS ecs, float alpha) {
        Global global = ecs.getSharedContext(Global.class);
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        DudeFilter dudes = ecs.getSystem(DudeFilter.class);
        if (dudes.dude_count > 0) {
            Rectanglef frustum = world_cam.camera.frustum;
            dudes.quadtree.query(list,frustum);
            if (!list.isEmpty()) {
                LineBatch batch = global.graphics.line_batch;
                batch.begin(world_cam.camera);
                for (Dude dude : list) {
                    int color;
                    switch (dude.disposition) {
                        case FRIENDLY -> color = 0xFF00FF00;
                        case NEUTRAL -> color = 0xFF00FFFF;
                        case HOSTILE -> color = 0xFF0000FF;
                        default -> color = 0xFFFFFFFF;
                    } PhysicsUtils.drawBody(dude,batch,color);
                    //batch.drawCircle(dude.position,16,32,0xFFFFFF00);
                } batch.end();
                list.clear();
            }
        }
    }

}
