package no.hio.jagfw.testing.ecs.systems.input;

import io.github.heathensoft.jagfw.core.Controller;
import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Keyboard;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Body;
import no.hio.jagfw.testing.ecs.Context;
import no.hio.jagfw.testing.ecs.components.AimingDirection;
import no.hio.jagfw.testing.ecs.components.PlayerTag;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Frederik Dahl 2/24/2025
 */
public class PlayerControls extends ProcessSystem {

    private final Vector2f player_move_direction = new Vector2f();
    private final Vector2f player_average_position = new Vector2f();
    private final Vector2f controller_aim_direction = new Vector2f(0,-1);
    private final List<Integer> post_processing_list = new ArrayList<>();
    private float player_movement_magnitude;
    private boolean player_dodge_impulse;
    private boolean controller_connected;


    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(PlayerTag.class);
        required_components.add(Body.class);
        required_components.add(AimingDirection.class);
    }

    protected void preProcessing(ECS ecs, float dt) {
        Keyboard keys = Engine.get().window().keys();
        Controller controller = Engine.get().window().controller();
        Context context = ecs.getSharedContext(Context.class);

        player_move_direction.zero();
        player_average_position.zero();
        player_movement_magnitude = 0;
        player_dodge_impulse = false;

        controller.setActiveSlot(0);
        if (controller.isConnected()) {
            if (!controller_connected) {
                Engine.get().window().showCursor(false);
                controller_connected = true;
            }

            if (controller.leftStickPushed()) {
                player_movement_magnitude = controller.leftStickMagnitude();
                player_move_direction.set(controller.leftStickDirection());
                if (controller.buttonJustPressed(Controller.BUTTON_CROSS)) {
                    player_dodge_impulse = true;
                }
            }
            if (controller.rightStickPushed()) {
                controller_aim_direction.set(controller.rightStickDirection());
            }
        }
        else {
            if (controller_connected) {
                Engine.get().window().showCursor(true);
                controller_connected = false;
            }

            if (keys.pressed(GLFW_KEY_A)) {
                player_move_direction.add(-1,0);
            } if (keys.pressed(GLFW_KEY_S)) {
                player_move_direction.add(0,-1);
            } if (keys.pressed(GLFW_KEY_D)) {
                player_move_direction.add(1,0);
            } if (keys.pressed(GLFW_KEY_W)) {
                player_move_direction.add(0,1);
            }

            if (player_move_direction.lengthSquared() > 0) {
                player_movement_magnitude = 1.0f;
                player_move_direction.normalize();
                if (keys.justPressed(GLFW_KEY_SPACE)) {
                    player_dodge_impulse = true;
                }
            }
        }
    }

    protected void process(ECS ecs, int entity, float dt) {
        Body body = ecs.getComponent(entity, Body.class);
        AimingDirection aim_dir = ecs.getComponent(entity, AimingDirection.class);
        PlayerTag player_tag = ecs.getComponent(entity, PlayerTag.class);
        if (body != null && aim_dir != null && player_tag != null) {
            applyControlledForces(body,player_tag);
            player_average_position.add(body.position);
            post_processing_list.add(entity);
        }
    }

    protected void postProcessing(ECS ecs, float dt) {
        if (!post_processing_list.isEmpty()) {
            Context context = ecs.getSharedContext(Context.class);
            player_average_position.div(post_processing_list.size());
            context.camera_desired_position.set(player_average_position);
            Vector2f aim_direction = U.popVec2();
            if (controller_connected) {
                aim_direction.set(controller_aim_direction);
            } else {
                aim_direction.set(context.mouse_position);
                aim_direction.sub(player_average_position).normalize();
            }
            for (Integer entity : post_processing_list) {
                AimingDirection player_aim = ecs.getComponent(entity, AimingDirection.class);
                player_aim.direction.set(aim_direction);
            }
            U.pushVec2();
            post_processing_list.clear();
        }
    }

    private void applyControlledForces(Body body, PlayerTag player_tag) {
        if (player_movement_magnitude > 0) {
            Vector2f v = U.popVec2();
            v.set(player_move_direction).mul(player_tag.input_movement_force);
            if (v.isFinite()) body.addForceControlled(v);
            if (player_dodge_impulse) {
                v.set(player_move_direction).mul(player_tag.input_dodge_impulse);
                if (v.isFinite()) body.applyImpulse(v);
            } U.pushVec2();
        }

    }
}
