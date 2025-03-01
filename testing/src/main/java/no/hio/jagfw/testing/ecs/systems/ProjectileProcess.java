package no.hio.jagfw.testing.ecs.systems;

import io.github.heathensoft.jagfw.core.utils.U;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.ProcessSystem;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.TileMap;
import no.hio.jagfw.testing.ecs.components.Dude;
import no.hio.jagfw.testing.ecs.components.Projectile;
import no.hio.jagfw.testing.ecs.systems.dude.DudeInfo;
import no.hio.jagfw.testing.ecs.systems.dude.DudeTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class ProjectileProcess extends ProcessSystem {

    private final List<Dude> potential_targets = new ArrayList<>();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Projectile.class);
    }

    protected void process(ECS ecs, int entity, float dt) {
        Projectile projectile = ecs.getComponent(entity, Projectile.class);
        if (projectile != null) {
            projectile.move(dt);
            if (projectile.outOfBounds()) {
                ecs.deleteEntity(entity);
                return;
            }
            Global global = ecs.getSharedContext(Global.class);
            TileMap map = global.world.tilemap;
            if (map.contains(projectile.position)) {
                int x = U.floor(projectile.position.x);
                int y = U.floor(projectile.position.y);
                if (map.isBlock(x,y)) {
                    ecs.deleteEntity(entity);
                    return;
                }
            }
            DudeTree dude_tree = ecs.getSystem(DudeTree.class);
            DudeInfo dude_info = ecs.getSystem(DudeInfo.class);
            float r = dude_info.max_radius + projectile.radius;
            dude_tree.tree.query(potential_targets,projectile.position.x,projectile.position.y,r);
            for (Dude dude : potential_targets) {
                if (dude.disposition.isValidTarget(projectile.source_disposition)) {
                    if (dude.base_health > 0) {
                        projectile.effect.onHit(ecs,entity,dude);
                    }
                }
            } potential_targets.clear();
        }
    }
}
