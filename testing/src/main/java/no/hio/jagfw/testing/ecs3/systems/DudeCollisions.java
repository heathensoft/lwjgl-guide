package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Collision;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.World;
import no.hio.jagfw.testing.ecs3.components.Death;
import no.hio.jagfw.testing.ecs3.components.Dude;
import no.hio.jagfw.testing.ecs3.components.PhysicsResolution;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/27/2025
 */
public class DudeCollisions extends ProcessSystem {

    private final List<Dude> ground_bodies = new ArrayList<>();
    private final List<Dude> flying_bodies = new ArrayList<>();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
        blocking_components.add(Death.class);
    }

    protected void process(ECS ecs, int entity, float dt) {

        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            Dude dude = ecs.getComponent(entity, Dude.class);
            if (dude != null && dude.resolution != PhysicsResolution.SLEEP) {
                if (dude.flying) flying_bodies.add(dude);
                else ground_bodies.add(dude);
            }
        }
    }

    protected void postProcessing(ECS ecs, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            World world = global.world;
            if (!ground_bodies.isEmpty()) {
                int num_bodies = ground_bodies.size();
                for (int i = 0; i < num_bodies; i++) {
                    Dude A = ground_bodies.get(i);
                    for (int j = i + 1; j < num_bodies; j++) {
                        Dude B = ground_bodies.get(j);
                        Collision.resolveBodyBody(A,B);
                    } Collision.resolveBodyMap(A, world.tilemap);
                    Collision.resolveBodyGeometry(A,world.bounds);
                } ground_bodies.clear();
            } if (!flying_bodies.isEmpty()) {
                int num_bodies = flying_bodies.size();
                for (int i = 0; i < num_bodies; i++) {
                    Dude A = flying_bodies.get(i);
                    for (int j = i + 1; j < num_bodies; j++) {
                        Dude B = flying_bodies.get(j);
                        Collision.resolveBodyBody(A,B);
                    } Collision.resolveBodyGeometry(A,world.bounds);
                } flying_bodies.clear();
            }
        }
    }
}
