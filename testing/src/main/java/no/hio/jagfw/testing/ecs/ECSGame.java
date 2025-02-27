package no.hio.jagfw.testing.ecs;

import io.github.heathensoft.jagfw.core.*;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.physics.Body;
import no.hio.jagfw.testing.ecs.components.AimingDirection;
import no.hio.jagfw.testing.ecs.components.PlayerTag;
import no.hio.jagfw.testing.ecs.systems.input.*;
import no.hio.jagfw.testing.ecs.systems.rendering.AimingVisualized;
import no.hio.jagfw.testing.ecs.systems.rendering.BodyRenderer;
import no.hio.jagfw.testing.ecs.systems.rendering.TileMapRender;
import no.hio.jagfw.testing.ecs.systems.update.PhysicsUpdate;

/**
 * Frederik Dahl 2/23/2025
 */
public class ECSGame extends Game {

    public static void main(String[] args) {
        Engine.get().run(new ECSGame(),args);
    }

    public static final int component_type_capacity = 32;
    public static final int entity_capacity = 256;
    public static final int game_res_w = 1280;
    public static final int game_res_h = 720;
    public static final float tile_size = 32;

    private ECS ecs; // entity component system
    private Context ecs_context; // shared system variables

    protected void configure(BootConfiguration boot_config, String[] args) {
        boot_config.window_title = "physics";
        boot_config.supported_resolutions.add(new Resolution(game_res_w,game_res_h));
        boot_config.windowed_mode_height = game_res_h;
        boot_config.windowed_mode_width = game_res_w;
        boot_config.windowed_mode = true;
        boot_config.resizable_window = true;
        boot_config.vsync_enabled = true;
        boot_config.target_ups = 120;
    }

    protected void start(Resolution resolution) throws Exception {
        ecs_context = new Context();
        ecs = new ECS(ecs_context,entity_capacity, component_type_capacity);
        ecs.addSystemToPipeline(new InputControl());
        ecs.addSystemToPipeline(new EditorInput());
        ecs.addSystemToPipeline(new PlayerControls());
        ecs.addSystemToPipeline(new CameraRefresh());
        ecs.addSystemToPipeline(new MouseUpdate());
        ecs.addSystemToPipeline(new PhysicsUpdate());
        ecs.addSystemToPipeline(new TileMapRender());
        ecs.addSystemToPipeline(new BodyRenderer());
        ecs.addSystemToPipeline(new AimingVisualized());


        int player = ecs.newEntity();
        ecs.addComponent(player,new Body(0,0,0.5f,100),true);
        ecs.addComponent(player,new PlayerTag(),true);
        ecs.addComponent(player,new AimingDirection(),true);

        //int player2 = ecs.newEntity();
        //ecs.addComponent(player2,new Body(2,2,0.5f,100),true);
        //ecs.addComponent(player2,new PlayerTag(),true);
        //ecs.addComponent(player2,new AimingDirection(),true);
        //int player3 = ecs.newEntity();
        //ecs.addComponent(player3,new Body(0,2,0.5f,100),true);
        //ecs.addComponent(player3,new PlayerTag(),true);
        //ecs.addComponent(player3,new AimingDirection(),true);
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
        Disposable.dispose(ecs,ecs_context);
    }
}
