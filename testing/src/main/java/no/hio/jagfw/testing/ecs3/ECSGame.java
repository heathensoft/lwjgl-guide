package no.hio.jagfw.testing.ecs3;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.ecs.ECS;
import no.hio.jagfw.testing.ecs3.components.Disposition;
import no.hio.jagfw.testing.ecs3.components.Dude;
import no.hio.jagfw.testing.ecs3.systems.*;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/23/2025
 */
public class ECSGame extends Game {

    public static void main(String[] args) {
        Engine.get().run(new ECSGame(),args);
    }

    public static final int component_type_capacity = 32;
    public static final int entity_capacity = 1024;
    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;

    private ECS ecs; // entity component system
    private Global ecs_global;

    protected void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "physics";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = false;
        boot_config.target_ups = 120;
    }

    protected void start(Resolution resolution) throws Exception {
        Vector2f start_position = new Vector2f(4,4);
        ecs_global = new Global();
        ecs = new ECS(ecs_global,entity_capacity,component_type_capacity);
        ecs.addSystemToPipeline(new PlayerInput());
        ecs.addSystemToPipeline(new Editor());
        ecs.addSystemToPipeline(new DudeGenerator());
        ecs.addSystemToPipeline(new DudeFilter());
        ecs.addSystemToPipeline(new DudeMovement());
        ecs.addSystemToPipeline(new DudeCollisions());
        ecs.addSystemToPipeline(new WorldCamera(start_position));
        ecs.addSystemToPipeline(new DyingDudes());
        ecs.addSystemToPipeline(new TileRenderer());
        ecs.addSystemToPipeline(new DudeRenderer());

        int player = ecs.newEntity();
        Dude dude = new Dude(
                start_position.x,
                start_position.y,
                0.5f,
                100, Disposition.FRIENDLY);
        dude.base_health = 100f;
        dude.base_max_health = 100f;
        dude.base_movement_force = 4000f;
        dude.base_dodge_impulse = 6000f;
        dude.setPlayer();
        ecs.addComponent(player,dude,true);
    }

    protected void resize(Resolution resolution) {
        ecs.resizeEvent(resolution);
    }

    protected void update(float delta_time) {
        if (ecs.shouldExit()) {
            Engine.get().exitMainLoop();
        } else ecs.update(delta_time);
    }

    protected void render(float alpha) {
        Framebuffer.bindDefault();
        Framebuffer.viewport();
        ecs.render(alpha);
    }

    protected void exit() {
        Disposable.dispose(ecs,ecs_global);
    }
}
