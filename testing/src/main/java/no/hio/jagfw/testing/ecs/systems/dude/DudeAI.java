package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs.components.Dude;
import no.hio.jagfw.testing.ecs.components.PlayerTag;

import java.util.List;

/**
 * Frederik Dahl 3/1/2025
 */
public class DudeAI extends ProcessSystem {


    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
        blocking_components.add(PlayerTag.class);
    }

    protected void process(ECS ecs, int entity, float dt) {

    }
}
