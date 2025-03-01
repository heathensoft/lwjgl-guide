package no.hio.jagfw.testing.ecs.systems;

import io.github.heathensoft.jagfw.core.gfx.SpriteBatch;
import io.github.heathensoft.jagfw.ecs.ECS;
import io.github.heathensoft.jagfw.ecs.RenderSystem;
import no.hio.jagfw.testing.ecs.Global;
import no.hio.jagfw.testing.ecs.components.Projectile;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;

import java.util.List;

/**
 * Frederik Dahl 2/28/2025
 */
public class ProjectileRenderer extends RenderSystem {

    private final Vector4f uv = new Vector4f();
    private final Rectanglef rect = new Rectanglef();

    protected void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components) {
        required_components.add(Projectile.class);
    }

    protected void preRender(ECS ecs, float alpha) {
        SpriteBatch batch = ecs.getSharedContext(Global.class).graphics.sprite_batch;
        WorldCamera cam = ecs.getSystem(WorldCamera.class);
        cam.camera.refresh();
        batch.begin(cam.camera);
    }

    protected void render(ECS ecs, int entity, float alpha) {
        Projectile projectile = ecs.getComponent(entity, Projectile.class);
        if (projectile != null) {
            SpriteBatch batch = ecs.getSharedContext(Global.class).graphics.sprite_batch;
            rect.minX = projectile.position.x - projectile.radius;
            rect.maxX = projectile.position.x + projectile.radius;
            rect.minY = projectile.position.y - projectile.radius;
            rect.maxY = projectile.position.y + projectile.radius;
            batch.draw(null,rect,uv,0xFF000000,0);
        }
    }

    protected void postRender(ECS ecs, float alpha) {
        ecs.getSharedContext(Global.class).graphics.sprite_batch.end();
    }
}
