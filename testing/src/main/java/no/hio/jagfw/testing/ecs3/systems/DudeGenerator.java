package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.utils.Rand;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs3.components.Disposition;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.TileMap;
import no.hio.jagfw.testing.ecs3.components.Dude;

/**
 * Frederik Dahl 2/27/2025
 */
public class DudeGenerator extends ECSystem {

    public float spawn_timer = 0f;
    public float spawn_interval = 1f;
    public float spawn_time_accumulator = 0f;

    public int enemies_to_spawn = 500;
    public int num_spawn_attempts = 10;
    public int num_failed_to_spawn = 0;
    public int max_dudes = 10000; // --- global instead

    protected void processSystem(ECS ecs, float dt) {

        DudeFilter dudes = ecs.getSystem(DudeFilter.class); // ----

        Global global = ecs.getSharedContext(Global.class);
        TileMap tilemap = global.world.tilemap;
        if (!(global.menu_mode || global.editor_mode)) {
            spawn_timer += dt;
            spawn_time_accumulator += dt;
            while (spawn_time_accumulator >= spawn_interval) {
                spawn_time_accumulator -= spawn_interval;
                int num_entities = dudes.dude_count;
                int max_num_to_spawn = max_dudes - num_entities;
                int to_spawn = Math.min(max_num_to_spawn,enemies_to_spawn);
                for (int i = 0; i < to_spawn; i++) {
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
                                dude.base_health = 100f;
                                dude.base_max_health = 100f;
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
