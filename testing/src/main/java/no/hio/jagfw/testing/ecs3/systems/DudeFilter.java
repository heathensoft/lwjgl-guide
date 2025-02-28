package no.hio.jagfw.testing.ecs3.systems;

import io.github.heathensoft.jagfw.core.Engine;
import io.github.heathensoft.jagfw.core.utils.QuadTree;
import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs3.Global;
import no.hio.jagfw.testing.ecs3.TileMap;
import no.hio.jagfw.testing.ecs3.components.Dude;
import no.hio.jagfw.testing.ecs3.components.PhysicsResolution;
import org.joml.Vector2f;

import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class DudeFilter extends ProcessSystem {


    public int dude_count;
    public float dude_max_radius;
    public QuadTree<Dude> quadtree;

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Dude.class);
    }

    protected void preProcessing(ECS ecs, float dt) {

        System.out.println(Engine.get().time().framesPerSecond());

        Global global = ecs.getSharedContext(Global.class);
        TileMap tileMap = global.world.tilemap;
        quadtree = new QuadTree<>(0,0,128); // todo: map size enum
        dude_max_radius = 0;
        dude_count = 0;
    }

    protected void process(ECS ecs, int entity, float dt) {
        Dude dude = ecs.getComponent(entity, Dude.class);
        WorldCamera world_cam = ecs.getSystem(WorldCamera.class);
        Vector2f camera_pos = world_cam.target_position;
        if (dude != null) {
            float d2 = camera_pos.distanceSquared(dude.position);
            if (d2 > U.square(PhysicsResolution.LOW.range)) {
                if (d2 > U.square(PhysicsResolution.SLEEP.range)) {
                    dude.resolution = PhysicsResolution.SLEEP;
                } else dude.resolution = PhysicsResolution.LOW;
            } else dude.resolution = PhysicsResolution.HIGH;
            quadtree.insert(dude,dude.position);
            dude_max_radius = Math.max(dude_max_radius,dude.radius);
            dude_count++;
        }
    }
}
