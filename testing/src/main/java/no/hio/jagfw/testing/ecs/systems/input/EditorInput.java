package no.hio.jagfw.testing.ecs.systems.input;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Mouse;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.Context;
import no.hio.jagfw.testing.ecs.TileMap;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/23/2025
 */
public class EditorInput extends ECSystem {

    private final Vector2f camera_drag_origin = new Vector2f();

    protected void processSystem(ECS ecs, float dt) {
        Mouse mouse = Engine.get().window().mouse();
        Context context = ecs.getSharedContext(Context.class);
        if (context != null) {
            if (mouse.isDragging(Mouse.WHEEL)) {
                if (mouse.justStartedDrag(Mouse.WHEEL)) {
                    camera_drag_origin.set(context.camera.position);
                } Vector2f drag = U.popSetVec2((mouse.dragVector(Mouse.WHEEL)));
                context.camera.unProjectVector(drag);
                context.camera_desired_position.set(camera_drag_origin);
                context.camera_desired_position.add(drag.negate());
                U.pushVec2();
            }
            TileMap tilemap = context.tilemap;
            Vector2f mouse_pos = context.mouse_position;
            if (mouse.buttonPressed(Mouse.LEFT)) {
                if (tilemap.contains(mouse_pos)) {
                    tilemap.placeBlock(U.floor(mouse_pos.x),U.floor(mouse_pos.y));
                }
            } else if (mouse.buttonPressed(Mouse.RIGHT)) {
                if (tilemap.contains(mouse_pos)) {
                    tilemap.removeBlock(U.floor(mouse_pos.x),U.floor(mouse_pos.y));
                }
            }
        }

    }
}
