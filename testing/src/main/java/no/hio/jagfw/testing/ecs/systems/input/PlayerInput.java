package no.hio.jagfw.testing.ecs.systems.input;

import io.github.heathensoft.jagfw.core.Controller;
import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Keyboard;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.Body;
import no.hio.jagfw.testing.ecs.Context;
import no.hio.jagfw.testing.ecs.components.PlayerTag;
import org.joml.Math;
import org.joml.Vector2f;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Frederik Dahl 2/23/2025
 */
public class PlayerInput extends ProcessSystem {

    private final Vector2f player_force = new Vector2f();
    private final Vector2f player_impulse = new Vector2f();
    private final Vector2f average_position = new Vector2f();

    private final Vector2f controller_aim_dir = new Vector2f(0,-1);
    private float controller_aim_strength;
    private boolean controller_connected;

    private int entity_count;

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(PlayerTag.class);
        required_components.add(Body.class);
    }

    protected void preProcessing(ECS ecs, float dt) {
        Keyboard keys = Engine.get().window().keys();
        Controller controller = Engine.get().window().controller();
        Context context = ecs.getSharedContext(Context.class);
        entity_count = 0;
        player_force.zero();
        player_impulse.zero();
        average_position.zero();
        if (context != null) {
            controller.setActiveSlot(0);
            if (controller.isConnected()) {
                controller_connected = true;
                if (controller.leftStickPushed()) {
                    float strength = controller.leftStickMagnitude();
                    strength *= context.player_force_scale;
                    player_force.set(controller.leftStickDirection());
                    player_force.mul(strength);
                    if (player_force.lengthSquared() > 0) {
                        if (controller.buttonJustPressed(Controller.BUTTON_CROSS)) {
                            player_impulse.set(player_force).normalize();
                            player_impulse.mul(context.player_impulse_scale);
                        }

                    }
                } if (controller.rightStickPushed()) {
                    controller_aim_strength = controller.rightStickMagnitude();
                    controller_aim_dir.set(controller.rightStickDirection());
                } else controller_aim_strength = 0;
            } else {
                controller_connected = false;
                if (keys.pressed(GLFW_KEY_A)) {
                    player_force.add(-context.player_force_scale,0);
                } if (keys.pressed(GLFW_KEY_S)) {
                    player_force.add(0,-context.player_force_scale);
                } if (keys.pressed(GLFW_KEY_D)) {
                    player_force.add(context.player_force_scale,0);
                } if (keys.pressed(GLFW_KEY_W)) {
                    player_force.add(0,context.player_force_scale);
                } if (player_force.lengthSquared() > 0) {
                    if (keys.justPressed(GLFW_KEY_SPACE)) {
                        player_impulse.set(player_force).normalize();
                        player_impulse.mul(context.player_impulse_scale);
                    }
                }
            }
        }
    }

    protected void process(ECS ecs, int entity, float dt) {
        Body body = ecs.getComponent(entity, Body.class);
        PlayerTag player_tag = ecs.getComponent(entity, PlayerTag.class);
        if (body != null && player_tag != null) {
            body.applyImpulse(player_impulse);
            body.addForceControlled(player_force);
            average_position.add(body.position);
            entity_count++;
            float range = Math.max(body.radius,player_tag.aim_range_limit);
            if (controller_connected) {
                float t = U.lerp(body.radius,range,U.smooth(U.pow(controller_aim_strength,3)));
                player_tag.aim_vector.set(controller_aim_dir).mul(t);
            } else {
                Context context = ecs.getSharedContext(Context.class);
                Vector2f body_to_mouse = U.popSetVec2(context.mouse_position);
                body_to_mouse.sub(body.position);
                float t = body_to_mouse.length();
                t = U.clamp(t,body.radius,range);
                body_to_mouse.normalize();
                if (!body_to_mouse.isFinite()) {
                    body_to_mouse.set(0,-1);
                } player_tag.aim_vector.set(body_to_mouse).mul(t);
                U.pushVec2();
            }
        }
    }

    protected void postProcessing(ECS ecs, float dt) {
        if (entity_count > 0) {
            Context context = ecs.getSharedContext(Context.class);
            if (context != null) {
                average_position.div(entity_count);
                context.camera_desired_position.set(average_position);
            }
        }
        average_position.zero();
        player_impulse.zero();
        player_force.zero();
        entity_count = 0;
    }
}
