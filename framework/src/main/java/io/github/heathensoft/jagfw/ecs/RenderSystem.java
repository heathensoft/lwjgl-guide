package io.github.heathensoft.jagfw.ecs;

import java.util.List;

/**
 * Frederik Dahl 2/18/2025
 */
public abstract class RenderSystem extends ECSystem {
    protected abstract void defineAccess(List<Class<?>> required_components, List<Class<?>> blocking_components);
    protected void preRender(ECS ecs, float alpha) { /* */ }
    protected abstract void render(ECS ecs, int entity, float alpha);
    protected void postRender(ECS ecs, float alpha) { /* */ }
}
