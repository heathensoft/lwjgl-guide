package io.github.heathensoft.jagfw.ecs;

/**
 * Frederik Dahl 2/18/2025
 */
public abstract class ProcessSystem extends ECSystem {
    protected void preProcessing(ECS ecs, float dt) { /* */ }
    protected abstract void process(ECS ecs, int entity, float dt);
    protected void postProcessing(ECS ecs, float dt) { /* */ };
}
