package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.utils.QuadTree;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Collision;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.components.Death;
import no.hio.jagfw.testing.ecs3.components.Dude;
import no.hio.jagfw.testing.ecs3.components.PhysicsResolution;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class CollisionResolver extends ProcessSystem {

    private final List<Dude> list = new ArrayList<>();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
        blocking_components.add(Death.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            Dude dude = ecs.getComponent(entity, Dude.class);
            if (dude != null && dude.resolution != PhysicsResolution.SLEEP) {
                DudeFilter dudes = ecs.getSystem(DudeFilter.class);
                if (dudes.dude_count > 1) {
                    // query area
                    Vector2f p = dude.position;
                    float r = dude.radius + dudes.dude_max_radius + 1;
                    dudes.quadtree.query(list,p.x,p.y,r);
                    if (!list.isEmpty()) {
                        for (Dude other : list) {
                            if (dude != other && !other.dying) {
                                if (dude.flying == other.flying) { // On same level
                                    Collision.resolveBodyBody(dude,other);
                                }
                            }
                        }
                        list.clear();
                    }
                }
                Collision.resolveBodyMap(dude,global.world.tilemap);
                Collision.resolveBodyGeometry(dude,global.world.bounds);
            }
        }
    }
}
