package no.hio.jagfw.testing.ecs.systems.input;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Mouse;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.Context;

/**
 * Frederik Dahl 2/23/2025
 */
public class MouseUpdate extends ECSystem {

    protected void processSystem(ECS ecs, float dt) {
        Context context = ecs.getSharedContext(Context.class);
        if (context != null) {
            Mouse mouse = Engine.get().window().mouse();
            context.mouse_position.set(mouse.position());
            context.camera.unProjectPosition(context.mouse_position);
        }
    }
}
