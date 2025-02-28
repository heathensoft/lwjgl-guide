package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.core.gfx.LineBatch;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.components.Dude;
import no.hio.jagfw.testing.ecs.systems.WorldCamera;
import org.joml.primitives.Rectanglef;

import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class DudeRenderer extends ECSystem {

    private final List<Dude> list = new ArrayList<>();

    protected void renderSystem(ECS ecs, float alpha) {
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        Rectanglef frustum = world_cam.camera.frustum;
        DudeTree dude_tree = ecs.getSystem(DudeTree.class);
        dude_tree.tree.query(list,frustum);
        if (!list.isEmpty()) {
            Global global = ecs.getSharedContext(Global.class);
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
