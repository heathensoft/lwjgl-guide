package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import io.github.heathensoft.jagfw.physics.PhysicsUtils;
import no.hio.jagfw.testing.ecs.components.*;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.systems.PlayerInput;
import no.hio.jagfw.testing.ecs.systems.WorldCamera;
import org.joml.Math;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 2/27/2025
 */
public class DudeMovement extends ProcessSystem {

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
        //blocking_components.add(Death.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            Dude dude = ecs.getComponent(entity, Dude.class);
            if (dude != null && dude.resolution != PhysicsResolution.SLEEP) {
                Dude player = Dude.PLAYER;
                if (player == dude) {
                    playerMovement(ecs, dude,dt);
                } else if (ecs.getComponent(entity, Death.class) == null){
                    DudeInfo dude_info = ecs.getSystem(DudeInfo.class);
                    DudeTree dude_tree = ecs.getSystem(DudeTree.class);
                    Vector2f p = dude.position;
                    float r = dude.radius + dude_info.max_radius;
                    int surrounding = dude_tree.tree.query(p.x,p.y,r);
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
                                    dude.facing_direction.set(dude_to_player);
                                    dude_to_player.mul(dude.base_movement_force);
                                    dude.addForce(dude_to_player);
                                } U.pushVec2();
                            }
                        }
                    }
                }
                PhysicsUtils.applyDrag(dude,global.world.drag);
                PhysicsUtils.applyFriction(dude,global.world.friction); // grounded dudes
                dude.update(dt);
            }
        }
    }

    float rate_of_fire = 5 / 60f;
    float accum = 0f;
    protected void playerMovement(ECS ecs, Dude player, float dt) {
        PlayerInput input = ecs.getSystem(PlayerInput.class);
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        if (input.move_magnitude > 0) {
            Vector2f vec = U.popSetVec2(input.move_direction);
            vec.mul(input.move_magnitude);
            player.facing_direction.set(vec);
            vec.mul(player.base_movement_force);
            player.addForce(vec);
            if (input.action) {
                vec.normalize();
                vec.mul(player.base_dodge_impulse);
                player.applyImpulse(vec);
            } U.pushVec2();
        } world_cam.target_position.set(player.position);
        if (input.fire_default) {
            if (accum >= rate_of_fire) {
                System.out.println(ecs.numEntities());
                accum = 0f;
                int num_projectiles = 1;
                for (int i = 0; i < num_projectiles; i++) {
                    int entity = ecs.newEntity();
                    if (entity != -1) {
                        Projectile projectile = new Projectile(new ProjectileEffect() {
                            public void onHit(ECS ecs, int source, Dude target) {
                                Projectile p = ecs.getComponent(source, Projectile.class);
                                target.base_health -= 50;
                                ecs.deleteEntity(source);
                            }
                        });

                        projectile.velocity = 20f;
                        projectile.direction.set(input.aim_direction);
                        projectile.source_disposition = player.disposition;
                        projectile.position.set(player.position);
                        projectile.origin.set(player.position);
                        ecs.addComponent(entity,projectile,true);
                    }
                }

            }
        } accum += dt;
    }
}
