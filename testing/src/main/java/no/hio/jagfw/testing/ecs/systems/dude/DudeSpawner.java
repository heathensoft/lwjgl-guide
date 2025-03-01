package no.hio.jagfw.testing.ecs.systems.dude;

import io.github.heathensoft.jagfw.core.utils.Rand;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.components.Disposition;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.TileMap;
import no.hio.jagfw.testing.ecs.components.Dude;

/**
 * Frederik Dahl 2/27/2025
 */
public class DudeSpawner extends ECSystem {

    public float spawn_timer = 0f;
    public float spawn_interval = 2f;
    public float spawn_time_accumulator = 0f;

    public int enemies_to_spawn = 5;
    public int num_spawn_attempts = 10;
    public int num_failed_to_spawn = 0;
    public int dude_cap = 10000; // --- global instead

    protected void processSystem(ECS ecs, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (!(global.menu_mode || global.editor_mode)) {
            TileMap tilemap = global.world.tilemap;
            DudeTree dude_tree = ecs.getSystem(DudeTree.class);
            DudeInfo dude_info = ecs.getSystem(DudeInfo.class);
            spawn_timer += dt;
            spawn_time_accumulator += dt;
            while (spawn_time_accumulator >= spawn_interval) {
                spawn_time_accumulator -= spawn_interval;
                int spawn_limit = dude_cap - dude_info.dude_count;
                int spawn_count = Math.min(spawn_limit,enemies_to_spawn);
                for (int i = 0; i < spawn_count; i++) {
                    boolean success = false;
                    for (int j = 0; j < num_spawn_attempts; j++) {
                        int spawn_x = Rand.nextInt(tilemap.width() - 1);
                        int spawn_y = Rand.nextInt(tilemap.height() - 1);
                        if (!tilemap.isBlock(spawn_x,spawn_y)) {
                            // flood fill here
                            int entity = ecs.newEntity();
                            if (entity != -1) {
                                Dude dude = new Dude(
                                        spawn_x + 0.5f,
                                        spawn_y + 0.5f,
                                        0.5f,
                                        50, Disposition.HOSTILE);

                                if (Rand.nextFloat() > 0.7) {
                                    dude.base_movement_force = 2000f;
                                    dude.base_health = 200f;
                                    dude.base_max_health = 200f;
                                } else  {
                                    dude.base_movement_force = 1000f;
                                    dude.base_health = 100f;
                                    dude.base_max_health = 100f;
                                }
                                ecs.addComponent(entity,dude,true);
                            } success = true;
                            break;
                        }
                    }
                    if (!success) num_failed_to_spawn++;
                }
            }
        }
    }

    public float timeToSpawn() {
        return spawn_interval - spawn_time_accumulator;
    }
}
