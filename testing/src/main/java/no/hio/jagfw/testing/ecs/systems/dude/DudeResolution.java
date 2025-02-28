package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs.components.Dude;
import no.hio.jagfw.testing.ecs.components.PhysicsResolution;
import no.hio.jagfw.testing.ecs.systems.WorldCamera;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class DudeResolution extends ProcessSystem {

    private final Vector2f center = new Vector2f();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
    }

    protected void preProcessing(ECS ecs, float dt) {
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        center.set(world_cam.target_position);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Dude dude = ecs.getComponent(entity, Dude.class);
        if (dude != null) {
            float d2 = center.distanceSquared(dude.position);
            if (d2 > U.square(PhysicsResolution.LOW.range)) {
                if (d2 > U.square(PhysicsResolution.SLEEP.range)) {
                    dude.resolution = PhysicsResolution.SLEEP;
                } else dude.resolution = PhysicsResolution.LOW;
            } else dude.resolution = PhysicsResolution.HIGH;
        }
    }
}
