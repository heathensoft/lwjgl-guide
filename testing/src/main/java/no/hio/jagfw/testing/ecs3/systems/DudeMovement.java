package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs3.components.Disposition;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.components.Death;
import no.hio.jagfw.testing.ecs3.components.Dude;
import no.hio.jagfw.testing.ecs3.components.PhysicsResolution;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 2/27/2025
 */
public class DudeMovement extends ProcessSystem {


    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
        blocking_components.add(Death.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            Dude dude = ecs.getComponent(entity, Dude.class);
            if (dude != null && dude.resolution != PhysicsResolution.SLEEP) {
                Dude player = Dude.PLAYER;
                if (player == dude) {
                    playerMovement(ecs, dude,dt);
                } else {

                    DudeFilter dudes = ecs.getSystem(DudeFilter.class);
                    Vector2f p = dude.position;
                    float r = dude.radius + dudes.dude_max_radius;
                    int surrounding = dudes.quadtree.query(p.x,p.y,r);
                    if (surrounding < 4) {
                        if (dude.disposition == Disposition.HOSTILE) {
                            if (player != null) {
                                Vector2f dude_to_player = U.popVec2();
                                dude_to_player.set(player.position);
                                dude_to_player.sub(dude.position);
                                float range = 2;
                                float dist2 = dude_to_player.lengthSquared();

                                float r2 = U.square(dude.radius + player.radius + 2);
                                if (dist2 > r2) {
                                    dude_to_player.normalize();
                                    dude_to_player.mul(dude.base_movement_force);
                                    dude.addForceControlled(dude_to_player);
                                    if (U.sqrt(dist2) < 4) {
                                        dude_to_player.normalize();
                                        dude_to_player.mul(dude.base_dodge_impulse / 32f);
                                        dude.applyImpulse(dude_to_player);
                                    }
                                }

                                U.pushVec2();
                            }
                        }
                    }

                }

                PhysicsUtils.applyDrag(dude,global.world.drag);
                PhysicsUtils.applyFriction(dude,global.world.friction);
                dude.update(dt);
            }
        }
    }

    protected void playerMovement(ECS ecs, Dude player, float dt) {
        PlayerInput input = ecs.getSystem(PlayerInput.class);
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        if (input.move_magnitude > 0) {
            Vector2f vec = U.popSetVec2(input.move_direction);
            vec.mul(input.move_magnitude);
            vec.mul(player.base_movement_force);
            player.addForceControlled(vec);
            if (input.action) {
                vec.normalize();
                vec.mul(player.base_dodge_impulse);
                player.applyImpulse(vec);
            } U.pushVec2();
        } world_cam.target_position.set(player.position);
    }
}
