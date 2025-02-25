package no.hio.jagfw.testing.ecs.systems.input;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Keyboard;
import io.github.heathensoft.jagfw.core.gfx.Bitmap;
import io.github.heathensoft.jagfw.core.gfx.Framebuffer;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import org.lwjgl.glfw.GLFW;
import org.tinylog.Logger;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;

/**
 * Frederik Dahl 2/23/2025
 */
public class InputControl extends ECSystem {

    private boolean editor_mode = true;

    protected void processSystem(ECS ecs, float dt) {
        Keyboard keys = Engine.get().window().keys();
        if (keys.justPressed(GLFW.GLFW_KEY_ESCAPE)) {
            ecs.signalToExit();
            return;
        }
        PlayerControls player_input = ecs.getSystem(PlayerControls.class);
        EditorInput editor_input = ecs.getSystem(EditorInput.class);

        if (player_input == null || editor_input == null) {
            Logger.warn("missing system: player input system or editor input system");
            ecs.signalToExit();
            return;
        }

        if (keys.justPressed(GLFW.GLFW_KEY_F1)) {
            editor_mode = !editor_mode;
        }
        if (editor_mode) {
            player_input.pause();
            editor_input.unpause();
        } else {
            player_input.unpause();
            editor_input.pause();
        }

        if (keys.justPressed(GLFW_KEY_F2)) {
            Bitmap bitmap = Framebuffer.screenshot();
            bitmap.compressToDisk("screenshot.png",true);
            bitmap.dispose();
        }
    }

    public boolean editorModeEnabled() {
        return editor_mode;
    }

}
