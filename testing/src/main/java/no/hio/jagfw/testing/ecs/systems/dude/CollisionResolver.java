package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Collision;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.World;
import no.hio.jagfw.testing.ecs.components.Death;
import no.hio.jagfw.testing.ecs.components.Dude;
import no.hio.jagfw.testing.ecs.components.PhysicsResolution;

import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/27/2025
 */
public class CollisionResolver extends ProcessSystem {

    private final List<Dude> ground_dudes = new ArrayList<>();
    private final List<Dude> flying_dudes = new ArrayList<>();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
        blocking_components.add(Death.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            Dude dude = ecs.getComponent(entity, Dude.class);
            if (dude != null && dude.resolution != PhysicsResolution.SLEEP) {
                if (dude.flying) flying_dudes.add(dude);
                else ground_dudes.add(dude);
            }
        }
    }

    protected void postProcessing(ECS ecs, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            World world = global.world;
            if (!ground_dudes.isEmpty()) {
                int num_bodies = ground_dudes.size();
                for (int i = 0; i < num_bodies; i++) {
                    Dude A = ground_dudes.get(i);
                    for (int j = i + 1; j < num_bodies; j++) {
                        Dude B = ground_dudes.get(j);
                        Collision.resolveBodyBody(A,B);
                    } Collision.resolveBodyMap(A, world.tilemap);
                    Collision.resolveBodyGeometry(A,world.bounds);
                } ground_dudes.clear();
            } if (!flying_dudes.isEmpty()) {
                int num_bodies = flying_dudes.size();
                for (int i = 0; i < num_bodies; i++) {
                    Dude A = flying_dudes.get(i);
                    for (int j = i + 1; j < num_bodies; j++) {
                        Dude B = flying_dudes.get(j);
                        Collision.resolveBodyBody(A,B);
                    } Collision.resolveBodyGeometry(A,world.bounds);
                } flying_dudes.clear();
            }
        }
    }
}
