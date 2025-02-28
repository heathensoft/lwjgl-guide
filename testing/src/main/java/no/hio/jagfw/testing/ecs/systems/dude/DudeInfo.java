package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs.components.Dude;

import java.util.List;

/**
 * Collecting usable data from dudes
 * Frederik Dahl 2/28/2025
 */
public class DudeInfo extends ProcessSystem {

    public float max_radius;
    public int dude_count;
    public int null_dudes;
    // *
    // *
    // *

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
    }


    protected void preProcessing(ECS ecs, float dt) {
        dude_count = 0;
        max_radius = 0;
        null_dudes = 0;
    }

    protected void process(ECS ecs, int entity, float dt) {
        Dude dude = ecs.getComponent(entity, Dude.class);
        if (dude != null) {
            max_radius = Math.max(max_radius,dude.radius);
            dude_count++;
        } else null_dudes++;

    }
}
