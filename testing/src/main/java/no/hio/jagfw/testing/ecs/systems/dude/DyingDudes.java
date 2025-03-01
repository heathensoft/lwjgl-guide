package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.components.Death;
import no.hio.jagfw.testing.ecs.components.Dude;

import java.util.List;

/**
 * Frederik Dahl 2/27/2025
 */
public class DyingDudes extends ProcessSystem {


    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            Dude dude = ecs.getComponent(entity, Dude.class);
            Death death = ecs.getComponent(entity, Death.class);
            if (dude != null) {
                if (death == null) {
                    if (dude.base_health <= 0) {
                        ecs.addComponent(entity,new Death(),false);
                    }
                } else {
                    death.time_to_die -= dt;
                    if (death.time_to_die <= 0) {
                        if (dude.isPlayer()) {
                            Dude.PLAYER = null;
                            ecs.signalToExit();
                            // how to handle exit? saving etc.
                        } else {
                            // span exp orbs
                            global.player_state.experience_points += dude.base_experience_yield;
                        }
                        ecs.deleteEntity(entity);
                    }
                }

            }
        }

    }
}
