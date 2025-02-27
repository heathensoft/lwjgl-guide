package no.hio.jagfw.testing.ecs2.systems;

import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Body;
import no.hio.jagfw.testing.ecs2.Context;
import no.hio.jagfw.testing.ecs2.components.PlayerTag;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 2/26/2025
 */
public class PlayerMovement extends ProcessSystem {



    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(PlayerTag.class);
        required_components.add(Body.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Context context = ecs.getSharedContext(Context.class);
        if (!(context.global_flags.menu_mode || context.global_flags.editor_mode)) {
            Body body = ecs.getComponent(entity, Body.class);
            if (body != null) {
                Context.PlayerVariables player_vars = context.player_vars;
                Context.PlayerInput input = context.player_input;
                if (input.move_strength > 0) {
                    Vector2f vec = U.popSetVec2(input.move_direction);
                    vec.mul(input.move_strength);
                    vec.mul(player_vars.movement_force);
                    body.addForceControlled(vec);
                    if (input.action) {
                        vec.normalize();
                        vec.mul(player_vars.dodge_impulse);
                        body.applyImpulse(vec);
                    } U.pushVec2();
                }
            }
        }
    }
}
