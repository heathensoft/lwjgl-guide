package no.hio.jagfw.testing.ecs2.systems;

import io.github.heathensoft.jagfw.core.Controller;
import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Keyboard;
import io.github.heathensoft.jagfw.core.Mouse;
import io.github.heathensoft.jagfw.core.gfx.Bitmap;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs2.Context;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;

/**
 * Frederik Dahl 2/25/2025
 */
public class InputRead extends ECSystem {


    protected void processSystem(ECS ecs, float dt) {
        Keyboard keys = Engine.get().window().keys();
        Mouse mouse = Engine.get().window().mouse();
        Controller controller = Engine.get().window().controller();
        if (keys.justPressed(GLFW.GLFW_KEY_F1)) {
            ecs.signalToExit();
            return;
        }
        if (keys.justPressed(GLFW_KEY_F2)) {
            Bitmap bitmap = Framebuffer.screenshot();
            bitmap.compressToDisk("screenshot.png",true);
            bitmap.dispose();
        }

        Context context = ecs.getSharedContext(Context.class);

    }
}
