package no.hio.jagfw.testing.ecs.systems;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.Mouse;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ECSystem;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.TileMap;
import org.joml.Vector2f;

/**
 * Frederik Dahl 2/27/2025
 */
public class Editor extends ECSystem {

    private final Vector2f camera_drag_origin = new Vector2f();

    protected void processSystem(ECS ecs, float dt) {
        Global global = ecs.getSharedContext(Global.class);
        if (global.editor_mode &!global.menu_mode) {
            Mouse mouse = Engine.get().window().mouse();
            WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
            if (mouse.isDragging(Mouse.WHEEL)) {
                if (mouse.justStartedDrag(Mouse.WHEEL)) {
                    camera_drag_origin.set(world_cam.camera.position);
                } Vector2f drag = U.popSetVec2((mouse.dragVector(Mouse.WHEEL)));
                world_cam.camera.unProjectVector(drag);
                world_cam.target_position.set(camera_drag_origin);
                world_cam.target_position.add(drag.negate());
                U.pushVec2();
            }
            TileMap tilemap = global.world.tilemap;
            PlayerInput input = ecs.getSystem(PlayerInput.class);
            Vector2f mouse_pos = input.mouse_position;
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
