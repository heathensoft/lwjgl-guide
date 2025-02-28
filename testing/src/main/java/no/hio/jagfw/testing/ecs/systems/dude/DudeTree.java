package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.core.utils.QuadTree;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.components.Dude;

import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class DudeTree extends ProcessSystem {

    public QuadTree<Dude> tree;

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
    }

    protected void preProcessing(ECS ecs, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        tree = new QuadTree<>(0,0,global.world.tilemap.size());
    }

    protected void process(ECS ecs, int entity, float dt) {
        Dude dude = ecs.getComponent(entity, Dude.class);
        if (dude != null) tree.insert(dude,dude.position);
    }
}
