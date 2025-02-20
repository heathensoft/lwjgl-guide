package io.github.heathensoft.jagfw.ecs;

/**
 * Frederik Dahl 2/18/2025
 */
public abstract class RenderSystem extends ECSystem {
    protected void preRender(ECS ecs, float alpha) { /* */ }
    protected abstract void render(ECS ecs, int entity, float alpha);
    protected void postRender(ECS ecs, float alpha) { /* */ }
}
