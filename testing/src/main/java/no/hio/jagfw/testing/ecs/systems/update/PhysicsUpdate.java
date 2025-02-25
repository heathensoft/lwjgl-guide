package no.hio.jagfw.testing.ecs.systems.update;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Body;
import io.github.heathensoft.jagfw.physics.Collision;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs.Context;
import no.hio.jagfw.testing.ecs.TileMap;


import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/23/2025
 */
public class PhysicsUpdate extends ProcessSystem {

    private final List<Body> bodies = new ArrayList<>();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Body.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Body body = ecs.getComponent(entity, Body.class);
        if (body != null) { bodies.add(body);
            Context context = ecs.getSharedContext(Context.class);
            PhysicsUtils.applyDrag(body,context.body_drag_force);
            PhysicsUtils.applyFriction(body,context.body_friction_force);
            body.update(dt);
        }
    }

    protected void postProcessing(ECS ecs, float dt) {
        if (!bodies.isEmpty()) {
            TileMap tilemap = ecs.getSharedContext(Context.class).tilemap;
            Collision.resolve(bodies,tilemap);
            bodies.clear();
        }
    }
}
